package decaf.dataflow.global;

import decaf.codegen.flatir.Name;

public class InductionVariable {
	private LoopQuadrupletStmt lqStmt;
	private Name familyName;
	private Name variable;

	public InductionVariable(LoopQuadrupletStmt lqStmt) {
		this.lqStmt = lqStmt;
		this.variable = lqStmt.getqStmt().getDestination();
	}
	
	public LoopQuadrupletStmt getLqStmt() {
		return lqStmt;
	}

	public void setLqStmt(LoopQuadrupletStmt lqStmt) {
		this.lqStmt = lqStmt;
	}
	
	public Name getFamilyName() {
		return familyName;
	}

	public void setFamilyName(Name familyName) {
		this.familyName = familyName;
	}

	public Name getVariable() {
		return variable;
	}

	public void setVariable(Name variable) {
		this.variable = variable;
	}
}
