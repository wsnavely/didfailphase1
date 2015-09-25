package cert.didfail.analysis;

import soot.Value;

public class AssignedValue extends TaggedValue<ValueType> {

	public AssignedValue(Value value) {
		super(value);
	}

	public AssignedValue(Value value, ValueType type) {
		super(value, type);
	}

}
