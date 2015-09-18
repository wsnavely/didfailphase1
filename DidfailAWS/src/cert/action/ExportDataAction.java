package cert.action;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession;
import cert.config.ExperimentConfig;

public class ExportDataAction extends SSHSessionAction {
	public ExportDataAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		String src = "/home/ubuntu/output";
		String dest = "s3://flowdroidresults/%s/%s/";
		dest = String.format(dest, ExperimentConfig.experimentId, info.id);
		String command = makeS3SyncCmd(src, dest);
		System.out.printf("[%s]: %s\n", info.ip, command);
		String result = session.sendCommand(command);
	}

	private String makeS3SyncCmd(String src, String dest) {
		return String.format("s3cmd sync %s %s", src, dest);
	}

	public static void main(String[] args) throws Exception {
		AmazonEC2 conn = ExperimentHelper.getConnection();
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn);
		ExportDataAction step = new ExportDataAction(conn);
		step.setInstanceInfos(infos);
		step.runAction();
	}

}
