package test.config;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;

import cert.didfail.phase1.FlowDroidFactory;
import soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder;

public class TestConfiguration {

	public String readFile(String pathname) throws IOException {
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

	@Test
	public void testConfig1() {
		try {
			String data = readFile("testdata/config1.json");
			InfoflowAndroidConfiguration config = FlowDroidFactory.configFromJson(data);
			Assert.assertEquals(10, InfoflowAndroidConfiguration.getAccessPathLength());
			Assert.assertEquals(false, InfoflowAndroidConfiguration.getUseRecursiveAccessPaths());
			Assert.assertEquals(false, InfoflowAndroidConfiguration.getUseThisChainReduction());
			Assert.assertEquals(false, InfoflowAndroidConfiguration.getPathAgnosticResults());
			Assert.assertEquals(false, InfoflowAndroidConfiguration.getOneResultPerAccessPath());
			Assert.assertEquals(false, InfoflowAndroidConfiguration.getMergeNeighbors());
			Assert.assertEquals(false, config.getComputeResultPaths());
			Assert.assertEquals(false, config.getEnableCallbacks());
			Assert.assertEquals(false, config.getEnableCallbackSources());
			Assert.assertEquals(false, config.getStopAfterFirstFlow());
			Assert.assertEquals(false, config.getEnableImplicitFlows());
			Assert.assertEquals(false, config.getEnableStaticFieldTracking());
			Assert.assertEquals(false, config.getEnableExceptionTracking());
			Assert.assertEquals(false, config.getEnableArraySizeTainting());
			Assert.assertEquals(false, config.getFlowSensitiveAliasing());
			Assert.assertEquals(false, config.getEnableTypeChecking());
			Assert.assertEquals(false, config.getIgnoreFlowsInSystemPackages());
			Assert.assertEquals(false, config.getInspectSources());
			Assert.assertEquals(false, config.getInspectSinks());
			Assert.assertEquals(CallgraphAlgorithm.OnDemand, config.getCallgraphAlgorithm());
			Assert.assertEquals(AliasingAlgorithm.PtsBased, config.getAliasingAlgorithm());
			Assert.assertEquals(CodeEliminationMode.PropagateConstants, config.getCodeEliminationMode());
			Assert.assertEquals(PathBuilder.ContextInsensitive, config.getPathBuilder());
			Assert.assertEquals(LayoutMatchingMode.MatchAll, config.getLayoutMatchingMode());
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testConfig2() {
		try {
			String data = readFile("testdata/config2.json");
			InfoflowAndroidConfiguration config = FlowDroidFactory.configFromJson(data);
			Assert.assertEquals(8, InfoflowAndroidConfiguration.getAccessPathLength());
			Assert.assertEquals(true, InfoflowAndroidConfiguration.getUseRecursiveAccessPaths());
			Assert.assertEquals(true, InfoflowAndroidConfiguration.getUseThisChainReduction());
			Assert.assertEquals(true, InfoflowAndroidConfiguration.getPathAgnosticResults());
			Assert.assertEquals(true, InfoflowAndroidConfiguration.getOneResultPerAccessPath());
			Assert.assertEquals(true, InfoflowAndroidConfiguration.getMergeNeighbors());
			Assert.assertEquals(true, config.getComputeResultPaths());
			Assert.assertEquals(true, config.getEnableCallbacks());
			Assert.assertEquals(true, config.getEnableCallbackSources());
			Assert.assertEquals(true, config.getStopAfterFirstFlow());
			Assert.assertEquals(true, config.getEnableImplicitFlows());
			Assert.assertEquals(true, config.getEnableStaticFieldTracking());
			Assert.assertEquals(true, config.getEnableExceptionTracking());
			Assert.assertEquals(true, config.getEnableArraySizeTainting());
			Assert.assertEquals(true, config.getFlowSensitiveAliasing());
			Assert.assertEquals(true, config.getEnableTypeChecking());
			Assert.assertEquals(true, config.getIgnoreFlowsInSystemPackages());
			Assert.assertEquals(true, config.getInspectSources());
			Assert.assertEquals(true, config.getInspectSinks());
			Assert.assertEquals(CallgraphAlgorithm.SPARK, config.getCallgraphAlgorithm());
			Assert.assertEquals(AliasingAlgorithm.FlowSensitive, config.getAliasingAlgorithm());
			Assert.assertEquals(CodeEliminationMode.RemoveSideEffectFreeCode, config.getCodeEliminationMode());
			Assert.assertEquals(PathBuilder.Recursive, config.getPathBuilder());
			Assert.assertEquals(LayoutMatchingMode.NoMatch, config.getLayoutMatchingMode());
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

}
