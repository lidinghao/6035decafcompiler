package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockDeadCodeOptimizer {
	private HashSet<Name> neededSet;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private ProgramFlattener pf;
	
	public BlockDeadCodeOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.neededSet = new HashSet<Name>();
		this.cfgMap = cfgMap;
		this.pf = pf;
	}

	public void performDeadCode() {
		for (String s: this.cfgMap.keySet()) {
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
				reset();
			}

			// Update statements in program flattener
			List<LIRStatement> stmts = new ArrayList<LIRStatement>();
			
			for (int i = 0; i < this.cfgMap.get(s).size(); i++) {
				stmts.addAll(getBlockWithIndex(i, this.cfgMap.get(s)).getStatements());
			}
			
			pf.getLirMap().put(s, stmts);
		}
	}
	
	public CFGBlock getBlockWithIndex(int i, List<CFGBlock> list) {
		for (CFGBlock block: list) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		
		return null;
	}
	
	public void optimize(CFGBlock block) {
		
	}
	
	public void reset() {
		this.neededSet.clear();
	}
}

