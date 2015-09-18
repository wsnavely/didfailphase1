package cert.action;

import java.io.File;
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

	public static AmazonEC2 getConnection(ExperimentConfig expConfig) {
		AWSCredentials credentials = null;
		credentials = new ProfileCredentialsProvider("default").getCredentials();
		ClientConfiguration clientConfig = new ClientConfiguration();
		if (expConfig.useProxy) {
			clientConfig.setProxyHost(expConfig.proxyHost);
			clientConfig.setProxyPort(expConfig.proxyPort);
		}
		return new AmazonEC2Client(credentials, clientConfig);
	}

	public static List<String> readInstanceIds(ExperimentConfig expConfig) {
		List<String> lines = new ArrayList<String>();
		String idsPath = new File(expConfig.workingDir, "ids").toString();
		try {
			lines = Files.readAllLines(Paths.get(idsPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	public static List<MyInstanceInfo> getInstanceInfo(AmazonEC2 conn, ExperimentConfig expConfig) {
		List<String> ids = readInstanceIds(expConfig);
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
