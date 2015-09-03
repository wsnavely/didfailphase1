package old;

public class BoolExpr {
	abstract class Operand {
	}

	class IntentPropertyOperand extends Operand {
	}

	class StringConstantOperand extends Operand {
	}

	abstract class Operator {
	}

	class Equals extends Operator {
	}

	class NotEquals extends Operator {
	}

	Operand op1;
	Operand op2;
	Operator op;

	public static String and(String e1, String e2) {
		if (e1 == null || e1.isEmpty()) {
			return e2;
		}
		if (e2 == null || e2.isEmpty()) {
			return e1;
		}
		return String.format("(%s AND %s)", e1, e2);
	}

	public static String or(String e1, String e2) {
		if (e1 == null || e1.isEmpty()) {
			return e2;
		}
		if (e2 == null || e2.isEmpty()) {
			return e1;
		}
		return String.format("(%s OR %s)", e1, e2);
	}
}
