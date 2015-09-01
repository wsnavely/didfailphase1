package cert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import soot.Body;
import soot.Hierarchy;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class DidfailPhase1 {
	static class DidfailArgs {
		@Parameter
		private List<String> parameters = new ArrayList<String>();

		@Parameter(names = "-apk")
		private String apk;

		@Parameter(names = "-platforms")
		private String platforms;

		@Parameter(names = "-out")
		private String outfile;

		@Parameter(names = "-config")
		private String config;

		@Parameter(names = "-ss")
		private String sourcesAndSinks;
	}

	private static class DidfailPreprocessor implements PreAnalysisHandler {

		@Override
		public void onBeforeCallgraphConstruction() {
			PackManager.v().getPack("wjap").add(new Transform("wjap.myTransform", new SceneTransformer() {
				@Override
				protected void internalTransform(String phaseName, Map<String, String> options) {
					for (SootClass sc : Scene.v().getClasses()) {
						for (SootMethod m : sc.getMethods()) {
							if (m.getName().startsWith("test"))
								try {
									Body b = m.retrieveActiveBody();
									new AssignmentAnalysis(new ExceptionalUnitGraph(b));
								} catch (Exception e) {
									continue;
								}
						}
					}
				}
			}));
		}

		@Override
		public void onAfterCallgraphConstruction() {
			PackManager.v().getPack("wjap").apply();
		}
	}

	private static final class DidfailResultHandler extends AndroidInfoflowResultsHandler {
		private BufferedWriter wr;

		private DidfailResultHandler() {
			this.wr = null;
		}

		private DidfailResultHandler(BufferedWriter wr) {
			this.wr = wr;
		}

		public void handleSink(ResultSinkInfo sinkInfo, IInfoflowCFG cfg, InfoflowResults results) {
			Stmt sink = sinkInfo.getSink();
			String methSig = getMethSig(sink);
			printf("\t<sink method=\"%s\"", escapeXML(methSig));
			if (isIntentSink(sink)) {
				// IntentTag tag = (IntentTag) sink.getTag("IntentID");
				// String id = escapeXML(tag.getIntentID());
				printf(" is-intent=\"1\"");
				// printf(" intent-id=\"%s\"", id);
				try {
					InvokeExpr ie = sink.getInvokeExpr();
					AbstractInstanceInvokeExpr aie = (AbstractInstanceInvokeExpr) ie;
					Type baseType = aie.getBase().getType();
					String cmp = escapeXML(baseType.toString());
					printf(" component=\"%s\"", cmp);
				} catch (Exception e) {
				}
			}
			if (isIntentResultSink(sink)) {
				print(" is-intent-result=\"1\"");
				SootMethod sm = cfg.getMethodOf(sink);
				SootClass cls = sm.getDeclaringClass();
				String cmp = escapeXML(cls.toString());
				printf(" component=\"%s\"", cmp);
			}
			if (sink.hasTag("BooleanExpressionTag")) {
				Tag tag = sink.getTag("BooleanExpressionTag");
				String value = new String(tag.getValue());
				System.out.println();
				System.out.println(sink);
				print(" cond=\"" + escapeXML(value) + "\"");
			}
			println("></sink>");
		}

		public void handleSource(ResultSourceInfo srcInfo, IInfoflowCFG cfg, InfoflowResults results) {
			Stmt src = srcInfo.getSource();
			SootMethod sm = cfg.getMethodOf(src);
			String methName = sm.getName();
			String methSig = getMethSig(srcInfo.getSource());
			printf("\t<source method=\"%s\"", escapeXML(methSig));
			if (methSig.indexOf(" getIntent()") != -1) {
				InvokeExpr ie = src.getInvokeExpr();
				AbstractInstanceInvokeExpr aie = (AbstractInstanceInvokeExpr) ie;
				Type baseType = aie.getBase().getType();
				String cmp = escapeXML(baseType.toString());
				printf(" component=\"%s\"", cmp);
			} else if (methSig.indexOf(":= @parameter") != -1) {
				SootClass cls = sm.getDeclaringClass();
				String cmp = escapeXML(cls.toString());
				printf(" component=\"%s\"", cmp);
			}
			printf(" in=\"%s\"", escapeXML(methName));
			println("></source>");
		}

		public String getMethSig(Stmt stmt) {
			if (!stmt.containsInvokeExpr()) {
				return "Stmt(" + stmt.toString() + ")";
			}
			AbstractInvokeExpr ie = (AbstractInvokeExpr) stmt.getInvokeExpr();
			SootMethod meth = ie.getMethod();
			return meth.getSignature();
		}

		private class SinkComparator implements Comparator<ResultSinkInfo> {
			public String sinkToString(ResultSinkInfo s) {
				String sig = getMethSig(s.getSink());
				String tag = "";
				if (s.getSink().hasTag("IntentID")) {
					// tag = ((IntentTag)
					// s.getSink().getTag("IntentID")).getIntentID();
				}
				return sig + tag;
			}

			@Override
			public int compare(ResultSinkInfo o1, ResultSinkInfo o2) {
				return sinkToString(o1).compareTo(sinkToString(o2));
			}
		}

		private class SourceComparator implements Comparator<ResultSourceInfo> {
			public String sourceToString(ResultSourceInfo s) {
				return getMethSig(s.getSource());
			}

			@Override
			public int compare(ResultSourceInfo o1, ResultSourceInfo o2) {
				return sourceToString(o1).compareTo(sourceToString(o2));
			}
		}

		public void handleResults(IInfoflowCFG cfg, InfoflowResults results) {
			if (results == null) {
				print("No results found.");
				return;
			}
			Map<ResultSinkInfo, Set<ResultSourceInfo>> resultInfos;
			resultInfos = results.getResults();
			Comparator<ResultSinkInfo> sinkSorter = new SinkComparator();
			Comparator<ResultSourceInfo> sourceSorter = new SourceComparator();
			String pkg = escapeXML(this.getAppPackage());
			// Sort the sinks
			Set<ResultSinkInfo> sinkSet = results.getResults().keySet();
			List<ResultSinkInfo> sinks = new ArrayList<ResultSinkInfo>();
			sinks.addAll(sinkSet);
			Collections.sort(sinks, sinkSorter);
			println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			printf("<results package=\"%s\">\n", pkg);
			for (ResultSinkInfo sinkInfo : sinks) {
				println("<flow>");
				System.out.println("SINK " + sinkInfo.getSink());
				handleSink(sinkInfo, cfg, results);
				// Sort the sources
				Set<ResultSourceInfo> srcSet = resultInfos.get(sinkInfo);
				List<ResultSourceInfo> srcs = new ArrayList<ResultSourceInfo>();
				srcs.addAll(srcSet);
				Collections.sort(srcs, sourceSorter);
				for (ResultSourceInfo srcInfo : srcs) {
					System.out.println("SOURCE " + srcInfo.getSource());
					handleSource(srcInfo, cfg, results);
				}
				println("</flow>");
			}
			println("</results>");
		}

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			try {
				handleResults(cfg, results);
			} finally {
				if (this.wr != null) {
					try {
						this.wr.close();
					} catch (Exception e) {
					}
				}
			}
		}

		public static String escapeXML(Object obj) {
			return escapeXML(obj.toString(), "");
		}

		public static String escapeXML(String str, String retIfNull) {
			/*
			 * Based on
			 * http://www.docjar.com/html/api/org/apache/commons/lang/Entities
			 * .java.html
			 */
			if (str == null) {
				return retIfNull;
			}
			StringWriter writer = new StringWriter();
			int len = str.length();
			for (int i = 0; i < len; i++) {
				char c = str.charAt(i);
				if (c > 0x7F) {
					writer.write("&#");
					writer.write(Integer.toString(c, 10));
					writer.write(';');
				} else {
					switch ((byte) c) {
					case '&':
						writer.write("&amp;");
						break;
					case '<':
						writer.write("&lt;");
						break;
					case '>':
						writer.write("&gt;");
						break;
					case '"':
						writer.write("&quot;");
						break;
					case '\'':
						writer.write("&apos;");
						break;
					default:
						writer.write(c);
					}
				}
			}
			return writer.toString();
		}

		private void printf(String format, Object... args) {
			try {
				// System.out.printf(format, args);
				if (wr != null)
					wr.write(String.format(format, args));
			} catch (IOException ex) {
				// ignore
			}
		}

		private void println(String string) {
			try {
				// System.out.println(string);
				if (wr != null)
					wr.write(string + "\n");
			} catch (IOException ex) {
				// ignore
			}
		}

		private void print(String string) {
			try {
				System.out.print(string);
				if (wr != null)
					wr.write(string);
			} catch (IOException ex) {
				// ignore
			}
		}
	}

	public static void usage() {
		System.err.println("Usage: [<outfile>] -- <flowdroid arguments>");
	}

	private static String readFile(String pathname) throws IOException {
		File file = new File(pathname);
		StringBuilder fileContents = new StringBuilder((int) file.length());
		Scanner scanner = new Scanner(file);
		String lineSeparator = System.getProperty("line.separator");

		try {
			while (scanner.hasNextLine()) {
				fileContents.append(scanner.nextLine() + lineSeparator);
			}
			return fileContents.toString();
		} finally {
			scanner.close();
		}
	}
	// BEGIN DIDFAIL ADDITIONS

	public static boolean isIntentSink(Stmt stmt) {
		if (!stmt.containsInvokeExpr()) {
			return false;
		}
		AbstractInvokeExpr ie = (AbstractInvokeExpr) stmt.getInvokeExpr();
		SootMethod meth = ie.getMethod();
		SootClass android_content_Context = Scene.v().getSootClass("android.content.Context");
		// FIXME: Check the method name better!
		if (meth.toString().indexOf("startActivity") == -1) {
			return false;
		}
		return ((new Hierarchy()).isClassSuperclassOfIncluding(android_content_Context, meth.getDeclaringClass()));
	}

	public static boolean isIntentResultSink(Stmt stmt) {
		if (!stmt.containsInvokeExpr()) {
			return false;
		}
		AbstractInvokeExpr ie = (AbstractInvokeExpr) stmt.getInvokeExpr();
		SootMethod meth = ie.getMethod();
		SootClass android_content_Context = Scene.v().getSootClass("android.app.Activity");
		if (meth.toString().indexOf("setResult") == -1) {
			return false;
		}
		return ((new Hierarchy()).isClassSuperclassOfIncluding(android_content_Context, meth.getDeclaringClass()));
	}

	public static String extractIntentID(Stmt prevStmt) {
		try {
			if (!prevStmt.containsInvokeExpr()) {
				return "";
			}
			AbstractInvokeExpr ie = (AbstractInvokeExpr) prevStmt.getInvokeExpr();
			String sig = ie.getMethod().getSignature();
			if (!sig.equals(
					"<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.String)>")) {
				return "";
			}
			StringConstant ret = (StringConstant) ie.getArg(0);
			return ret.value;
		} catch (Exception e) {
			return "";
		}
	}

	public static void main(final String[] args) throws IOException, InterruptedException, XmlPullParserException {

		DidfailArgs jct = new DidfailArgs();
		new JCommander(jct, args);
		InfoflowAndroidConfiguration config = FlowDroidFactory.configFromJson(readFile(jct.config));
		SetupApplication app = new SetupApplication(jct.platforms, jct.apk);
		app.setConfig(config);
		app.setTaintWrapper(null);
		app.calculateSourcesSinksEntrypoints(jct.sourcesAndSinks);

		List<PreAnalysisHandler> preprocessors = new ArrayList<PreAnalysisHandler>();
		preprocessors.add(new DidfailPreprocessor());

		BufferedWriter bw = null;
		if (jct.outfile != null && !jct.outfile.isEmpty()) {
			File out = new File(jct.outfile);
			bw = new BufferedWriter(new FileWriter(out));
		}

		DidfailResultHandler handler = new DidfailResultHandler(bw);
		// String pkg = app.getSourceSinkManager().getAppPackageName();
		// handler.setAppPackage(pkg);
		app.runInfoflow(handler, preprocessors);
	}
}
