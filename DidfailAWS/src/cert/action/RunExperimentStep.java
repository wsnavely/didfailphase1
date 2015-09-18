package cert.action;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.beust.jcommander.JCommander;

import cert.SSHSession;
import cert.config.ExperimentConfig;

public class RunExperimentStep extends SSHSessionStep {
	public RunExperimentStep(AmazonEC2 ec2Conn, ExperimentConfig config) {
		super(ec2Conn, config);
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		String cmd = makeAnalysisCommand(info);
		System.out.println(cmd);
		session.sendCommand(cmd);
	}

	private String makeAnalysisCommand(MyInstanceInfo info) {
		String dest = String.format("s3://flowdroidresults/%s/%s/", this.config.experimentId, info.id);
		String cd_cmd = "cd /home/ubuntu/flowdroid-runner";
		String run_cmd = "nohup python %s --jvmargs=\"%s\" --s3out=\"%s\" > /dev/null 2>&1";
		run_cmd = String.format(run_cmd, "/home/ubuntu/flowdroid-runner/run.py", this.config.jvmArgs, dest);
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
		step.runAction();
	}
}