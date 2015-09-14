package cert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;

public class RunExperiment {
	private static AmazonEC2 ec2;
	private static ArrayList<String> instanceIds;
	private static ArrayList<String> spotInstanceRequestIds;

	public static void main() throws Exception {
		try {
			ConnectToAWS();
			CreateSecurityGroup();
			LaunchInstances();
			WaitForInstancesToLaunch();
			//PrepareInstances();
			//RunExperiment();
		} finally {
			//Cleanup();
		}
	}

	private static void ConnectToAWS() throws IOException {
		AWSCredentials credentials = new PropertiesCredentials(
				RunExperiment.class.getResourceAsStream("AwsCredentials.properties"));
		ec2 = new AmazonEC2Client(credentials);
	}

	private static void CreateSecurityGroup() {
		try {
			CreateSecurityGroupRequest securityGroupRequest = new CreateSecurityGroupRequest(
					ExperimentConfig.securityGroupName, "");
			ec2.createSecurityGroup(securityGroupRequest);
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}

		String ipAddr = "0.0.0.0/0";
		ArrayList<String> ipRanges = new ArrayList<String>();
		ipRanges.add(ipAddr);

		ArrayList<IpPermission> ipPermissions = new ArrayList<IpPermission>();
		IpPermission ipPermission = new IpPermission();
		ipPermission.setIpProtocol("tcp");
		ipPermission.setFromPort(new Integer(22));
		ipPermission.setToPort(new Integer(22));
		ipPermission.setIpRanges(ipRanges);
		ipPermissions.add(ipPermission);

		try {
			AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest(
					ExperimentConfig.securityGroupName, ipPermissions);
			ec2.authorizeSecurityGroupIngress(ingressRequest);
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
	}

	private static void LaunchInstances() {
		RequestSpotInstancesRequest spotRequest = new RequestSpotInstancesRequest();
		LaunchSpecification launchSpecification = new LaunchSpecification();
		ArrayList<String> securityGroups = new ArrayList<String>();
		RequestSpotInstancesResult requestResult;

		spotRequest.setSpotPrice(ExperimentConfig.spotPrice);
		spotRequest.setInstanceCount(ExperimentConfig.instanceCount);
		launchSpecification.setImageId(ExperimentConfig.instanceAMI);
		launchSpecification.setInstanceType(ExperimentConfig.instanceSize);
		securityGroups.add(ExperimentConfig.securityGroupName);
		launchSpecification.setSecurityGroups(securityGroups);
		spotRequest.setLaunchSpecification(launchSpecification);

		requestResult = ec2.requestSpotInstances(spotRequest);
		List<SpotInstanceRequest> requestResponses = requestResult.getSpotInstanceRequests();

		for (SpotInstanceRequest requestResponse : requestResponses) {
			System.out.println("Created Spot Request: " + requestResponse.getSpotInstanceRequestId());
			spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());
		}
	}

	private static void WaitForInstancesToLaunch() throws InterruptedException {
		do {
			Thread.sleep(ExperimentConfig.sleepLength);
		} while (areAnyRequestsOpen());
	}

	public static boolean areAnyRequestsOpen() {
		DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
		describeRequest.setSpotInstanceRequestIds(spotInstanceRequestIds);

		System.out.println("Checking to determine if Spot Bids have reached the active state...");
		instanceIds = new ArrayList<String>();

		try {
			DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
			List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

			for (SpotInstanceRequest describeResponse : describeResponses) {
				System.out.println(" " + describeResponse.getSpotInstanceRequestId() + " is in the "
						+ describeResponse.getState() + " state.");

				if (describeResponse.getState().equals("open")) {
					return true;
				}
				instanceIds.add(describeResponse.getInstanceId());
			}
		} catch (AmazonServiceException e) {
			// Print out the error.
			System.out.println("Error when calling describeSpotInstances");
			System.out.println("Caught Exception: " + e.getMessage());
			System.out.println("Reponse Status Code: " + e.getStatusCode());
			System.out.println("Error Code: " + e.getErrorCode());
			System.out.println("Request ID: " + e.getRequestId());

			// If we have an exception, ensure we don't break out of the loop.
			// This prevents the scenario where there was blip on the wire.
			return true;
		}

		return false;
	}
}
