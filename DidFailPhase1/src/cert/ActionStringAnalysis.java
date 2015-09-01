package cert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class ActionStringAnalysis {
	static class DidfailArgs {
		@Parameter
		private List<String> parameters = new ArrayList<String>();

		@Parameter(names = "-apk")
		private String apk;

		@Parameter(names = "-platforms")
		private String platforms;

		@Parameter(names = "-config")
		private String config;

		@Parameter(names = "-ss")
		private String sourcesAndSinks;
	}

	static class MyTag implements Tag {
		@Override
		public String getName() {
			return "flag";
		}

		@Override
		public byte[] getValue() throws AttributeValueException {
			return "tag".getBytes();
		}
	}

	private static final class ResultHandler extends AndroidInfoflowResultsHandler {

		public void handleSink(ResultSinkInfo sinkInfo, IInfoflowCFG cfg, InfoflowResults results) {
			System.out.println("Sink: " + sinkInfo.getSink());
		}

		public void handleSource(ResultSourceInfo srcInfo, IInfoflowCFG cfg, InfoflowResults results) {
			System.out.println("Source: " + srcInfo.getSource());
			srcInfo.getSource().addTag(new MyTag());
			if (srcInfo.getSource().hasTag("flag")) {
				System.out.println("tagged!");
			}
		}

		public void handleResults(IInfoflowCFG cfg, InfoflowResults results) {
			if (results == null) {
				return;
			}
			Map<ResultSinkInfo, Set<ResultSourceInfo>> resultInfos;
			resultInfos = results.getResults();

			for (ResultSinkInfo sinkInfo : results.getResults().keySet()) {
				handleSink(sinkInfo, cfg, results);
				for (ResultSourceInfo srcInfo : resultInfos.get(sinkInfo)) {
					handleSource(srcInfo, cfg, results);
				}
			}
		}

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			handleResults(cfg, results);
		}
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

	public static void main(final String[] args) throws IOException, InterruptedException, XmlPullParserException {

		DidfailArgs jct = new DidfailArgs();
		new JCommander(jct, args);
		InfoflowAndroidConfiguration config = FlowDroidFactory.configFromJson(readFile(jct.config));
		SetupApplication app = new SetupApplication(jct.platforms, jct.apk);
		app.setConfig(config);
		app.setTaintWrapper(null);
		app.calculateSourcesSinksEntrypoints(jct.sourcesAndSinks);
		ResultHandler handler = new ResultHandler();
		app.runInfoflow(handler);
	}
}
