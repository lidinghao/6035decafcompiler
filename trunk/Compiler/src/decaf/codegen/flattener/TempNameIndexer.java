package decaf.codegen.flattener;

import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.TempName;

public class TempNameIndexer {
	private int tempCount;
	private int tempNeeded;

	public TempNameIndexer() {
		reset();
	}

	public int indexTemps(List<LIRStatement> flatIR) throws Exception {
		reset();

		for (LIRStatement stmt : flatIR) {
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt quadruplet = (QuadrupletStmt) stmt;
				processQuadruplet(quadruplet);
			}
		}

		return tempNeeded;
	}

	private void reset() {
		tempCount = 0;
		tempNeeded = 0;
	}

	private void processQuadruplet(QuadrupletStmt stmt) throws Exception {
		if (tempCount < 0) {
			throw new Exception("Temp index cannot be negative");
		}
		
		if (isTempName(stmt.getArg1())) {
			tempCount--;
		}

		if (isTempName(stmt.getArg2())) {
			tempCount--;
		}

		if (isTempName(stmt.getDestination())) {
			TempName temp = (TempName) stmt.getDestination();
			if (temp.getId() == -1) { // Not already set
				temp.setId(tempCount);
				tempCount++;
			}
		}

		if (tempCount > tempNeeded) {
			tempNeeded = tempCount;
		}
	}

	private boolean isTempName(Name name) {
		if (name == null) {
			return false;
		}

		return name.getClass().equals(TempName.class);
	}

	public int getTempVarsNeeded() {
		return tempNeeded;
	}
}
