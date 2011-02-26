package decaf.ir.ast;

public class ArrayLocation extends Location {
	private Expression expr;
	
	public ArrayLocation(String id, Expression expr) {
		this.id = id;
		this.expr = expr;
	}
	
	public void setExpr(Expression expr) {
		this.expr = expr;
	}
	
	public Expression getExpr() {
		return expr;
	}
	
	@Override
	public String toString() {
		return id + "[" + expr + "]";
		
	}
}
