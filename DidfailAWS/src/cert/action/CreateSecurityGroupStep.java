package cert.action;

import java.util.ArrayList;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.IpPermission;
import com.beust.jcommander.JCommander;

import cert.config.ExperimentConfig;

public class CreateSecurityGroupStep extends ExperimentStep {
	private String name;
	private String description;

	public CreateSecurityGroupStep(AmazonEC2 ec2Conn, ExperimentConfig config) {
		super(ec2Conn, config);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void runAction() {
		try {
			CreateSecurityGroupRequest secGroupRequest = new CreateSecurityGroupRequest(this.name, this.description);
			ec2Conn.createSecurityGroup(secGroupRequest);
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}

		String ipAddr = "0.0.0.0/0";
		ArrayList<String> ipRanges = new ArrayList<String>();
		ipRanges.add(ipAddr);

		ArrayList<IpPermission> ipPermissions = new ArrayList<IpPermission>();
		IpPermission ipPermission = new IpPermission();
		ipPermission.setIpProtocol("tcp");
		ipPermission.setFromPort(new Integer(22));
		ipPermission.setToPort(new Integer(22));
		ipPermission.setIpRanges(ipRanges);
		ipPermissions.add(ipPermission);

		try {
			AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest(this.name,
					ipPermissions);
			ec2Conn.authorizeSecurityGroupIngress(ingressRequest);
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
	}

	public static void main(String[] args) throws Exception {
		ExperimentArgs jct = new ExperimentArgs();
		new JCommander(jct, args);
		ExperimentConfig config = ExperimentConfig.loadConfig(jct.workingDir);
		AmazonEC2 conn = ExperimentHelper.getConnection(config);
		CreateSecurityGroupStep step = new CreateSecurityGroupStep(conn, config);
		step.setDescription(config.securityGroupDesc);
		step.setName(config.securityGroupName);
		step.runAction();
	}
}
