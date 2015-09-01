package cert;

import java.io.IOException;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

import soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class FlowDroidFactory {

	public static InfoflowAndroidConfiguration configFromJson(String json) {
		InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();
		Map jsonData = parser.parseJson(json);

		for (Object key : jsonData.keySet()) {
			Object value = jsonData.get(key);
			boolean booleanValue;
			int intValue;
			
			switch ((String) key) {
			case "accessPathLength":
				intValue = Integer.parseInt((String) value);
				InfoflowAndroidConfiguration.setAccessPathLength(intValue);
				break;
			case "useRecursiveAccessPath":
				booleanValue = Boolean.parseBoolean((String) value);
				InfoflowAndroidConfiguration.setUseRecursiveAccessPaths(booleanValue);
				break;
			case "useThisChainReduction":
				booleanValue = Boolean.parseBoolean((String) value);
				InfoflowAndroidConfiguration.setUseThisChainReduction(booleanValue);
				break;
			case "pathAgnosticResults":
				booleanValue = Boolean.parseBoolean((String) value);
				InfoflowAndroidConfiguration.setPathAgnosticResults(booleanValue);
				break;
			case "oneResultPerAccessPath":
				booleanValue = Boolean.parseBoolean((String) value);
				InfoflowAndroidConfiguration.setOneResultPerAccessPath(booleanValue);
				break;
			case "mergeNeighbors":
				booleanValue = Boolean.parseBoolean((String) value);
				InfoflowAndroidConfiguration.setMergeNeighbors(booleanValue);
				break;
			case "computeResultPaths":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setComputeResultPaths(booleanValue);
				break;
			case "enableCallbacks":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setEnableCallbacks(booleanValue);
				break;
			case "enableCallbackSources":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setEnableCallbackSources(booleanValue);
				break;
			case "stopAfterFirstFlow":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setStopAfterFirstFlow(booleanValue);
				break;
			case "enableImplicitFlows":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setEnableImplicitFlows(booleanValue);
				break;
			case "enableStaticFields":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setEnableStaticFieldTracking(booleanValue);
				break;
			case "enableExceptions":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setEnableExceptionTracking(booleanValue);
				break;
			case "enableArraySizeTainting":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setEnableArraySizeTainting(booleanValue);
				break;
			case "flowSensitiveAliasing":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setFlowSensitiveAliasing(booleanValue);
				break;
			case "enableTypeChecking":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setEnableTypeChecking(booleanValue);
				break;
			case "ignoreFlowsInSystemPackages":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setIgnoreFlowsInSystemPackages(booleanValue);
				break;
			case "inspectSources":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setInspectSources(booleanValue);
				break;
			case "inspectSinks":
				booleanValue = Boolean.parseBoolean((String) value);
				config.setInspectSinks(booleanValue);
				break;
			case "callgraphAlgorithm":
				config.setCallgraphAlgorithm(Enum.valueOf(CallgraphAlgorithm.class, (String) value));
				break;
			case "aliasingAlgorithm":
				config.setAliasingAlgorithm(Enum.valueOf(AliasingAlgorithm.class, (String) value));
				break;
			case "codeEliminationMode":
				config.setCodeEliminationMode(Enum.valueOf(CodeEliminationMode.class, (String) value));
				break;
			case "pathBuilder":
				config.setPathBuilder(Enum.valueOf(PathBuilder.class, (String) value));
				break;
			case "layoutMatchingMode":
				config.setLayoutMatchingMode(Enum.valueOf(LayoutMatchingMode.class, (String) value));
				break;
			default:
				throw new UnsupportedOperationException("Unexpected option: " + key);
			}
		}

		return config;
	}

	public static SetupApplication runAnalysis(
			//@formatter:off
			String apk, 
			String platforms, 
			String sourcesAndSinks,
			InfoflowAndroidConfiguration config, 
			ITaintPropagationWrapper taintWrapper,
			ResultsAvailableHandler resultHandler)  
			//@formatter:on 
					throws IOException, XmlPullParserException {
		SetupApplication app = new SetupApplication(platforms, apk);
		app.setConfig(config);
		app.setTaintWrapper(taintWrapper);
		app.calculateSourcesSinksEntrypoints(sourcesAndSinks);
		return app;
	}
}