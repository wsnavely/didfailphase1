package cert;

import java.util.ArrayList;
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

public class AssignmentAnalysis extends JimpleAnalysis<FlowStorage> {

	private IntentOracle io;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public AssignmentAnalysis(UnitGraph graph) {
		super(graph);
		io = new AndroidIntentOracle();
		doAnalysis();
	}

	@Override
	protected void flowThrough(FlowStorage in, Unit s, List<FlowStorage> fall, List<FlowStorage> branch) {
		try {
			ActionStringConstraintSet cs = in.getConstraintSet();
			for (String p : cs.getPositive()) {
				logger.debug("CONSTRAINT: " + p);
				s.addTag(new PathConstraintTag(p, false));
			}
			for (String p : cs.getNegative()) {
				logger.debug("CONSTRAINT: !(" + p + ")");
				s.addTag(new PathConstraintTag(p, true));
			}

			for (FlowStorage map : fall) {
				in.copy(map);
			}
			for (FlowStorage map : branch) {
				in.copy(map);
			}
			super.flowThrough(in, s, fall, branch);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void flowThroughAssign(FlowStorage in, AssignStmt s, List<FlowStorage> fall, List<FlowStorage> branch) {
		Value lhs = s.getLeftOp();
		Value rhs = s.getRightOp();
		handleAssignment(lhs, rhs, fall.get(0));
	}

	private void handleAssignment(Value lhs, Value rhs, FlowStorage map) {
		Set<String> actionStrings;

		if (map.containsKey(rhs)) {
			map.copy(rhs, lhs);
			logger.debug("ALIAS: " + lhs + " = " + rhs);
		} else if (isCallToGetAction(rhs)) {
			map.putAssignment(lhs, new AssignedValue(rhs, ValueType.ACTION_STRING));
			logger.debug("ACTION STRING: " + lhs + " = " + rhs);
		} else if (rhs instanceof StringConstant) {
			map.putAssignment(lhs, new AssignedValue(rhs, ValueType.STR_CONST));
			logger.debug("STRING CONSTANT: " + lhs + " = " + rhs);
		} else if (!(actionStrings = getConstantsComparedToActionString(rhs, map)).isEmpty()) {
			logger.debug("ACTION STRING COMPARISON: " + lhs + " = " + rhs);
			List<AssignedValue> values = new ArrayList<AssignedValue>();
			AssignedValue val;
			for (String s : actionStrings) {
				StringConstant sc = StringConstant.v(s);
				val = new AssignedValue(sc, ValueType.ACTION_STRING_CMP);
				values.add(val);
			}
			map.putAssignments(lhs, values);
		} else if (map.containsKey(lhs)) {
			map.remove(lhs);
		}
	}

	@Override
	protected void flowThroughIf(FlowStorage in, IfStmt stmt, List<FlowStorage> fall, List<FlowStorage> branch) {
		Value cond = stmt.getCondition();

		if (cond instanceof JEqExpr || cond instanceof JNeExpr) {
			AbstractBinopExpr eq = (AbstractBinopExpr) cond;
			Value lop = eq.getOp1();
			Value rop = eq.getOp2();
			boolean stringEquals = cond instanceof JNeExpr;

			Set<String> actionStrings = new HashSet<String>();
			if (in.containsKey(lop)) {
				ValueTypeMap lopTypes = in.get(lop);
				for (AssignedValue val : lopTypes.get(ValueType.ACTION_STRING_CMP)) {
					actionStrings.add(((StringConstant) val.getValue()).value);
				}
			}

			if (in.containsKey(rop)) {
				ValueTypeMap ropTypes = in.get(rop);
				for (AssignedValue val : ropTypes.get(ValueType.ACTION_STRING_CMP)) {
					actionStrings.add(((StringConstant) val.getValue()).value);
				}
			}

			if (stringEquals) {
				logger.debug("ACTION STRING EQUALS: " + stmt);
				for (String s : actionStrings) {
					branch.get(0).andConstraint(s, false);
					fall.get(0).andConstraint(s, true);
				}
			} else {
				logger.debug("ACTION STRING NOT EQUALS: " + stmt);
				for (String s : actionStrings) {
					branch.get(0).andConstraint(s, true);
					fall.get(0).andConstraint(s, false);
				}
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

	private Set<String> getConstantsComparedToActionString(Value rhs, FlowStorage map) {
		Set<String> result = new HashSet<String>();
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

			ValueTypeMap lopTypes = new ValueTypeMap();
			ValueTypeMap ropTypes = new ValueTypeMap();
			if (map.containsKey(lop)) {
				lopTypes = map.get(lop);
			}
			if (map.containsKey(rop)) {
				ropTypes = map.get(rop);
			}

			if (!ropTypes.get(ValueType.ACTION_STRING).isEmpty()) {
				if (lop instanceof StringConstant) {
					result.add(((StringConstant) lop).value);
				}
				for (AssignedValue val : lopTypes.get(ValueType.STR_CONST)) {
					result.add(((StringConstant) val.getValue()).value);
				}
			}

			if (!lopTypes.get(ValueType.ACTION_STRING).isEmpty()) {
				if (rop instanceof StringConstant) {
					result.add(((StringConstant) rop).value);
				}
				for (AssignedValue val : ropTypes.get(ValueType.STR_CONST)) {
					result.add(((StringConstant) val.getValue()).value);
				}
			}
		}
		return result;
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
	protected FlowStorage newInitialFlow() {
		return new FlowStorage();
	}

	@Override
	protected void merge(FlowStorage in1, FlowStorage in2, FlowStorage out) {
		out.clear();
		in1.merge(in2, out);
	}

	@Override
	protected void copy(FlowStorage source, FlowStorage dest) {
		dest.clear();
		source.copy(dest);
	}
}
