//package soot.jimple.infoflow.android.TestApps;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import soot.SootClass;
//import soot.SootMethod;
//import soot.UnitBox;
//import soot.Value;
//import soot.ValueBox;
//import soot.jimple.AssignStmt;
//import soot.jimple.IfStmt;
//import soot.jimple.InvokeExpr;
//import soot.jimple.StringConstant;
//import soot.jimple.infoflow.android.TestApps.BETSet.Comparison;
//import soot.jimple.internal.AbstractBinopExpr;
//import soot.jimple.internal.JEqExpr;
//import soot.jimple.internal.JNeExpr;
//import soot.toolkits.graph.CompleteUnitGraph;
//import soot.toolkits.graph.UnitGraph;
//import soot.toolkits.scalar.LocalDefs;
//import soot.toolkits.scalar.SimpleLocalDefs;
//
//public class BooleanExpressionTagger extends JimpleAnalysis {
//
//	private final Logger logger = LoggerFactory.getLogger(getClass());
//
//	private AktFlowProcessor fp;
//	
//	public BooleanExpressionTagger(UnitGraph graph) {
//		super(graph);
//		fp = new AndroidFlowProcessor();
//		doAnalysis();
//	}
//
//	@Override
//	protected void flowThroughAssign(BETSet in, AssignStmt stmt,
//			List<BETSet> fallOut, List<BETSet> branchOuts) {
//		Value left = stmt.getLeftOp();
//		Value right = stmt.getRightOp();
//		BETSet out = fallOut.get(0);
//
//		if (out.isIntentProperty(right)) {
//			out.addIntentPropertyAlias(left, right);
//			logger.info("[AKTION][INTENTALIAS] " + stmt);
//		} else if (out.isStringConstant(right)) {
//			out.addStringConstantAlias(left, right);
//			logger.info("[AKTION][STRALIAS] " + stmt);
//		} else if (right instanceof StringConstant) {
//			out.addStringConstant(left, right);
//			logger.info("[AKTION][STRSRC] " + stmt);
//		} else if (stmt.containsInvokeExpr()) {
//			InvokeExpr ie = stmt.getInvokeExpr();
//			SootMethod sm = ie.getMethod();
//			if (fp.isIntentPropertyGetter(sm)) {
//				out.addIntentPropertySource(left, ie);
//				logger.info("[AKTION][INTENTSRC] " + stmt);
//			} else if (isStringEquals(sm)) {
//				Set<ValueBox> boxes = new HashSet<ValueBox>(ie.getUseBoxes());
//				ValueBox argBox = ie.getArgBox(0);
//				boxes.remove(argBox);
//				Value arg = argBox.getValue();
//				Value caller = null;
//				for (ValueBox v : boxes) {
//					caller = v.getValue();
//					break;
//				}
//				out.processComparison(left, arg, caller);
//			}
//		}
//	}
//
//	@Override
//	protected void flowThroughIf(BETSet in, IfStmt stmt, List<BETSet> fallOut,
//			List<BETSet> branchOuts) {
//		Value cond = stmt.getCondition();
//		for(UnitBox u : stmt.getUnitBoxes()) {
//			System.out.println(u);
//		}
//		if (cond instanceof JEqExpr || cond instanceof JNeExpr) {
//			AbstractBinopExpr eq = (AbstractBinopExpr) cond;
//			Value left = eq.getOp1();
//			Value right = eq.getOp2();
//			Value other = null;
//			Comparison<Value, Value> cmp = null;
//			String op = cond instanceof JEqExpr ? "!=" : "==";
//			String opNeg = cond instanceof JEqExpr ? "==" : "!=";
//
//			if (in.isIntentPropertyCmp(left)) {
//				cmp = in.getIntentPropertyCmp(left);
//				other = right;
//			} else if (in.isIntentProperty(right)) {
//				cmp = in.getIntentPropertyCmp(right);
//				other = left;
//			}
//
//			if (cmp != null) {
//				logger.info("[AKTION][BRANCH] " + stmt);
//				String expr = String.format("(%s %s %s)", fmt(cmp.x), op,
//						fmt(cmp.y));
//				String exprNeg = String.format("(%s %s %s)", fmt(cmp.x), opNeg,
//						fmt(cmp.y));
//				String fallBET = BoolExpr.and(in.booleanExpression, exprNeg);
//				String branchBET = BoolExpr.and(
//						branchOuts.get(0).booleanExpression, expr);
//				fallOut.get(0).booleanExpression = fallBET;
//				branchOuts.get(0).booleanExpression = branchBET;
//			}
//		}
//	}
//
//	private String fmt(Value val) {
//		if (val instanceof InvokeExpr) {
//			InvokeExpr ie = (InvokeExpr) val;
//			String method = ie.getMethod().getName();
//
//			String argStr = "";
//			boolean first = true;
//			for (Value arg : ie.getArgs()) {
//				if (first) {
//					first = false;
//				} else {
//					argStr += ",";
//				}
//				argStr += arg.toString();
//			}
//
//			Set<ValueBox> boxes = new HashSet<ValueBox>(ie.getUseBoxes());
//			Value caller = null;
//			for (ValueBox v : boxes) {
//				caller = v.getValue();
//				break;
//			}
//			return "(" + caller.toString() + "." + method + ")";
//		} else {
//			return val.toString();
//		}
//	}
//
//	private boolean isStringEquals(SootMethod sm) {
//		SootClass sc = sm.getDeclaringClass();
//		String className = sc.getName();
//		String methodName = sm.getName();
//		if (className.equals("java.lang.String")) {
//			if (methodName.equals("equals")) {
//				return true;
//			}
//		}
//		return false;
//	}
//}