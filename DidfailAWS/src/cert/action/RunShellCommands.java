package cert.action;

import java.util.List;
import java.util.Scanner;

import com.amazonaws.services.ec2.AmazonEC2;
import com.beust.jcommander.JCommander;

import cert.SSHSession;
import cert.config.ExperimentConfig;

public class RunShellCommands extends SSHSessionStep {
	public RunShellCommands(AmazonEC2 ec2Conn, ExperimentConfig config) {
		super(ec2Conn);
	}

	private String cmd;

	@Override
	public void runAction() {
		Scanner in = new Scanner(System.in);
		try {
			while (true) {
				this.cmd = in.nextLine();
				super.runAction();
			}
		} finally {
			in.close();
		}
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		System.out.printf("[%s]\n%s\n", info.ip, session.sendCommand(this.cmd));
	}

	public static void main(String[] args) throws Exception {
		ExperimentArgs jct = new ExperimentArgs();
		new JCommander(jct, args);
		ExperimentConfig config = ExperimentConfig.loadConfig(jct.workingDir);
		AmazonEC2 conn = ExperimentHelper.getConnection(config);
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn, config);
		RunShellCommands step = new RunShellCommands(conn, config);
		step.setInstanceInfos(infos);
		step.setLogin(config.login);
		step.setKeyFile(config.privateKeyFile);
		step.runAction();
	}
}
