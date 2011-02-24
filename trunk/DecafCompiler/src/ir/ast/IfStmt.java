package ir.ast;

public class IfStmt extends Statement {
	private final Expression condition;
	private final Block ifBlock;
	private final Block elseBlock;
	
	public IfStmt(Expression cond, Block ifBl) {
		this.condition = cond;
		this.ifBlock = ifBl;
		this.elseBlock = null;
	}
	
	public IfStmt(Expression cond, Block ifBl, Block elseBl) {
		this.condition = cond;
		this.ifBlock = ifBl;
		this.elseBlock = elseBl;
	}
	
	public Expression getCondition() {
		return this.condition;
	}
	
	public Block getIfBlock() {
		return this.ifBlock;
	}
	
	public Block getElseBlock() {
		return this.elseBlock;
	}
 	
	
	

}
