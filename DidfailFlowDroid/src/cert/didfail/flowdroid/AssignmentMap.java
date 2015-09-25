package cert.didfail.flowdroid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import soot.Value;

public class AssignmentMap extends HashMap<Value, List<AssignedValue>> {

	public void putAssignment(Value s, AssignedValue v) {
		ArrayList<AssignedValue> vs = new ArrayList<AssignedValue>();
		vs.add(v);
		this.put(s, vs);
	}

	public void putAssignments(Value s, Collection<AssignedValue> vs) {
		ArrayList<AssignedValue> list = new ArrayList<AssignedValue>(vs);
		this.put(s, list);
	}

	public void addAssignment(Value s, AssignedValue v) {
		if (!this.containsKey(s)) {
			this.put(s, new ArrayList<AssignedValue>());
		}
		this.get(s).add(v);
	}

	public void addAllAssignments(Value s, Collection<AssignedValue> vs) {
		if (!this.containsKey(s)) {
			this.put(s, new ArrayList<AssignedValue>());
		}
		this.get(s).addAll(vs);
	}
}
