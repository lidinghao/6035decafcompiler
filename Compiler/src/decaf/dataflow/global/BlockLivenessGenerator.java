package decaf.dataflow.global;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.Name;
import decaf.dataflow.cfg.CFGBlock;

public class BlockLivenessGenerator {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockLiveVars;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<Name, HashSet<Integer>> nameToStmtIds;
	private int totalExpressionStmts;
	
	public BlockLivenessGenerator(HashMap<String, List<CFGBlock>> cMap) {
		this.cfgMap = cMap;
		setNameToStmtIds(new HashMap<Name, HashSet<Integer>>());
		setBlockLiveVars(new HashMap<CFGBlock, BlockDataFlowState>());
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		totalExpressionStmts = 0;
	}
	
	private void generate() {
		
	}
	
	private void initialize() {
		
	}

	
	
	public void setNameToStmtIds(HashMap<Name, HashSet<Integer>> nameToStmtIds) {
		this.nameToStmtIds = nameToStmtIds;
	}

	public HashMap<Name, HashSet<Integer>> getNameToStmtIds() {
		return nameToStmtIds;
	}

	public void setBlockLiveVars(HashMap<CFGBlock, BlockDataFlowState> blockLiveVars) {
		this.blockLiveVars = blockLiveVars;
	}

	public HashMap<CFGBlock, BlockDataFlowState> getBlockLiveVars() {
		return blockLiveVars;
	}
	
	

}
