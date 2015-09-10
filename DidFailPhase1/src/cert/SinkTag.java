package cert;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class SinkTag implements Tag {

	private int id;

	public SinkTag(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	@Override
	public String getName() {
		return "SinkTag";
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		return null;
	}
}
