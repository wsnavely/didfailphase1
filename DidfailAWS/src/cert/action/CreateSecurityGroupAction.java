package cert.action;

import java.util.ArrayList;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.IpPermission;

import cert.config.ExperimentConfig;

public class CreateSecurityGroupAction extends Action {
	private String name;
	private String description;

	public CreateSecurityGroupAction(AmazonEC2 ec2Conn) {
		super(ec2Conn);
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
		AmazonEC2 conn = ExperimentHelper.getConnection();
		CreateSecurityGroupAction step = new CreateSecurityGroupAction(conn);
		step.setDescription(ExperimentConfig.securityGroupDesc);
		step.setName(ExperimentConfig.securityGroupName);
		step.runAction();
	}
}
