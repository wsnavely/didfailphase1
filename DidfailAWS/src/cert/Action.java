package cert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;

public abstract class Action {
	protected AmazonEC2 ec2Conn;

	public Action() throws Exception {
		this.connectToEc2();
	}

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

	private void connectToEc2() throws IOException {
		AWSCredentials credentials = null;
		credentials = new PropertiesCredentials(Action.class.getResourceAsStream("AwsCredentials.properties"));
		this.ec2Conn = new AmazonEC2Client(credentials);
	}

	public List<String> readIds(String path) {
		List<String> ids = new ArrayList<String>();
		try {
			ids = Files.readAllLines(Paths.get(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ids;
	}
}
