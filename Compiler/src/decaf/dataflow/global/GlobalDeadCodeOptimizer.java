package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import decaf.dataflow.cfg.CFGBlock;
import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;

public class GlobalDeadCodeOptimizer {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private BlockLivenessGenerator livenessGenerator;
	private ProgramFlattener pf;
	private HashMap<CFGBlock, BlockDataFlowState> blockLiveVars;
	private HashMap<Name, Variable> nameToVar;
	
	public GlobalDeadCodeOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.cfgMap = cfgMap;
		this.pf = pf;
		this.livenessGenerator = new BlockLivenessGenerator(cfgMap);
		this.livenessGenerator.generate();
		this.blockLiveVars = livenessGenerator.getBlockLiveVars();
		this.nameToVar = livenessGenerator.getNameToVar();
	}
	
	public void performDeadCodeElimination(){

		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
			}
			
			// Change statements
			List<LIRStatement> stmts = new ArrayList<LIRStatement>();
			
			for (int i = 0; i < this.cfgMap.get(s).size(); i++) {
				stmts.addAll(getBlockWithIndex(i, 
						this.cfgMap.get(s)).getStatements());
			}
			
			pf.getLirMap().put(s, stmts);
		}
	}
	
	private void optimize(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		BlockDataFlowState bFlow = blockLiveVars.get(block);
		Integer varId; 
		
		for (LIRStatement stmt: block.getStatements()){
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
				Name dest = qStmt.getDestination();
				if (nameToVar.containsKey(dest)) {
					varId = nameToVar.get(dest).getMyId();
					if (isDead(varId, bFlow.getOut())) {
						// Don't add statement
						continue;
					}
				}
			}
			newStmts.add(stmt);
		}
		block.setStatements(newStmts);
	}
	
	private boolean isDead(int varId, BitSet out){
		// Check if variable Id is true (live) in the outset
		// If it is not live, then it is redefined (or not used) after this block, 
		// so anything assigning the corresponding variable can be eliminated
		return !out.get(varId);
	}

	private CFGBlock getBlockWithIndex(int i, List<CFGBlock> list) {
		for (CFGBlock block: list) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		return null;
	}
}