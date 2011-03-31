package decaf.dataflow.global;

import java.util.HashMap;
import java.util.List;

import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class GlobalCSEOptimizer {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private BlockAvailableExpressionGenerator availableGenerator;
	private ProgramFlattener pf;
	private int tempCount;
	
	public GlobalCSEOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.cfgMap = cfgMap;
		this.pf = pf;
		this.availableGenerator = new BlockAvailableExpressionGenerator(cfgMap);
		// Generate Available Expressions for CFG
		this.availableGenerator.generate();
	}
}
