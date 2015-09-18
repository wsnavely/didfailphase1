package cert.config;

public class ExperimentConfig {
	public static String experimentId = "bilbo";
	public static int instanceCount = 5;
	public static String instanceId = "ami-21295e44";
	public static String instanceType = "r3.xlarge";
	public static String login = "ubuntu";
	public static String securityGroupName = "MySecurityGroup";
	public static String securityGroupDesc = "Experiment security group.";
	public static String accessKey = "desktop_home";
	public static String privateKeyFile = "/home/osboxes/desktop_home.pem";
	public static String apkPathFile = "/home/osboxes/git/didfailphase1/DidfailAWS/input_paths";
	public static boolean useProxy = true;
	public static String proxyHost = "proxy.sei.cmu.edu";
	public static int proxyPort = 8080;
}
