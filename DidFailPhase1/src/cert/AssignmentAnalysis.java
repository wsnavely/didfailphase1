package cert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JNeExpr;
import soot.toolkits.graph.UnitGraph;

public class AssignmentAnalysis extends JimpleAnalysis<AssignmentMap> {

	private IntentOracle io;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public AssignmentAnalysis(UnitGraph graph) {
		super(graph);
		io = new AndroidIntentOracle();
		doAnalysis();
	}

	@Override
	protected void flowThrough(AssignmentMap in, Unit s, List<AssignmentMap> fall, List<AssignmentMap> branch) {
		for (AssignmentMap map : fall) {
			map.putAll(in);
		}
		for (AssignmentMap map : branch) {
			map.putAll(in);
		}
		super.flowThrough(in, s, fall, branch);
	}

	private void printAssignments(AssignmentMap m) {
		System.out.println("Assignments");
		System.out.println("-----------------");
		for (Object key : m.keySet()) {
			System.out.print(key + ": ");
			boolean first = true;
			for (Object val : (List) m.get(key)) {
				if (first) {
					first = false;
				} else {
					System.out.print(", ");
				}
				System.out.print(val);
			}
			System.out.println();
		}
		System.out.println("-----------------");
	}

	@Override
	protected void flowThroughAssign(AssignmentMap in, AssignStmt s, List<AssignmentMap> fall,
			List<AssignmentMap> branch) {
		// List lst = new ArrayList();
		// lst.add(stmt.getRightOp());
		// fallOut.get(0).put(stmt.getLeftOp(), lst);
		Value lhs = s.getLeftOp();
		Value rhs = s.getRightOp();
		handleAssignment(lhs, rhs, fall.get(0));
	}

	private void handleAssignment(Value lhs, Value rhs, AssignmentMap map) {
		if (map.containsKey(rhs)) {
			map.putAssignments(lhs, map.get(rhs));
			System.out.println("ALIAS: " + lhs + " = " + rhs);
		} else if (isCallToGetAction(rhs)) {
			map.putAssignment(lhs, new AssignedValue(rhs, ValueType.ACTION_STRING));
			System.out.println("ACTION STRING: " + lhs + " = " + rhs);
		} else if (rhs instanceof StringConstant) {
			map.putAssignment(lhs, new AssignedValue(rhs, ValueType.STR_CONST));
			System.out.println("STRING CONSTANT: " + lhs + " = " + rhs);
		} else if (isActionStringComparison(rhs, map)) {
			map.putAssignment(lhs, new AssignedValue(rhs, ValueType.ACTION_STRING_CMP));
			System.out.println("ACTION STRING COMPARISON: " + lhs + " = " + rhs);
		} else if (map.containsKey(lhs)) {
			map.remove(lhs);
		}
	}

	@Override
	protected void flowThroughIf(AssignmentMap in, IfStmt stmt, List<AssignmentMap> fall, List<AssignmentMap> branch) {
		Value cond = stmt.getCondition();

		if (cond instanceof JEqExpr || cond instanceof JNeExpr) {
			AbstractBinopExpr eq = (AbstractBinopExpr) cond;
			Value lop = eq.getOp1();
			Value rop = eq.getOp2();

			Value actionStringCmp = null;
			Value other = null;

			if (getTypes(in, lop).contains(ValueType.ACTION_STRING_CMP)) {
				actionStringCmp = lop;
				other = rop;
			} else if (getTypes(in, rop).contains(ValueType.ACTION_STRING_CMP)) {
				actionStringCmp = rop;
				other = lop;
			}

			if (actionStringCmp != null && other != null) {
				System.out.println("BRANCH: " + stmt);
			}
		}
	}

	private boolean isCallToGetAction(Value rhs) {
		if (rhs instanceof InvokeExpr) {
			InvokeExpr ie = (InvokeExpr) rhs;
			SootMethod sm = ie.getMethod();
			if (this.io.isGetAction(sm)) {
				return true;
			}
		}
		return false;
	}

	Set<ValueType> getTypes(AssignmentMap map, Value dest) {
		Set<ValueType> types = new HashSet<ValueType>();
		if (map.containsKey(dest)) {
			for (AssignedValue v : map.get(dest)) {
				types.add(v.getTag());
			}
		}
		return types;
	}

	private boolean isActionStringComparison(Value rhs, AssignmentMap map) {
		if (isCallToStringEquals(rhs)) {

			InvokeExpr ie = (InvokeExpr) rhs;
			Set<ValueBox> boxes = new HashSet<ValueBox>(rhs.getUseBoxes());
			ValueBox argBox = ie.getArgBox(0);
			boxes.remove(argBox);

			Value lop = argBox.getValue();
			Value rop = null;
			for (ValueBox v : boxes) {
				rop = v.getValue();
				break;
			}

			Set<ValueType> ropTypes = getTypes(map, rop);
			Set<ValueType> lopTypes = getTypes(map, lop);

			boolean ropIsStrConst = ropTypes.contains(ValueType.STR_CONST) || rop instanceof StringConstant;
			boolean lopIsStrConst = lopTypes.contains(ValueType.STR_CONST) || lop instanceof StringConstant;

			if (ropTypes.contains(ValueType.ACTION_STRING) && lopIsStrConst) {
				return true;
			}

			if (lopTypes.contains(ValueType.ACTION_STRING) && ropIsStrConst) {
				return true;
			}
		}
		return false;
	}

	private boolean isCallToStringEquals(Value rhs) {
		if (rhs instanceof InvokeExpr) {
			InvokeExpr ie = (InvokeExpr) rhs;
			SootMethod sm = ie.getMethod();
			SootClass sc = sm.getDeclaringClass();
			String className = sc.getName();
			String methodName = sm.getName();

			if (className.equals("java.lang.String")) {
				if (methodName.equals("equals")) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected AssignmentMap newInitialFlow() {
		return new AssignmentMap();
	}

	@Override
	protected void merge(AssignmentMap in1, AssignmentMap in2, AssignmentMap out) {
		out.putAll((in1));
		for (Value key : in2.keySet()) {
			if (out.containsKey(key)) {
				out.addAllAssignments(key, in2.get(key));
			} else {
				out.put(key, in2.get(key));
			}
		}
	}

	@Override
	protected void copy(AssignmentMap source, AssignmentMap dest) {
		dest.putAll(source);
	}
}
