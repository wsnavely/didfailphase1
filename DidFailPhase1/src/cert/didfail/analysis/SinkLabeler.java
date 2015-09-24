package cert.didfail.analysis;

import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.Hierarchy;
import soot.Local;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.AbstractInvokeExpr;

public class SinkLabeler extends SceneTransformer {
	static int numSendIntentMethods = 0;
	static String newField = "newField_";

	private static Local addTmpRef(Body body) {
		Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
		body.getLocals().add(tmpRef);
		return tmpRef;
	}

	public static boolean intentSinkMethod(Stmt stmt) {
		if (!stmt.containsInvokeExpr()) {
			return false;
		}

		SootClass activityClass;
		boolean isActivity;
		boolean result = false;
		String[] candidates = { "void startActivity(android.content.Intent)",
				"void startActivityForResult(android.content.Intent,int)",
				"void startActivityForResult(android.content.Intent,int,android.os.Bundle)" };

		AbstractInvokeExpr ie = (AbstractInvokeExpr) stmt.getInvokeExpr();
		SootMethod meth = ie.getMethod();
		String methodSubSig = meth.getSubSignature();

		for (String sig : candidates) {
			if (methodSubSig.contains(sig)) {
				activityClass = Scene.v().getSootClass("android.app.Activity");
				isActivity = (new Hierarchy()).isClassSuperclassOfIncluding(activityClass, meth.getDeclaringClass());
				if (isActivity == true) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	void transform(Body b) {
		final PatchingChain<Unit> units = b.getUnits();
		Value val2, val3;
		Type argType;
		Local tmpRef;

		for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			Unit u = iter.next();
			Stmt stmt = (Stmt) u;
			int id;

			boolean containsInvoke = stmt.containsInvokeExpr();
			boolean hasTag = stmt.hasTag("SinkTag");
			boolean isIntentSink = intentSinkMethod(stmt);

			if (!containsInvoke || !hasTag || !isIntentSink) {
				continue;
			}

			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			SinkTag tag = (SinkTag) stmt.getTag("SinkTag");
			id = tag.getId();

			for (Value arg : invokeExpr.getArgs()) {
				argType = arg.getType();
				if (argType.toString().contentEquals("android.content.Intent")) {
					String tempString = newField.concat(Integer.toString(id));

					tmpRef = addTmpRef(b);
					tmpRef = (Local) arg;
					SootMethod toCall = Scene.v().getSootClass("android.content.Intent")
							.getMethod("android.content.Intent putExtra(java.lang.String,java.lang.String)");
					val2 = StringConstant.v(tempString);
					val3 = StringConstant.v(tempString);
					units.insertBefore(Jimple.v()
							.newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), val2, val3)), u);
				}
			}
		}

	}

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		for (SootClass sc : Scene.v().getClasses()) {
			for (SootMethod m : sc.getMethods()) {
				try {
					Body b = m.retrieveActiveBody();
					transform(b);
					b.validate();
				} catch (Exception e) {
					continue;
				}
			}
		}
	}
}