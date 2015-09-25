package cert.didfail.flowdroid;

import soot.SootClass;
import soot.SootMethod;
import soot.Value;

public class AndroidIntentOracle implements IntentOracle {

	@Override
	public boolean isGetAction(SootMethod sm) {
		SootClass sc = sm.getDeclaringClass();
		String className = sc.getName();
		String methodName = sm.getName();

		if (className.equals("android.content.Intent")) {
			if (methodName.equals("getAction")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isIntentSource(SootMethod sm) {
		SootClass sc = sm.getDeclaringClass();
		String className = sc.getName();
		String methodName = sm.getName();
		if (className.equals("android.app.Activity")) {
			if (methodName.equals("startActivityForResult")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isIntentType(Value v) {
		return v.getType().toString().equals("android.content.Intent");
	}
}
