package decaf.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class LoadsDC {
	private ReachingLoads rl;
	private HashMap<String, MethodIR> mMap;
	
	public LoadsDC(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		rl = new ReachingLoads(mMap);
	}
	
	public void removeDeadLoads() {
		rl.analyze();
		
		for (String methodName: this.mMap.keySet()) {
			for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
				removeDeadLoads(block);
			}
			
			this.mMap.get(methodName).regenerateStmts();
		}
	}

	private void removeDeadLoads(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(LoadStmt.class)) {
				if (!stmt.isDead()) {
					newStmts.add(stmt);
				}
			}
			else {
				newStmts.add(stmt);
			}
		}
		
		block.setStatements(newStmts);
	}
}
