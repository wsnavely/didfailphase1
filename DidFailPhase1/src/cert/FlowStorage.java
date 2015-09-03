package cert;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import soot.Value;

@SuppressWarnings("serial")
public class FlowStorage extends HashMap<Value, ValueTypeMap> {
	Set<ActionStringConstraint> constraints;

	public FlowStorage() {
		constraints = new HashSet<ActionStringConstraint>();
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
		System.out.println("Add Assignment: " + s + "," + v);
		if (!this.containsKey(s)) {
			this.put(s, new ValueTypeMap());
		}
		this.get(s).addValue(v);
		System.out.println("Added Assignment");
	}

	public void addAllAssignments(Value s, Collection<AssignedValue> vs) {
		for (AssignedValue v : vs) {
			this.addAssignment(s, v);
		}
	}

	public void empty() {
		this.clear();
		this.constraints.clear();
	}

	public void copy(FlowStorage dest) {
		// Copy map
		for (Value key : this.keySet()) {
			dest.put(key, this.get(key).clone());
		}

		// Copy constraints
		for (ActionStringConstraint asc : this.constraints) {
			dest.constraints.add(asc.clone());
		}
	}

	public void merge(FlowStorage in2, FlowStorage out) {
		this.copy(out);
		System.out.println("merge");
		// Copy remaining constraints

		try {
			for (ActionStringConstraint asc : in2.constraints) {
				out.constraints.add(asc.clone());
			}

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
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
