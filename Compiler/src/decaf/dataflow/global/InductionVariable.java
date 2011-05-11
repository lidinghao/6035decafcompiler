package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;

public class InductionVariable {
	private static int StatementId = 0;
	private LoopQuadrupletStmt lqStmt;
	private int blockId;
	// (familyName, adder, multiplier) make up the triple which
	// defines the InductionVariable
	private Name familyName;
	private Name adder;
	private Name multiplier;
	// QuadrupletStmt which calculates the adder and multipler Name of
	// derived InductionVariable - these statements must be inserted into
	// the loop body before the InductionVariable update statements
	private QuadrupletStmt forAdder;
	private QuadrupletStmt forMultiplier;
	private Name variable;
	private Name variablePrime;
	private boolean isDerived;
	// Null for basic induction variables
	private InductionVariable derivedFrom;

	public InductionVariable(LoopQuadrupletStmt lqStmt) {
		this.lqStmt = lqStmt;
		this.variable = lqStmt.getqStmt().getDestination();
		this.variablePrime = new VarName("$srprime" + Integer.toString(StatementId));
		StatementId++;
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
	
	public boolean isDerived() {
		return isDerived;
	}

	public void setDerived(boolean isDerived) {
		this.isDerived = isDerived;
	}

	public InductionVariable getDerivedFrom() {
		return derivedFrom;
	}

	public void setDerivedFrom(InductionVariable derivedFrom) {
		this.derivedFrom = derivedFrom;
	}
	
	public void setAdderByAdd(Name a, QuadrupletOp qOp) {
		this.adder = new VarName("$srtemp" + Integer.toString(StatementId));
		((VarName)this.adder).setBlockId(blockId);
		this.forAdder = new QuadrupletStmt(qOp, this.adder, a, this.derivedFrom.adder);
		StatementId++;
	}
	
	public void setAdderByMult(Name m) {
		this.adder = new VarName("$srtemp" + Integer.toString(StatementId));
		((VarName)this.adder).setBlockId(blockId);
		this.forAdder = new QuadrupletStmt(QuadrupletOp.MUL, this.adder, m, this.derivedFrom.adder);
		StatementId++;
	}
	
	public void setMultiplierByMult(Name m) {
		this.multiplier = new VarName("$srtemp" + Integer.toString(StatementId));
		((VarName)this.multiplier).setBlockId(blockId);
		this.forMultiplier = new QuadrupletStmt(QuadrupletOp.MUL, this.adder, m, this.derivedFrom.multiplier);
		StatementId++;
	}
	
	// i <- i + c where i is a base induction variable
	// If j has triple (i,a,b), then j <- j + c*b should be generated
	// This method should only be used for derived induction variables
	public List<QuadrupletStmt> getInductionStmts(Name c) {
		VarName temp = new VarName("$srtemp" + Integer.toString(StatementId));
		StatementId++;
		temp.setBlockId(blockId);
		List<QuadrupletStmt> stmts = new ArrayList<QuadrupletStmt>();
		stmts.add(new QuadrupletStmt(QuadrupletOp.MUL, temp, c, this.multiplier));
		stmts.add(new QuadrupletStmt(QuadrupletOp.ADD, this.variablePrime, this.variablePrime, temp));
		return stmts;
	}
	
	// If j has triple (i, a, b), the loop preheader should compute j <- a + i*b
	// This method should only be used for derived induction variables
	public List<QuadrupletStmt> getLoopPreheaderStmts() {
		VarName temp = new VarName("$srtemp" + Integer.toString(StatementId));
		StatementId++;
		temp.setBlockId(blockId);
		List<QuadrupletStmt> stmts = new ArrayList<QuadrupletStmt>();
		stmts.add(new QuadrupletStmt(QuadrupletOp.MUL, temp, this.familyName, this.multiplier));
		stmts.add(new QuadrupletStmt(QuadrupletOp.ADD, this.variablePrime, this.adder, temp));
		return stmts;
	}
	
	// Returns the assignment of j <- j'
	// This method should only be used for derived induction variables
	public QuadrupletStmt getInductionAssignmentStmt() {
		return new QuadrupletStmt(null, this.variable, this.variablePrime, null);
	}
	
	public Name getAdder() {
		return adder;
	}

	public void setAdder(Name adder) {
		this.adder = adder;
	}

	public Name getMultiplier() {
		return multiplier;
	}

	public void setMultiplier(Name multiplier) {
		this.multiplier = multiplier;
	}
	
	public int getBlockId() {
		return blockId;
	}

	public void setBlockId(int blockId) {
		this.blockId = blockId;
	}
	
	public QuadrupletStmt getForAdder() {
		return forAdder;
	}

	public void setForAdder(QuadrupletStmt forAdder) {
		this.forAdder = forAdder;
	}

	public QuadrupletStmt getForMultiplier() {
		return forMultiplier;
	}

	public void setForMultiplier(QuadrupletStmt forMultiplier) {
		this.forMultiplier = forMultiplier;
	}

	public Name getVariablePrime() {
		return variablePrime;
	}

	public void setVariablePrime(Name variablePrime) {
		this.variablePrime = variablePrime;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return lqStmt.toString() + ", derived from: [" + derivedFrom.toString() + 
		"], family name: " + familyName.toString();
	}
}
