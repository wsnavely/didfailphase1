package cert.action;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession;
import cert.SSHSession.AuthMethod;
import cert.config.ExperimentConfig;

public abstract class SSHSessionStep extends ExperimentStep {
	protected List<MyInstanceInfo> instanceInfos;

	public SSHSessionStep(AmazonEC2 ec2Conn, ExperimentConfig config) {
		super(ec2Conn, config);
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
				session.setUser(this.config.login);
				session.setAuthMethod(AuthMethod.Key);
				session.setHost(info.ip);
				session.setKeyFile(this.config.privateKeyFile);
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