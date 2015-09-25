package cert.didfail.analysis;

import java.util.Collection;
import java.util.HashMap;

import soot.Value;

@SuppressWarnings("serial")
public class FlowStorage extends HashMap<Value, ValueTypeMap> {
	// Set<ActionStringConstraint> constraints;
	private ActionStringConstraintSet constraints;

	public FlowStorage() {
		// constraints = new HashSet<ActionStringConstraint>();
		constraints = new ActionStringConstraintSet();
	}

	public void putAssignment(Value s, AssignedValue v) {
		ValueTypeMap map = new ValueTypeMap();
		map.addValue(v);
		this.put(s, map);
	}

	public void putAssignments(Value s, Collection<AssignedValue> vs) {
		ValueTypeMap map = new ValueTypeMap();
		for (AssignedValue v : vs) {
			map.addValue(v);
		}
		this.put(s, map);
	}

	public void copy(Value src, Value dest) {
		if (this.containsKey(src)) {
			ValueTypeMap srcMap = this.get(src);
			this.put(dest, new ValueTypeMap());
			for (ValueType v : srcMap.keySet()) {
				addAllAssignments(dest, srcMap.get(v));
			}
		}
	}

	public void merge(Value key, ValueTypeMap valueTypeMap) {
		try {
			if (!this.containsKey(key)) {
				this.put(key, new ValueTypeMap());
			}
			for (ValueType v : valueTypeMap.keySet()) {
				addAllAssignments(key, valueTypeMap.get(v));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addAssignment(Value s, AssignedValue v) {
		if (!this.containsKey(s)) {
			this.put(s, new ValueTypeMap());
		}
		this.get(s).addValue(v);
	}

	public void addAllAssignments(Value s, Collection<AssignedValue> vs) {
		for (AssignedValue v : vs) {
			this.addAssignment(s, v);
		}
	}

	public void clear() {
		super.clear();
		this.constraints.reset();
	}

	public void copy(FlowStorage dest) {
		// Copy map
		for (Value key : this.keySet()) {
			dest.put(key, this.get(key).clone());
		}

		// Copy constraints
		this.constraints.copy(dest.constraints);
	}

	public void merge(FlowStorage in2, FlowStorage out) {
		this.copy(out);

		try {
			for (Value key : in2.keySet()) {
				if (out.containsKey(key)) {
					ValueTypeMap vtm = in2.get(key);
					for (ValueType v : vtm.keySet()) {
						out.addAllAssignments(key, vtm.get(v));
					}
				} else {
					out.put(key, in2.get(key).clone());
				}
			}

			for (String p : in2.constraints.getPositive()) {
				out.constraints.or(p, false);
			}
			for (String p : in2.constraints.getNegative()) {
				out.constraints.or(p, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void andConstraint(String s, boolean not) {
		this.constraints.and(s, not);
	}

	public ActionStringConstraintSet getConstraintSet() {
		return this.constraints;
	}

	@Override
	public boolean equals(Object o) {
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCode();
	}
}
