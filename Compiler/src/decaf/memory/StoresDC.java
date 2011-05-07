package decaf.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.StoreStmt;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class StoresDC {
	private ReachingStores rs;
	private HashMap<String, MethodIR> mMap;
	
	public StoresDC(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		rs = new ReachingStores(mMap);
	}
	
	public void removeDeadStores() {
		rs.analyze();
		
		for (String methodName: this.mMap.keySet()) {
			for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
				removeDeadStores(block);
			}
			
			this.mMap.get(methodName).regenerateStmts();
		}
	}

	private void removeDeadStores(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(StoreStmt.class)) {
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
