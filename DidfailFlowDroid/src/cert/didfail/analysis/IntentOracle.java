package cert.didfail.analysis;

import soot.SootMethod;
import soot.Value;

public interface IntentOracle {
	boolean isGetAction(SootMethod sm);

	boolean isIntentSource(SootMethod sm);

	boolean isIntentType(Value v);
}