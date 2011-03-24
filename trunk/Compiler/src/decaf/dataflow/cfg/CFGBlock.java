package decaf.dataflow.cfg;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;

public class CFGBlock {
	private LabelStmt label;
	private List<LIRStatement> statements;
	private JumpStmt jump;
	private CFGBlock next;
	
	public CFGBlock(LabelStmt label) {
		setLabel(label);
		statements = new ArrayList<LIRStatement>();
	}
	
	public LabelStmt getLabel() {
		return label;
	}
	public void setLabel(LabelStmt label) {
		this.label = label;
	}
	public List<LIRStatement> getStatements() {
		return statements;
	}
	public void setStatements(List<LIRStatement> statements) {
		this.statements = statements;
	}
	
	public void addStatement(LIRStatement statement) {
		this.statements.add(statement);
	}
	
	public JumpStmt getJump() {
		return jump;
	}
	public void setJump(JumpStmt jump) {
		this.jump = jump;
	}
	public CFGBlock getNext() {
		return next;
	}
	public void setNext(CFGBlock next) {
		this.next = next;
	}
	
	@Override
	public String toString() {
		String str = "";
		str.concat(this.getLabel().toString());
		if (this.getJump() != null) {
			str.concat("-" + this.getJump().toString() + "\n");
		} else {
			str.concat("\n");
		}
		str.concat("  |\n" );
		str.concat(this.getNext().toString() + "\n");
		return str;
	}
}
