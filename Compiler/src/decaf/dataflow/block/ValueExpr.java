package decaf.dataflow.block;

public class ValueExpr {
	private ValueExprOp op;
	private SymbolicValue val1;
	private SymbolicValue val2; // Only for binary expression
	
	public ValueExpr(SymbolicValue val) {
		this.val1 = val;
		this.val2 = null;
		this.op = ValueExprOp.NONE;
	}
	
	public ValueExpr(ValueExprOp op, SymbolicValue val) {
		this.val1 = val;
		this.val2 = null;
		this.op = op;
	}
	
	public ValueExpr(ValueExprOp op, SymbolicValue val1, SymbolicValue val2) {
		this.val1 = val1;
		this.val2 = val2;
		this.op = op;
	}

	public ValueExprOp getOp() {
		return op;
	}

	public void setOp(ValueExprOp op) {
		this.op = op;
	}

	public SymbolicValue getVal1() {
		return val1;
	}

	public void setVal1(SymbolicValue val1) {
		this.val1 = val1;
	}

	public SymbolicValue getVal2() {
		return val2;
	}

	public void setVal2(SymbolicValue val2) {
		this.val2 = val2;
	}
	
	@Override
	public int hashCode() {
		return 17 * val1.hashCode() + 19 * val2.hashCode() + 23 * op.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(ValueExpr.class)) return false;
		
		ValueExpr expr = (ValueExpr)o;
		if (expr.getOp() != this.getOp()) return false;
		
		// Check for commutative shit
		
		if (!expr.getVal1().equals(expr.getVal1())) return false;
		
		if (expr.getVal2() == null) {
			return (this.getVal2() == null);
		}
		
		return expr.getVal2().equals(this.getVal2());
	}
}
