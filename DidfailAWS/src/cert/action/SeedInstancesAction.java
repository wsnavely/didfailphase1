package cert.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession;
import cert.config.ExperimentConfig;

public class SeedInstancesAction extends SSHSessionAction {
	private String apkPathListFile;
	private Map<String, List<String>> work;

	public SeedInstancesAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	public void setApkPathListFile(String apkPathListFile) {
		this.apkPathListFile = apkPathListFile;
	}

	@Override
	public void runAction() {
		this.work = new HashMap<String, List<String>>();
		for (MyInstanceInfo i : this.instanceInfos) {
			work.put(i.id, new ArrayList<String>());
		}

		List<String> lines = new ArrayList<String>();
		List<String> keys = new ArrayList<String>(this.work.keySet());
		try {
			lines = Files.readAllLines(Paths.get(this.apkPathListFile));
			int idx = 0;
			for (String line : lines) {
				work.get(keys.get(idx)).add(line);
				idx = (idx + 1) % keys.size();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		super.runAction();
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		String output;
		List<String> apks = this.work.get(info.id);
		System.out.printf("[%s]: Making directory to store APKs\n", info.ip);
		output = session.sendCommand("mkdir -p ~/apks");
		for (String path : apks) {
			System.out.printf("[%s]: Downloading %s\n", info.ip, path);
			String cmd = makeS3DownloadCmd(path, "~/apks/");
			output = session.sendCommand(cmd);
		}
		System.out.printf("[%s]: Cloning git repo\n", info.ip);
		output = session.sendCommand("git clone https://github.com/wsnavely/flowdroid-runner.git");
	}

	private String makeS3DownloadCmd(String path, String dest) {
		return String.format("s3cmd get %s %s", path, dest);
	}

	public static void main(String[] args) throws Exception {
		AmazonEC2 conn = ExperimentHelper.getConnection();
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn);
		SeedInstancesAction step = new SeedInstancesAction(conn);
		step.setApkPathListFile(ExperimentConfig.apkPathFile);
		step.setInstanceInfos(infos);
		step.runAction();
	}

}
