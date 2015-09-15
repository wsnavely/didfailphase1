package cert.action;

import com.amazonaws.services.ec2.AmazonEC2;

public class RunExperimentAction extends Action {
	public RunExperimentAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
	}

	public static void main(String[] args) throws Exception {
		AmazonEC2 conn = ExperimentHelper.getConnection();
		RunExperimentAction step = new RunExperimentAction(conn);

	}
}
