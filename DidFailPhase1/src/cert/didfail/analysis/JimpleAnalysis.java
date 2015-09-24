package cert.didfail.analysis;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NopStmt;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;

public abstract class JimpleAnalysis<T> extends ForwardBranchedFlowAnalysis<T> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public JimpleAnalysis(UnitGraph graph) {
		super(graph);
	}

	protected void flowThroughNop(T in, NopStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughIdentity(T in, IdentityStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughAssign(T in, AssignStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughIf(T in, IfStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughGoto(T in, GotoStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughTableSwitch(T in, TableSwitchStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughLookupSwitch(T in, LookupSwitchStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughInvoke(T in, InvokeStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughReturn(T in, ReturnStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughReturnVoid(T in, ReturnVoidStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughEnterMonitor(T in, EnterMonitorStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughExitMonitor(T in, ExitMonitorStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughThrow(T in, ThrowStmt s, List<T> fall, List<T> branch) {
		return;
	}

	protected void flowThroughRet(T in, RetStmt s, List<T> fall, List<T> branch) {
		return;
	}

	@Override
	protected void flowThrough(T in, Unit s, List<T> fall, List<T> branch) {
		if (s instanceof NopStmt) {
			flowThroughNop(in, (NopStmt) s, fall, branch);
		} else if (s instanceof IdentityStmt) {
			flowThroughIdentity(in, (IdentityStmt) s, fall, branch);
		} else if (s instanceof AssignStmt) {
			flowThroughAssign(in, (AssignStmt) s, fall, branch);
		} else if (s instanceof IfStmt) {
			flowThroughIf(in, (IfStmt) s, fall, branch);
		} else if (s instanceof GotoStmt) {
			flowThroughGoto(in, (GotoStmt) s, fall, branch);
		} else if (s instanceof TableSwitchStmt) {
			flowThroughTableSwitch(in, (TableSwitchStmt) s, fall, branch);
		} else if (s instanceof LookupSwitchStmt) {
			flowThroughLookupSwitch(in, (LookupSwitchStmt) s, fall, branch);
		} else if (s instanceof InvokeStmt) {
			flowThroughInvoke(in, (InvokeStmt) s, fall, branch);
		} else if (s instanceof ReturnStmt) {
			flowThroughReturn(in, (ReturnStmt) s, fall, branch);
		} else if (s instanceof ReturnVoidStmt) {
			flowThroughReturnVoid(in, (ReturnVoidStmt) s, fall, branch);
		} else if (s instanceof EnterMonitorStmt) {
			flowThroughEnterMonitor(in, (EnterMonitorStmt) s, fall, branch);
		} else if (s instanceof ExitMonitorStmt) {
			flowThroughExitMonitor(in, (ExitMonitorStmt) s, fall, branch);
		} else if (s instanceof ThrowStmt) {
			flowThroughThrow(in, (ThrowStmt) s, fall, branch);
		} else if (s instanceof RetStmt) {
			flowThroughRet(in, (RetStmt) s, fall, branch);
		}
	}
}