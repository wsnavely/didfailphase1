package cert.action;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.beust.jcommander.JCommander;

import cert.SSHSession;
import cert.config.ExperimentConfig;

public class UpdateGitRepoStep extends SSHSessionStep {
	public UpdateGitRepoStep(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		session.sendCommand("cd /home/ubuntu/flowdroid-runner;git pull");
	}

	public static void main(String[] args) throws Exception {
		ExperimentArgs jct = new ExperimentArgs();
		new JCommander(jct, args);
		ExperimentConfig config = ExperimentConfig.loadConfig(jct.workingDir);
		AmazonEC2 conn = ExperimentHelper.getConnection(config);
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn, config);
		UpdateGitRepoStep step = new UpdateGitRepoStep(conn);
		step.setLogin(config.login);
		step.setKeyFile(config.privateKeyFile);
		step.setInstanceInfos(infos);
		step.runAction();
	}
}
