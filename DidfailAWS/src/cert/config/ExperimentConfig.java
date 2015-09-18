package cert.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.json.parsers.JSONParser;

public class ExperimentConfig {
	private static final String CONFIG_NAME = "experiment.json";

	private static final String IDS_NAME = "ids";

	public String workingDir;
	public String experimentId;
	public Integer instanceCount;
	public String instanceId;
	public String instanceType;
	public String login;
	public String securityGroupName;
	public String securityGroupDesc;
	public String accessKey;
	public String privateKeyFile;
	public String apkPathFile;
	public Boolean useProxy;
	public String proxyHost;
	public Integer proxyPort;
	public String jvmArgs;

	private ExperimentConfig() {
	}

	@SuppressWarnings("rawtypes")
	public static ExperimentConfig loadConfig(String directory) throws Exception {
		String configPath = new File(directory, CONFIG_NAME).toString();
		ExperimentConfig config = new ExperimentConfig();
		config.workingDir = directory;
		JSONParser parser = new JSONParser();
		Object obj = parser.parseJson(new FileInputStream(configPath), "utf-8");
		Map jsonObject = (Map) obj;
		config.experimentId = (String) jsonObject.get("experimentId");
		config.instanceCount = Integer.parseInt(jsonObject.get("instanceCount").toString());
		config.instanceId = (String) jsonObject.get("instanceId");
		config.instanceType = (String) jsonObject.get("instanceType");
		config.login = (String) jsonObject.get("login");
		config.securityGroupName = (String) jsonObject.get("securityGroupName");
		config.securityGroupDesc = (String) jsonObject.get("securityGroupDesc");
		config.accessKey = (String) jsonObject.get("accessKey");
		config.privateKeyFile = (String) jsonObject.get("privateKeyFile");
		config.apkPathFile = (String) jsonObject.get("apkPathFile");
		config.useProxy = Boolean.parseBoolean(jsonObject.get("useProxy").toString());
		config.proxyHost = (String) jsonObject.get("proxyHost");
		config.proxyPort = Integer.parseInt(jsonObject.get("proxyPort").toString());
		config.jvmArgs = (String) jsonObject.get("jvmArgs");
		return config;
	}

	public List<String> readInstanceIds() {
		List<String> lines = new ArrayList<String>();
		String idsPath = new File(this.workingDir, IDS_NAME).toString();
		try {
			lines = Files.readAllLines(Paths.get(idsPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}
}
