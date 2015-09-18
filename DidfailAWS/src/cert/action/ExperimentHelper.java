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
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import cert.config.ExperimentConfig;

public class ExperimentHelper {
	private static final String ID_PATH = "ids";

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

	public static List<String> readInstanceIds() {
		List<String> lines = new ArrayList<String>();
		try {
			lines = Files.readAllLines(Paths.get(ID_PATH));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;

	}

	public static List<MyInstanceInfo> getInstanceInfo(AmazonEC2 conn) {
		List<String> ids = readInstanceIds();
		List<MyInstanceInfo> myInfos = new ArrayList<MyInstanceInfo>();
		DescribeInstancesRequest descInst;
		descInst = new DescribeInstancesRequest().withInstanceIds(ids);
		DescribeInstancesResult result = conn.describeInstances(descInst);
		for (Reservation r : result.getReservations()) {
			for (Instance i : r.getInstances()) {
				MyInstanceInfo myInfo = new MyInstanceInfo();
				myInfo.id = i.getInstanceId();
				myInfo.ip = i.getPublicIpAddress();
				myInfo.dns = i.getPublicDnsName();
				myInfos.add(myInfo);
			}
		}
		return myInfos;
	}
}
