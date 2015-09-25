package cert.didfail.flowdroid;

public class BooleanExpression {
	enum Operator {
		AND, OR;
	}

	class BooleanExpressionNode {
		BooleanExpressionNode left;
		BooleanExpressionNode right;

		public BooleanExpressionNode() {
			this.left = this.right = null;
		}

		public BooleanExpressionNode(BooleanExpressionNode left, BooleanExpressionNode right) {
			this.left = left;
			this.right = right;
		}
	}

	class BooleanExpressionTree extends BooleanExpressionNode {
		public BooleanExpressionTree(Operator op, BooleanExpressionNode left, BooleanExpressionNode right) {
			super(left, right);
			this.op = op;
		}

		Operator op;
	}

	class BooleanExpressionLeaf extends BooleanExpressionNode {
		public BooleanExpressionLeaf(String actionString, boolean notEquals) {
			super();
		}

		boolean negated;
		String actionString;
	}
}
