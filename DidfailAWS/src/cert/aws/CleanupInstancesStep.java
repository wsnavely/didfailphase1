package cert.aws;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.beust.jcommander.JCommander;

import cert.aws.config.ExperimentConfig;

public class CleanupInstancesStep extends ExperimentStep {
	protected List<MyInstanceInfo> instanceInfos;

	public CleanupInstancesStep(AmazonEC2 ec2Conn, ExperimentConfig config) {
		super(ec2Conn);
	}

	public void setInstanceInfos(List<MyInstanceInfo> instanceInfos) {
		this.instanceInfos = instanceInfos;
	}

	@Override
	public void runAction() {
		List<String> ids = new ArrayList<String>();
		for (MyInstanceInfo info : this.instanceInfos) {
			ids.add(info.id);
		}
		TerminateInstancesRequest termReq = new TerminateInstancesRequest(ids);
		this.ec2Conn.terminateInstances(termReq);
	}

	public static void main(String[] args) throws Exception {
		ExperimentArgs jct = new ExperimentArgs();
		new JCommander(jct, args);
		ExperimentConfig config = ExperimentConfig.loadConfig(jct.workingDir);
		AmazonEC2 conn = ExperimentHelper.getConnection(config);
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn, config);
		CleanupInstancesStep step = new CleanupInstancesStep(conn, config);
		step.setInstanceInfos(infos);
		step.runAction();
	}
}
