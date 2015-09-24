package cert.didfail.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ValueTypeMap extends HashMap<ValueType, List<AssignedValue>> {
	public ValueTypeMap() {
		for (ValueType t : ValueType.values()) {
			this.put(t, new ArrayList<AssignedValue>());
		}
	}

	public void addValue(AssignedValue value) {
		this.get(value.getTag()).add(value);
	}

	public ValueTypeMap clone() {
		ValueTypeMap clone = new ValueTypeMap();
		for (ValueType type : this.keySet()) {
			clone.put(type, new ArrayList<AssignedValue>(this.get(type)));
		}
		return clone;
	}
}
