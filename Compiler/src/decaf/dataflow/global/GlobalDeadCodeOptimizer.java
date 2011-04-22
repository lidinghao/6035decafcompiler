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
	private HashSet<CFGBlock> cfgBlocksToProcess;
	//private HashMap<Name, HashSet<Integer>> nameToVarIds;
	private HashMap<Name, Variable> nameToVar;
	private int totalExpressionStmts;
	
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
		Integer stmtID; // will be an integer - get value from here ;)
		
		for (LIRStatement stmt: block.getStatements()){
			if (!stmt.isExpressionStatement()) {
				newStmts.add(stmt);
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
			Name dest = qStmt.getDestination();
			
			if(this.nameToVar.containsKey(dest)){
				stmtID =  this.nameToVar.get(dest).getMyId();//null;//TODO:this.nameToStmtIds.get(dest);
				if(isDead(stmtID, bFlow.getIn(), bFlow.getOut())){
					continue;
				}
				//newStmts.add(stmt);
			}
			newStmts.add(stmt);
		}
		block.setStatements(newStmts);
	}
	
	private boolean isDead(Integer stmtID, BitSet inBits, BitSet outBits){
		if(!(inBits.get(stmtID) || outBits.get(stmtID))){
			return true;
		}
		return false;
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