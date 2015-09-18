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
		System.out.println(info.id);
		String cmd = makeAnalysisCommand();
		System.out.println(cmd);
		System.out.println(session.sendCommand(cmd));
	}

	private String makeAnalysisCommand() {
		return "(cd /home/ubuntu/flowdroid-runner; nohup python /home/ubuntu/flowdroid-runner/run.py -Xmx20000m  > /dev/null 2>&1 &)";
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
