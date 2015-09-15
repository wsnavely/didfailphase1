package cert;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession.AuthMethod;

public class RunExperimentAction extends Action {

	public RunExperimentAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	public RunExperimentAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
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
