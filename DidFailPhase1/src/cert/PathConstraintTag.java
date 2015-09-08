package cert;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class PathConstraintTag implements Tag {
	public boolean negate;
	public String name;
	public String string;

	public PathConstraintTag(String string, boolean negate) {
		this.negate = negate;
		this.string = string;

		String prefix = "PathConstraint:";
		prefix += this.negate ? "!" : "";
		this.name = prefix + this.string;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		return null;
	}

	public boolean isNegatedO() {
		return this.negate;
	}

}
