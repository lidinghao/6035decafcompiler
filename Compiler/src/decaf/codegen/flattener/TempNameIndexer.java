package decaf.codegen.flattener;

import java.util.List;

import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.TempName;

public class TempNameIndexer {
	private int tempCount;
	
	public TempNameIndexer() {
		tempCount = 0;
	}
	
	public void indexTemps(List<LIRStatement> flatIR) {
		for (LIRStatement stmt: flatIR) {
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt quadruplet = (QuadrupletStmt) stmt;
				processQuadruplet(quadruplet);
			}
		}
	}
	
	private void processQuadruplet(QuadrupletStmt stmt) {
		if (isTempName(stmt.getArg1())) {
			tempCount--;
		}
		
		if (isTempName(stmt.getArg2())) {
			tempCount--;
		}
		
		if (isTempName(stmt.getDestination())) {
			TempName temp = (TempName) stmt.getDestination();
			if (temp.getId() != -1) { // Not already set
				temp.setId(tempCount);
				tempCount++;
			}
		}
	}
	
	private boolean isTempName(Name name) {
		if (name == null) {
			return false;
		}
		
		return (name.getClass().equals(Name.class));
	}
}
