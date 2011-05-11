package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;

public class InductionVariable {
	private static int StatementId = 0;
	private LoopQuadrupletStmt lqStmt;
	private List<LIRStatement> boundCheckStmtForDest;
	private List<LIRStatement> boundCheckStmtForAdder;
	private List<LIRStatement> boundCheckStmtForMultiplier;
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
	
	public void setAdderByAdd(Name a, QuadrupletOp qOp, List<LIRStatement> boundChecks) {
		this.adder = new VarName("$srtemp" + Integer.toString(StatementId));
		((VarName)this.adder).setBlockId(blockId);
		this.boundCheckStmtForAdder = boundChecks;
		this.forAdder = new QuadrupletStmt(qOp, this.adder, a, this.derivedFrom.adder);
		StatementId++;
	}
	
	public void setAdderByMult(Name m, List<LIRStatement> boundChecks) {
		this.adder = new VarName("$srtemp" + Integer.toString(StatementId));
		((VarName)this.adder).setBlockId(blockId);
		this.boundCheckStmtForAdder = boundChecks;
		this.forAdder = new QuadrupletStmt(QuadrupletOp.MUL, this.adder, m, this.derivedFrom.adder);
		StatementId++;
	}
	
	public void setMultiplierByMult(Name m, List<LIRStatement> boundChecks) {
		this.multiplier = new VarName("$srtemp" + Integer.toString(StatementId));
		((VarName)this.multiplier).setBlockId(blockId);
		this.boundCheckStmtForMultiplier = boundChecks;
		this.forMultiplier = new QuadrupletStmt(QuadrupletOp.MUL, this.multiplier, m, this.derivedFrom.multiplier);
		StatementId++;
	}
	
	// i <- i + c where i is a base induction variable
	// If j has triple (i,a,b), then j <- j + c*b should be generated
	// This method should only be used for derived induction variables
	public List<LIRStatement> getInductionStmts() {
		VarName temp = new VarName("$srtemp" + Integer.toString(StatementId));
		StatementId++;
		temp.setBlockId(blockId);
		List<LIRStatement> stmts = new ArrayList<LIRStatement>();
		InductionVariable familyVar = getFamilyInductionVariable();
		stmts.add(new QuadrupletStmt(QuadrupletOp.MUL, temp, familyVar.getAdder(), this.multiplier));
		stmts.add(new QuadrupletStmt(QuadrupletOp.ADD, this.variablePrime, this.variablePrime, temp));
		return stmts;
	}
	
	// If j has triple (i, a, b), the loop preheader should compute j <- a + i*b
	// This method should only be used for derived induction variables
	public List<LIRStatement> getLoopPreheaderStmts() {
		VarName temp = new VarName("$srtemp" + Integer.toString(StatementId));
		StatementId++;
		temp.setBlockId(blockId);
		List<LIRStatement> stmts = new ArrayList<LIRStatement>();
		stmts.add(new QuadrupletStmt(QuadrupletOp.MUL, temp, this.familyName, this.multiplier));
		stmts.add(new QuadrupletStmt(QuadrupletOp.ADD, this.variablePrime, this.adder, temp));
		return stmts;
	}
	
	// Returns the assignment of j <- j'
	// This method should only be used for derived induction variables
	public List<LIRStatement> getInductionAssignmentStmt() {
		List<LIRStatement> stmts = new ArrayList<LIRStatement>();
		if (boundCheckStmtForDest != null) {
			stmts.addAll(boundCheckStmtForDest);
		}
		stmts.add(getInductionAssignmentStmtWithoutBound());
		return stmts;
	}
	
	public LIRStatement getInductionAssignmentStmtWithoutBound() {
		return new QuadrupletStmt(QuadrupletOp.MOVE, this.variable, this.variablePrime, null);
	}
	
	private InductionVariable getFamilyInductionVariable() {
		if (!isDerived) {
			return this;
		}
		return derivedFrom.getFamilyInductionVariable();
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
		((VarName)this.variablePrime).setBlockId(blockId);
	}
	
	public List<LIRStatement> getForAdder() {
		List<LIRStatement> stmts = new ArrayList<LIRStatement>();
		if (boundCheckStmtForAdder != null) {
			stmts.addAll(boundCheckStmtForAdder);
		}
		stmts.add(forAdder);
		return stmts;
	}

	public void setForAdder(QuadrupletStmt forAdder) {
		this.forAdder = forAdder;
	}

	public List<LIRStatement> getForMultiplier() {
		List<LIRStatement> stmts = new ArrayList<LIRStatement>();
		if (boundCheckStmtForMultiplier != null) {
			stmts.addAll(boundCheckStmtForMultiplier);
		}
		stmts.add(forMultiplier);
		return stmts;
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

	public List<LIRStatement> getBoundCheckStmtForDest() {
		return boundCheckStmtForDest;
	}

	public void setBoundCheckStmtForDest(List<LIRStatement> boundCheckStmtForDest) {
		this.boundCheckStmtForDest = boundCheckStmtForDest;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return lqStmt.toString() + ", derived from: [" + derivedFrom + 
		"], family name: " + familyName.toString();
	}
}
