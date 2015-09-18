package cert.action;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession;
import cert.SSHSession.AuthMethod;
import cert.config.ExperimentConfig;

public abstract class SSHSessionAction extends Action {
	protected List<MyInstanceInfo> instanceInfos;

	public SSHSessionAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
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
				session.setUser(ExperimentConfig.login);
				session.setAuthMethod(AuthMethod.Key);
				session.setHost(info.ip);
				session.setKeyFile(ExperimentConfig.privateKeyFile);
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
