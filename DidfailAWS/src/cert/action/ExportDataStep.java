package cert.action;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.beust.jcommander.JCommander;

import cert.SSHSession;
import cert.config.ExperimentConfig;

public class ExportDataStep extends SSHSessionStep {
	private String exportId;

	public ExportDataStep(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	public void setExportId(String exportId) {
		this.exportId = exportId;
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		String src = "/home/ubuntu/output";
		String dest = "s3://flowdroidresults/%s/%s/";
		dest = String.format(dest, this.exportId, info.id);
		String command = makeS3SyncCmd(src, dest);
		System.out.printf("[%s]: %s\n", info.ip, command);
		session.sendCommand(command);
	}

	private String makeS3SyncCmd(String src, String dest) {
		return String.format("s3cmd sync %s %s", src, dest);
	}

	public static void main(String[] args) throws Exception {
		ExperimentArgs jct = new ExperimentArgs();
		new JCommander(jct, args);
		ExperimentConfig config = ExperimentConfig.loadConfig(jct.workingDir);
		AmazonEC2 conn = ExperimentHelper.getConnection(config);
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn, config);
		ExportDataStep step = new ExportDataStep(conn);
		step.setExportId(config.experimentId);
		step.setInstanceInfos(infos);
		step.runAction();
	}

}
