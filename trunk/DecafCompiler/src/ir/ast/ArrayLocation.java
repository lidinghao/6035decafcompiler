package ir.ast;

public class ArrayLocation extends Location {
	private Expression expr;
	
	public ArrayLocation() {
		
	}
	
	public ArrayLocation(String id, Expression expr) {
		this.id = id;
		this.expr = expr;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void setExpr(Expression expr) {
		this.expr = expr;
	}
	
	public Expression getExpr() {
		return expr;
	}
}
