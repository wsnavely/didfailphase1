package cert.aws;

import com.amazonaws.services.ec2.AmazonEC2;

public abstract class ExperimentStep {

	protected AmazonEC2 ec2Conn;
	public ExperimentStep(AmazonEC2 ec2Conn) {
		this.ec2Conn = ec2Conn;
	}

	public abstract void runAction();

	protected void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
