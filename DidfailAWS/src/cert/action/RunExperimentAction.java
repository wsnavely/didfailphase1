package cert.action;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;

import cert.SSHSession;

public class RunExperimentAction extends SSHSessionAction {
	public RunExperimentAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
	}

	@Override
	public void runCommands(MyInstanceInfo info, SSHSession session) {
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String cmd = makeAnalysisCommand();
						System.out.println(cmd);
						session.sendCommand(cmd);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String makeAnalysisCommand() {
		return "cd /home/ubuntu/flowdroid-runner; nohup python /home/ubuntu/flowdroid-runner/run.py -Xmx20000m &";
	}

	public static void main(String[] args) throws Exception {
		AmazonEC2 conn = ExperimentHelper.getConnection();
		List<MyInstanceInfo> infos = ExperimentHelper.getInstanceInfo(conn);
		RunExperimentAction step = new RunExperimentAction(conn);
		step.setInstanceInfos(infos);
		step.runAction();
	}
}
