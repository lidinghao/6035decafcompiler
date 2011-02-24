package ir.ast;

public class ForStmt extends Statement {
	private final String id;
	private final Expression initialVal;
	private final Expression finalVal;
	private final Block block;
	
	public ForStmt(String i, Expression init, Expression fin, Block bl) {
		this.id = i;
		this.initialVal = init;
		this.finalVal = fin;
		this.block = bl;
	}
	
	public String getId() {
		return this.id;
	}
	
	public Expression getInitialVal() {
		return this.initialVal;
	}
	
	public Expression getFinalVal() {
		return this.finalVal;
	}
	
	public Block getBlock() {
		return this.block;
	}
}
