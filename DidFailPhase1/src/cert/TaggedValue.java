package cert;

import soot.Value;

public class TaggedValue<T> {
	private T tag;
	private Value value;

	public TaggedValue(Value value) {
		this.value = value;
	}

	public TaggedValue(Value value, T tag) {
		this.value = value;
		this.tag = tag;
	}

	public T getTag() {
		return this.tag;
	}

	public void setTag(T tag) {
		this.tag = tag;
	}
}