package cert.action;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession;

public class UpdateGitRepo extends SSHSessionAction {
	public UpdateGitRepo(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		session.sendCommand("cd /home/ubuntu/flowdroid-runner;git pull");
	}

	public static void main(String[] args) throws Exception {
		AmazonEC2 conn = ExperimentHelper.getConnection();
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn);
		UpdateGitRepo step = new UpdateGitRepo(conn);
		step.setInstanceInfos(infos);
		step.runAction();
	}

}
