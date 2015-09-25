package cert.aws;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.util.SSHSession;
import cert.util.SSHSession.AuthMethod;

public abstract class SSHSessionStep extends ExperimentStep {
	protected List<MyInstanceInfo> instanceInfos;

	private String login;

	private String keyFile;

	public SSHSessionStep(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setInstanceInfos(List<MyInstanceInfo> instanceInfos) {
		this.instanceInfos = instanceInfos;
	}

	public abstract void runCommands(MyInstanceInfo info, SSHSession session);

	@Override
	public void runAction() {
		for (MyInstanceInfo info : this.instanceInfos) {
			SSHSession session = null;
			try {
				session = new SSHSession();
				session.setUser(this.login);
				session.setAuthMethod(AuthMethod.Key);
				session.setHost(info.ip);
				session.setKeyFile(this.keyFile);
				session.connect();
				session.testLiveness();
				runCommands(info, session);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					session.close();
				} catch (Exception e) {
				}
			}
		}
	}
}
