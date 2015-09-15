package cert.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;

import cert.config.ExperimentConfig;

public class ExperimentHelper {
	public static AmazonEC2 getConnection() {
		AWSCredentials credentials = null;
		credentials = new ProfileCredentialsProvider("default").getCredentials();
		ClientConfiguration config = new ClientConfiguration();
		if (ExperimentConfig.useProxy) {
			config.setProxyHost(ExperimentConfig.proxyHost);
			config.setProxyPort(ExperimentConfig.proxyPort);
		}
		return new AmazonEC2Client(credentials, config);
	}

	public static List<MyInstanceInfo> readInstanceInfo() {
		List<String> lines = new ArrayList<String>();
		List<MyInstanceInfo> info = new ArrayList<MyInstanceInfo>();
		try {
			lines = Files.readAllLines(Paths.get("ids"));
			for (String line : lines) {
				String[] parts = line.split(",");
				MyInstanceInfo i = new MyInstanceInfo();
				i.id = parts[0];
				i.ip = parts[1];
				i.dns = parts[2];
				info.add(i);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return info;
	}
}
