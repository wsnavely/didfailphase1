package cert.aws;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.beust.jcommander.JCommander;

import cert.aws.config.ExperimentConfig;
import cert.util.SSHSession;

public class RunExperimentStep extends SSHSessionStep {
	private String experimentId;
	private String jvmHeap;

	public RunExperimentStep(AmazonEC2 ec2Conn, ExperimentConfig config) {
		super(ec2Conn);
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		String cmd = makeAnalysisCommand(info);
		System.out.println(cmd);
		session.sendCommand(cmd);
	}

	public void setExperimentId(String experimentId) {
		this.experimentId = experimentId;
	}

	public void setJvmMem(String jvmHeap) {
		this.jvmHeap = jvmHeap;
	}

	private String makeAnalysisCommand(MyInstanceInfo info) {
		String dest = String.format("s3://flowdroidresults/%s/%s/", this.experimentId, info.id);
		String script = "/home/ubuntu/flowdroid-runner/run.py";
		String cd_cmd = "cd /home/ubuntu/flowdroid-runner";
		String run_cmd = "nohup python %s --mem=\"%s\" --s3out=\"%s\" > /dev/null 2>&1";
		run_cmd = String.format(run_cmd, script, this.jvmHeap, dest);
		return String.format("(%s; %s &)", cd_cmd, run_cmd);
	}

	public static void main(String[] args) throws Exception {
		ExperimentArgs jct = new ExperimentArgs();
		new JCommander(jct, args);
		ExperimentConfig config = ExperimentConfig.loadConfig(jct.workingDir);
		AmazonEC2 conn = ExperimentHelper.getConnection(config);
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn, config);
		RunExperimentStep step = new RunExperimentStep(conn, config);
		step.setInstanceInfos(infos);
		step.setExperimentId(config.experimentId);
		step.setJvmMem(config.javaHeapSize);
		step.setLogin(config.login);
		step.setKeyFile(config.privateKeyFile);
		step.runAction();
	}
}
