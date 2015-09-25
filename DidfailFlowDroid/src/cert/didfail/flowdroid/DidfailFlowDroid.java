package cert.didfail.flowdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
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
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.options.Options;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.MultiMap;

public class DidfailFlowDroid {

	static class DidfailArgs {
		@Parameter
		private List<String> parameters = new ArrayList<String>();

		@Parameter(names = "-apk", required = true)
		private String apk;

		@Parameter(names = "-platforms", required = true)
		private String platforms;

		@Parameter(names = "-out")
		private String outfile = null;

		@Parameter(names = "-config")
		private String config;

		@Parameter(names = "-ss")
		private String sourcesAndSinks = "SourcesAndSinks.txt";

		@Parameter(names = "-tw")
		private String taintWrapper = null;

		@Parameter(names = "-labelsinks")
		private boolean labelSinks = false;
	}

	private static class DidfailPreprocessor implements PreAnalysisHandler {

		@Override
		public void onBeforeCallgraphConstruction() {
			PackManager.v().getPack("wjap").add(new Transform("wjap.myTransform", new SceneTransformer() {
				@Override
				protected void internalTransform(String phaseName, Map<String, String> options) {
					for (SootClass sc : Scene.v().getClasses()) {
						for (SootMethod m : sc.getMethods()) {
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
		private File output;
		private int intentId = 1;
		private boolean labelSinks;

		private DidfailResultHandler(File output, boolean labelSinks) {
			this.output = output;
			this.labelSinks = labelSinks;
		}

		public Element handleSink(ResultSinkInfo sinkInfo, IInfoflowCFG cfg, InfoflowResults results) {
			Stmt sink = sinkInfo.getSink();
			Element sinkElem = new Element("sink");
			sinkElem.addAttribute(new Attribute("method", getMethSig(sink)));

			if (isIntentSink(sink)) {
				sinkElem.addAttribute(new Attribute("is-intent", "1"));
				sinkElem.addAttribute(new Attribute("intent-id", Integer.toString(this.intentId)));
				sink.addTag(new SinkTag(this.intentId));
				this.intentId++;

				for (Tag tag : sink.getTags()) {
					if (tag instanceof PathConstraintTag) {
						PathConstraintTag pct = (PathConstraintTag) tag;
						Element constraint = new Element("constraint");
						constraint.addAttribute(new Attribute("name", pct.string));
						constraint.addAttribute(new Attribute("cmp", Boolean.toString(pct.negate)));
						sinkElem.appendChild(constraint);
					}
				}

				try {
					InvokeExpr ie = sink.getInvokeExpr();
					AbstractInstanceInvokeExpr aie = (AbstractInstanceInvokeExpr) ie;
					Type baseType = aie.getBase().getType();
					String cmp = baseType.toString();
					sinkElem.addAttribute(new Attribute("component", cmp));
				} catch (Exception e) {
				}
			}
			if (isIntentResultSink(sink)) {
				SootMethod sm = cfg.getMethodOf(sink);
				SootClass cls = sm.getDeclaringClass();
				String cmp = cls.toString();
				sinkElem.addAttribute(new Attribute("is-intent-result", "1"));
				sinkElem.addAttribute(new Attribute("component", cmp));
			}

			return sinkElem;
		}

		public Element handleSource(ResultSourceInfo srcInfo, IInfoflowCFG cfg, InfoflowResults results) {
			Stmt src = srcInfo.getSource();
			Element srcElem = new Element("source");
			SootMethod sm = cfg.getMethodOf(src);
			String methName = sm.getName();
			String methSig = getMethSig(srcInfo.getSource());
			srcElem.addAttribute(new Attribute("method", methSig));
			srcElem.addAttribute(new Attribute("in", methName));
			if (methSig.indexOf(" getIntent()") != -1) {
				InvokeExpr ie = src.getInvokeExpr();
				AbstractInstanceInvokeExpr aie = (AbstractInstanceInvokeExpr) ie;
				Type baseType = aie.getBase().getType();
				String cmp = baseType.toString();
				srcElem.addAttribute(new Attribute("component", cmp));
			} else if (methSig.indexOf(":= @parameter") != -1) {
				SootClass cls = sm.getDeclaringClass();
				String cmp = cls.toString();
				srcElem.addAttribute(new Attribute("component", cmp));
			}
			return srcElem;
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
				return sig;
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

		public Element handleResults(IInfoflowCFG cfg, InfoflowResults results) {
			Element root = new Element("results");
			root.addAttribute(new Attribute("package", this.getAppPackage()));

			if (results != null) {
				MultiMap<ResultSinkInfo, ResultSourceInfo> resultInfos;
				resultInfos = results.getResults();
				Comparator<ResultSinkInfo> sinkSorter = new SinkComparator();
				Comparator<ResultSourceInfo> sourceSorter = new SourceComparator();

				// Sort the sinks
				Set<ResultSinkInfo> sinkSet = results.getResults().keySet();
				List<ResultSinkInfo> sinks = new ArrayList<ResultSinkInfo>(sinkSet);
				Collections.sort(sinks, sinkSorter);

				for (ResultSinkInfo sinkInfo : sinks) {
					Element flow = new Element("flow");
					flow.appendChild(handleSink(sinkInfo, cfg, results));

					Set<ResultSourceInfo> srcSet = resultInfos.get(sinkInfo);
					List<ResultSourceInfo> srcs = new ArrayList<ResultSourceInfo>(srcSet);
					Collections.sort(srcs, sourceSorter);
					for (ResultSourceInfo srcInfo : srcs) {
						flow.appendChild(handleSource(srcInfo, cfg, results));
					}
					root.appendChild(flow);
				}
			}

			return root;
		}

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {

			Element root = handleResults(cfg, results);
			Document doc = new Document(root);
			OutputStream os = null;

			try {
				if (this.output != null) {
					os = new FileOutputStream(this.output);
				} else {
					os = System.out;
				}
				Serializer serializer = new Serializer(os, "UTF-8");
				serializer.setIndent(4);
				serializer.setMaxLength(64);
				serializer.write(doc);
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				if (this.output != null && os != null) {
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			if (this.labelSinks) {
				Options.v().set_output_format(Options.output_format_dex);
				PackManager.v().getPack("wjtp").add(new Transform("wjtp.myInstrumenter", new SinkLabeler()));
				PackManager.v().getPack("wjtp").apply();
				PackManager.v().writeOutput();
			}
		}
	}

	public static void usage() {
		System.err.println("Usage: [<outfile>] -- <flowdroid arguments>");
	}

	public static String readFile(String pathname) throws IOException {
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

		MySetupApplication app = new MySetupApplication(jct.platforms, jct.apk);
		app.setConfig(config);

		EasyTaintWrapper easyTaintWrapper = null;
		if (jct.taintWrapper != null) {
			easyTaintWrapper = new EasyTaintWrapper(jct.taintWrapper);
			easyTaintWrapper.setAggressiveMode(true);
		}
		app.setTaintWrapper(easyTaintWrapper);
		app.calculateSourcesSinksEntrypoints(jct.sourcesAndSinks);

		List<PreAnalysisHandler> preprocessors = new ArrayList<PreAnalysisHandler>();
		preprocessors.add(new DidfailPreprocessor());

		File out = null;
		if (jct.outfile != null) {
			out = new File(jct.outfile);
		}
		DidfailResultHandler handler = new DidfailResultHandler(out, jct.labelSinks);
		String pkg = app.getAppPackage();
		handler.setAppPackage(pkg);
		app.runInfoflow(handler, preprocessors);
	}
}
