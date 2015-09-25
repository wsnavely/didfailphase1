package cert.didfail.analysis;

public class ActionStringConstraint {
	private String actionString;
	private boolean isComplement;

	public ActionStringConstraint(String string, boolean isComplement) {
		this.actionString = string;
		this.isComplement = isComplement;
	}

	public String getActionString() {
		return this.actionString;
	}

	public boolean getIsComplement() {
		return this.isComplement;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ActionStringConstraint) {
			ActionStringConstraint asc = (ActionStringConstraint) obj;
			return this.actionString.equals(asc.actionString) && (this.isComplement == asc.isComplement);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.actionString.hashCode() ^ ((Boolean) this.isComplement).hashCode();
	}

	public ActionStringConstraint clone() {
		return new ActionStringConstraint(this.actionString, this.isComplement);
	}
}
