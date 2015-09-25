import edu.psu.cse.siis.ic3.Ic3Analysis;
import edu.psu.cse.siis.ic3.Ic3CommandLineArguments;
import edu.psu.cse.siis.ic3.Ic3CommandLineParser;

public class IC3Wrapper {
	private String apkPath;
	private String retargetedPath;
	private String androidJarPath;

	public IC3Wrapper() {
	}

	public void setAndroidJarPath(String androidJarPath) {
		this.androidJarPath = androidJarPath;
	}

	public void setApkPath(String apkPath) {
		this.apkPath = apkPath;
	}

	public void setRetargetedPath(String retargetedPath) {
		this.retargetedPath = retargetedPath;
	}

	public void runAnalysis() {
		// @formatter:off
		String[] args = new String[] {
			"-input", this.retargetedPath,
			"-apkormanifest", this.apkPath,
			"-cp", this.androidJarPath
		};
		// @formatter:on

		Ic3Analysis analysis = new MyIc3Analysis();
		Ic3CommandLineParser parser = new Ic3CommandLineParser();
		Ic3CommandLineArguments commandLineArguments = parser.parseCommandLine(args, Ic3CommandLineArguments.class);
		commandLineArguments.processCommandLineArguments();
		analysis.performAnalysis(commandLineArguments);
	}
}