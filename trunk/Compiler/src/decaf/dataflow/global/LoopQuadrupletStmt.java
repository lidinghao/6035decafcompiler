package decaf.dataflow.global;

import decaf.codegen.flatir.QuadrupletStmt;

public class LoopQuadrupletStmt {
	private QuadrupletStmt qStmt;
	private String loopBodyBlockId;
	private int stmtIndex;
	private boolean needConditionalCheck;
	
	public LoopQuadrupletStmt(QuadrupletStmt q, String loopId, int stmtIndex) {
		this.qStmt = q;
		this.loopBodyBlockId = loopId;
		this.stmtIndex = stmtIndex;
		this.needConditionalCheck = false;
	}
	
	public QuadrupletStmt getqStmt() {
		return qStmt;
	}

	public void setqStmt(QuadrupletStmt qStmt) {
		this.qStmt = qStmt;
	}
	
	public String getLoopBodyBlockId() {
		return loopBodyBlockId;
	}

	public void setLoopBodyBlockId(String loopBodyId) {
		this.loopBodyBlockId = loopBodyId;
	}
	
	public int getStmtIndex() {
		return stmtIndex;
	}

	public void setStmtIndex(int stmtIndex) {
		this.stmtIndex = stmtIndex;
	}

	public boolean isNeedConditionalCheck() {
		return needConditionalCheck;
	}

	public void setNeedConditionalCheck(boolean needConditionalCheck) {
		this.needConditionalCheck = needConditionalCheck;
	}
	
	public boolean getNeedConditionalCheck() {
		return this.needConditionalCheck;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return loopBodyBlockId + " " + qStmt.toString();
	}
}
