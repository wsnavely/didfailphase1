package cert;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession.AuthMethod;

public class SetupInstancesAction extends Action {

	public SetupInstancesAction() throws Exception {
		super();
	}

	public SetupInstancesAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	@Override
	public void run() {

	}

	public static void main(String[] args) throws Exception {
		RunExperimentAction step = new RunExperimentAction();
		step.readIds("ids");
		SSHSession session = new SSHSession();
		session.setUser("ubuntu");
		session.setAuthMethod(AuthMethod.Key);
		session.setHost("54.146.44.142");
		session.setKeyFile(ExperimentConfig.privateKeyFile);
		session.connect();
		System.out.println(session.sendCommand("whoami"));
		System.out.println(session.sendCommand("whereis git"));
		session.close();
		// step.run();
	}
}
