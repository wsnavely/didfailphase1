package cert.action;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;

import cert.config.ExperimentConfig;

public class CreateInstancesAction extends Action {
	private String instanceType;
	private String imageId;
	private int instanceCount;
	private String securityGroupName;
	private String keyName;
	private List<String> instanceIds = new ArrayList<String>();
	private List<Instance> instances = new ArrayList<Instance>();

	public CreateInstancesAction(AmazonEC2 ec2Conn) {
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

	public List<String> getInstanceIds() {
		return instanceIds;
	}

	public List<Instance> getInstances() {
		return instances;
	}

	@Override
	public void run() {
		launchInstances();
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
		AmazonEC2 conn = ExperimentHelper.getConnection();
		CreateInstancesAction step = new CreateInstancesAction(conn);
		step.setImageId(ExperimentConfig.instanceId);
		step.setInstanceType(ExperimentConfig.instanceType);
		step.setInstanceCount(ExperimentConfig.instanceCount);
		step.setKeyName(ExperimentConfig.accessKey);
		step.setSecurityGroupName(ExperimentConfig.securityGroupName);
		step.run();
		String outFile = "ids";
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "utf-8"));
			String line;
			for (Instance i : step.getInstances()) {
				line = String.format("%s,%s,%s", i.getInstanceId(), i.getPublicIpAddress(), i.getPublicDnsName());
				writer.write(line);
				writer.newLine();
			}
		} catch (IOException ex) {
			// report
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
				/* ignore */}
		}
	}
}
