package cert.didfail.flowdroid;

import java.util.HashSet;
import java.util.Set;

public class ActionStringConstraintSet {

	private Set<String> positive;
	private Set<String> negative;

	private boolean isUniverse;
	private boolean isEmpty;

	public ActionStringConstraintSet() {
		this.positive = new HashSet<String>();
		this.negative = new HashSet<String>();
		this.reset();
	}

	public void and(String s, boolean not) {
		if (this.isEmpty) {
			// Intersection with an empty set is empty
			return;
		}

		if (not) {
			if (positive.size() > 0) {
				if (positive.contains(s)) {
					if (positive.size() == 1) {
						this.empty();
					} else {
						this.positive.remove(s);
					}
				}
			} else {
				this.negative.add(s);
				this.isEmpty = false;
				this.isUniverse = false;
			}
		} else {
			if (this.negative.contains(s)) {
				this.empty();
			} else if (!this.positive.isEmpty()) {
				if (this.positive.contains(s)) {
					this.setOnly(s);
				} else {
					this.empty();
				}
			} else {
				this.setOnly(s);
			}
		}
	}

	public void or(String s, boolean not) {
		if (this.isUniverse) {
			return;
		}

		if (not) {
			if (!this.negative.isEmpty()) {
				if (this.negative.contains(s)) {
					this.setOnlyNegative(s);
				} else {
					this.reset();
				}
			} else {
				if (!this.positive.isEmpty()) {
					if (this.positive.contains(s)) {
						this.reset();
					} else {
						this.setOnlyNegative(s);
					}
				}
			}
		} else {
			if (!this.negative.isEmpty()) {
				if (this.negative.contains(s)) {
					this.negative.remove(s);
					if (this.negative.size() == 0) {
						this.reset();
					}
				}
			} else {
				if (!this.positive.isEmpty()) {
					this.positive.add(s);
				}
			}
		}
	}

	public void setOnly(String s) {
		this.positive.clear();
		this.negative.clear();
		this.positive.add(s);
		this.isUniverse = false;
		this.isEmpty = false;
	}

	public void setOnlyNegative(String s) {
		this.positive.clear();
		this.negative.clear();
		this.negative.add(s);
		this.isUniverse = false;
		this.isEmpty = false;
	}

	public void empty() {
		this.positive.clear();
		this.negative.clear();
		this.isUniverse = false;
		this.isEmpty = true;
	}

	public void reset() {
		this.positive.clear();
		this.negative.clear();
		this.isUniverse = true;
		this.isEmpty = false;
	}

	public void copy(ActionStringConstraintSet other) {
		other.reset();
		for(String s : this.positive) {
			other.positive.add(s);
		} 
		
		for(String s : this.negative) {
			other.negative.add(s);
		}
		
		other.isEmpty = this.isEmpty;
		other.isUniverse = this.isUniverse;
	}

	public boolean contains(String s) {
		if (this.isEmpty) {
			return false;
		} else if (this.isUniverse) {
			return true;
		} else if (!this.positive.isEmpty()) {
			return this.positive.contains(s);
		} else {
			return !this.negative.contains(s);
		}
	}

	public boolean isUniverse() {
		return this.isUniverse;
	}

	public boolean isEmpty() {
		return this.isEmpty;
	}
	
	public Iterable<String> getPositive() {
		return this.positive;
	}
	
	public Iterable<String> getNegative() {
		return this.negative;
	}
	
	
}
