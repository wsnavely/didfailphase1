package cert.didfail.analysis;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;

public abstract class AndroidInfoflowResultsHandler implements ResultsAvailableHandler {
	private String appPackageName;
	
	public AndroidInfoflowResultsHandler() {
		this.appPackageName = "";
	}
	
	public void setAppPackage(String name) {
		this.appPackageName = name;
	}
	
	public String getAppPackage() {
		return this.appPackageName;
	}
}
