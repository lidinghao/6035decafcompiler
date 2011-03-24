package decaf.dataflow.cfg;

import java.util.List;

import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;

public class LabelBlockTableGenerator {
	private LabelBlockTable labelBlockTable;
	
	public LabelBlockTableGenerator() {
		labelBlockTable = new LabelBlockTable();
	}
	
	public LabelBlockTable getLabelBlockTable() {
		return labelBlockTable;
	}

	public void setLabelBlockTable(LabelBlockTable labelBlockTable) {
		this.labelBlockTable = labelBlockTable;
	}
	
	public void processStatementList(List<LIRStatement> stmts) {
		CFGBlock curBlock = null;
		for (LIRStatement stmt: stmts) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				// End the previous CFGBlock if there is one
				CFGBlock newBlock = new CFGBlock((LabelStmt)stmt);
				if (curBlock != null) {
					curBlock.setNext(newBlock);
					// Add block to table
					labelBlockTable.put(curBlock.getLabel().getLabel(), curBlock);
				}
				curBlock = newBlock;
			}
			else if (stmt.getClass().equals(JumpStmt.class)) {
				curBlock.setJump((JumpStmt)stmt);
			}
			else {
				curBlock.addStatement(stmt);
			}
		}
	}
	
	@Override
	public String toString() {
		String str = this.labelBlockTable.get("main").toString();
		return str;
	}
}

