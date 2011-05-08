package decaf.ralloc;

import java.util.HashMap;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class QuadrupletArgReorderer {
	private HashMap<String, MethodIR> mMap;
	
	public QuadrupletArgReorderer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
	}
	
	public void reorder() {
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
				for (LIRStatement stmt: block.getStatements()) {
					if (stmt.getClass().equals(QuadrupletStmt.class)) {
						QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
						if (qStmt.getArg1() != null && qStmt.getArg2() != null) {
							if (qStmt.getArg2().equals(qStmt.getDestination())) {
								Name arg1 = qStmt.getArg1();
								qStmt.setArg1(qStmt.getArg2());
								qStmt.setArg2(arg1);
							}
						}
					}
				}
			}
		}
	}
}
