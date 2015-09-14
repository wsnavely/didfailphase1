package cert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.json.generators.JSONGenerator;
import com.json.generators.JsonGeneratorFactory;

import soot.Body;
import soot.Hierarchy;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.MultiMap;

public class ActionStringSinkAnalysis {
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
		
		@Parameter(names = "-tw")
		private String taintWrapper;
	}

	private static class MyPreprocessor implements PreAnalysisHandler {

		@Override
		public void onBeforeCallgraphConstruction() {
			PackManager.v().getPack("wjap").add(new Transform("wjap.myTransform", new SceneTransformer() {
				@Override
				protected void internalTransform(String phaseName, Map<String, String> options) {
					for (SootClass sc : Scene.v().getClasses()) {

						if (sc.getName().startsWith("android")) {
							continue;
						}

						if (sc.getName().startsWith("java")) {
							continue;
						}
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
			System.out.println("Running analysis...");
			PackManager.v().getPack("wjap").apply();
		}
	}

	private static final class MyResultHandler implements ResultsAvailableHandler {
		private BufferedWriter bw;

		public MyResultHandler(BufferedWriter bw) {
			this.bw = bw;
		}

		public void handleResults(IInfoflowCFG cfg, InfoflowResults results) {
			Map data = new HashMap();
			MultiMap<ResultSinkInfo, ResultSourceInfo> resultInfos;
			resultInfos = results.getResults();
			Set<ResultSinkInfo> sinkSet = results.getResults().keySet();

			List sinks = new ArrayList();
			for (ResultSinkInfo sinkInfo : sinkSet) {
				Map sinkData = new HashMap();
				Stmt sink = sinkInfo.getSink();
				sinkData.put("stmt", sink.toString());

				List constraints = new ArrayList();
				for (Tag t : sink.getTags()) {
					if (t instanceof PathConstraintTag) {
						PathConstraintTag pct = (PathConstraintTag) t;
						Map constraintData = new HashMap();
						constraintData.put("actionString", pct.string);
						constraintData.put("negate", pct.negate);
						constraints.add(constraintData);
					}
				}
				sinkData.put("constraints", constraints.toArray());

				List sources = new ArrayList();
				Set<ResultSourceInfo> srcSet = resultInfos.get(sinkInfo);
				for (ResultSourceInfo srcInfo : srcSet) {
					Map srcData = new HashMap();
					srcData.put("stmt", srcInfo.getSource().toString());
					sources.add(srcData);
				}
				sinkData.put("sources", sources.toArray());
				sinks.add(sinkData);
			}

			data.put("sinks", sinks.toArray());
			JsonGeneratorFactory factory = JsonGeneratorFactory.getInstance();
			JSONGenerator generator = factory.newJsonGenerator();
			String json = generator.generateJson(data);
			System.out.println(json);
			output(json);

		}

		public void output(String s) {
			if (this.bw != null) {
				try {
					this.bw.write(s);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			handleResults(cfg, results);
			try {
				if (this.bw != null) {
					this.bw.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
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
		MySetupApplication app = new MySetupApplication(jct.platforms, jct.apk);
		app.setConfig(config);
		
		EasyTaintWrapper easyTaintWrapper;
		easyTaintWrapper = new EasyTaintWrapper(jct.taintWrapper);
		easyTaintWrapper.setAggressiveMode(true);
		app.setTaintWrapper(easyTaintWrapper);
		
		app.calculateSourcesSinksEntrypoints(jct.sourcesAndSinks);

		BufferedWriter bw = null;
		if (jct.outfile != null && !jct.outfile.isEmpty()) {
			File out = new File(jct.outfile);
			bw = new BufferedWriter(new FileWriter(out));
		}

		List<PreAnalysisHandler> preprocessors = new ArrayList<PreAnalysisHandler>();
		preprocessors.add(new MyPreprocessor());
		MyResultHandler handler = new MyResultHandler(bw);
		app.runInfoflow(handler, preprocessors);
	}
}
