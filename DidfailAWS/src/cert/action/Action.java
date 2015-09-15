package cert.action;

import com.amazonaws.services.ec2.AmazonEC2;

public abstract class Action {
	protected AmazonEC2 ec2Conn;

	public Action(AmazonEC2 ec2Conn) {
		this.ec2Conn = ec2Conn;
	}

	public abstract void run();

	protected void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
