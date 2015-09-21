package cert.action;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.beust.jcommander.JCommander;

import cert.config.ExperimentConfig;

public class CreateInstancesStep extends ExperimentStep {
	private String instanceType;
	private String imageId;
	private int instanceCount;
	private String securityGroupName;
	private String securityGroupDesc;
	private String keyName;
	private List<String> instanceIds = new ArrayList<String>();
	private List<Instance> instances = new ArrayList<Instance>();

	public CreateInstancesStep(AmazonEC2 ec2Conn, ExperimentConfig config) {
		super(ec2Conn);
	}

	public void setInstanceType(String s) {
		this.instanceType = s;
	}

	public void setImageId(String s) {
		this.imageId = s;
	}

	public void setInstanceCount(int instanceCount) {
		this.instanceCount = instanceCount;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public void setSecurityGroupName(String securityGroupName) {
		this.securityGroupName = securityGroupName;
	}

	public void setSecurityGroupDesc(String description) {
		this.securityGroupDesc = description;
	}

	public List<String> getInstanceIds() {
		return instanceIds;
	}

	public List<Instance> getInstances() {
		return instances;
	}

	@Override
	public void runAction() {
		this.createSecurityGroup();
		this.launchInstances();
	}

	public void createSecurityGroup() {
		try {
			CreateSecurityGroupRequest secGroupRequest = new CreateSecurityGroupRequest(this.securityGroupName,
					this.securityGroupDesc);
			ec2Conn.createSecurityGroup(secGroupRequest);
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
					this.securityGroupName, ipPermissions);
			ec2Conn.authorizeSecurityGroupIngress(ingressRequest);
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
	}

	private void launchInstances() {
		this.instances = new ArrayList<Instance>();
		this.instanceIds = new ArrayList<String>();

		//@formatter:off
		RunInstancesRequest runRequest = new RunInstancesRequest()
			.withImageId(this.imageId)
			.withInstanceType(this.instanceType)
			.withMinCount(this.instanceCount)
			.withMaxCount(this.instanceCount)
			.withSecurityGroups(this.securityGroupName)
			.withKeyName(this.keyName);
		//@formatter:on

		RunInstancesResult runInstancesResult = ec2Conn.runInstances(runRequest);
		Reservation reservation = runInstancesResult.getReservation();
		for (Instance i : reservation.getInstances()) {
			instanceIds.add(i.getInstanceId());
		}

		System.out.println("Waiting for instances to launch...");
		sleep(10000);
		DescribeInstanceStatusRequest descStatus;
		descStatus = new DescribeInstanceStatusRequest().withInstanceIds(this.instanceIds);
		DescribeInstanceStatusResult descResults = ec2Conn.describeInstanceStatus(descStatus);
		List<InstanceStatus> state = descResults.getInstanceStatuses();

		while (state.size() != instanceIds.size()) {
			System.out.printf("%d/%d instances running.  Waiting...\n", state.size(), instanceIds.size());
			sleep(10000);
			descResults = ec2Conn.describeInstanceStatus(descStatus);
			state = descResults.getInstanceStatuses();
		}

		System.out.println("Instances are launched.");
		DescribeInstancesRequest descInst;
		descInst = new DescribeInstancesRequest().withInstanceIds(this.instanceIds);
		DescribeInstancesResult result = ec2Conn.describeInstances(descInst);
		for (Reservation r : result.getReservations()) {
			for (Instance i : r.getInstances()) {
				this.instances.add(i);
				System.out.printf("%s,%s,%s\n", i.getInstanceId(), i.getPublicIpAddress(), i.getPublicDnsName());
			}
		}
	}

	public static void main(String[] args) throws Exception {
		ExperimentArgs jct = new ExperimentArgs();
		new JCommander(jct, args);
		ExperimentConfig config = ExperimentConfig.loadConfig(jct.workingDir);
		AmazonEC2 conn = ExperimentHelper.getConnection(config);

		CreateInstancesStep step = new CreateInstancesStep(conn, config);
		step.setImageId(config.instanceId);
		step.setInstanceType(config.instanceType);
		step.setInstanceCount(config.instanceCount);
		step.setKeyName(config.accessKey);
		step.setSecurityGroupName(config.securityGroupName);
		step.setSecurityGroupDesc(config.securityGroupDesc);
		step.runAction();

		String outFile = new File(config.workingDir, "ids").toString();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "utf-8"));
			for (Instance i : step.getInstances()) {
				writer.write(i.getInstanceId());
				writer.newLine();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
			}
		}
	}
}
