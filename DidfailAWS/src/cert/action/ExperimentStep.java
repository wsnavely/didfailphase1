package cert.action;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.config.ExperimentConfig;

public abstract class ExperimentStep {

	protected AmazonEC2 ec2Conn;
	protected ExperimentConfig config;

	public ExperimentStep(AmazonEC2 ec2Conn, ExperimentConfig config) {
		this.ec2Conn = ec2Conn;
		this.config = config;
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
