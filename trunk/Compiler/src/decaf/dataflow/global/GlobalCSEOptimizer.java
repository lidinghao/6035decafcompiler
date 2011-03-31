package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class GlobalCSEOptimizer {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<CFGBlock, List<AvailableExpression>> blockExpressions;
	private HashMap<CFGBlock, BlockFlow> blockAvailableDefs;
	private HashMap<Integer, Integer> exprIDtoTempVal;
	private List<DynamicVarName> tempList;
	private BlockAvailableExpressionGenerator availableGenerator;
	private ProgramFlattener pf;
	private int sizeExprStmts; //equals to the number of bits in AvailableExpression
	private int tempCountOffset; //offset from base in the static temp count
	
	public GlobalCSEOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.cfgMap = cfgMap;
		this.pf = pf;
		this.availableGenerator = new BlockAvailableExpressionGenerator(cfgMap);
		// Generate Available Expressions for CFG
		this.availableGenerator.generate();
		this.blockExpressions = this.availableGenerator.getBlockExpressions();
		this.blockAvailableDefs = this.availableGenerator.getBlockReachingDefs();
		this.sizeExprStmts = this.availableGenerator.getTotalExpressionStmts();
		this.tempList = new ArrayList<DynamicVarName>();
	}
	
	//go first time over the CFG map and 
	private void generate(){
		//allocate temp values
		for(int i = 0; i < this.sizeExprStmts; i++){
			DynamicVarName temp = new DynamicVarName();
			//expToTemp.put(expr, temp);
			//newStmts.add(new QuadrupletStmt(QuadrupletOp.MOVE, temp, qStmt.getDestination(), null));
		}
	}
}
