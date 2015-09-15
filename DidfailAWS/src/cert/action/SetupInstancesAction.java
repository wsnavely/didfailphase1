package cert.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession;
import cert.SSHSession.AuthMethod;
import cert.config.ExperimentConfig;

public class SetupInstancesAction extends Action {
	private String apkPathListFile;
	private List<MyInstanceInfo> instanceInfos;

	public SetupInstancesAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	public void setApkPathListFile(String apkPathListFile) {
		this.apkPathListFile = apkPathListFile;
	}

	public void setInstanceInfos(List<MyInstanceInfo> instanceInfos) {
		this.instanceInfos = instanceInfos;
	}

	@Override
	public void run() {
		List<List<String>> work = new ArrayList<List<String>>();
		for (int i = 0; i < this.instanceInfos.size(); i++) {
			work.add(new ArrayList<String>());
		}

		List<String> lines = new ArrayList<String>();
		try {
			lines = Files.readAllLines(Paths.get(this.apkPathListFile));
			int idx = 0;
			for (String line : lines) {
				System.out.println(line);
				work.get(idx).add(line);
				idx = (idx + 1) % work.size();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < this.instanceInfos.size(); i++) {
			try {
				setup(this.instanceInfos.get(i), work.get(i));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void setup(MyInstanceInfo info, List<String> apks) throws Exception {
		SSHSession session = new SSHSession();
		session.setUser(ExperimentConfig.login);
		session.setAuthMethod(AuthMethod.Key);
		session.setHost(info.ip);
		session.setKeyFile(ExperimentConfig.privateKeyFile);
		session.connect();

		// Ensure we can send commands
		String login;
		do {
			login = session.sendCommand("whoami");
			System.out.println(login);
		} while (login == null);

		session.close();
	}

	private String makeS3DownloadCmd(String path, String dest) {
		return String.format("s3cmd get %s %s", path, dest);
	}

	public static void main(String[] args) throws Exception {
		AmazonEC2 conn = ExperimentHelper.getConnection();
		SetupInstancesAction step = new SetupInstancesAction(conn);
		List<MyInstanceInfo> infos = ExperimentHelper.readInstanceInfo();
		step.setApkPathListFile(ExperimentConfig.apkPathFile);
		step.setInstanceInfos(infos);
		step.run();
		// step.run();
	}
}
