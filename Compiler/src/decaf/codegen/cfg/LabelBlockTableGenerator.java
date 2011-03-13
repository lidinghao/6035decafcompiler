package decaf.codegen.cfg;

import java.util.List;

import decaf.codegen.flatir.LIRStatement;

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
		
	}
}

