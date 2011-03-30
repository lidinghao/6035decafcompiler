package decaf.dataflow.block;

import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBuilder;

public class BlockOptimizer {
	private BlockCSEOptimizer cse;
	private BlockCopyPropagationOptimizer copy;
	private BlockConsPropagationOptimizer cons;
	private BlockDeadCodeOptimizer dc;
	
	public BlockOptimizer(CFGBuilder cb, ProgramFlattener pf) {
		cse = new BlockCSEOptimizer(cb.getCfgMap(), pf);		
		copy = new BlockCopyPropagationOptimizer(cb.getCfgMap(), pf);
		cons = new BlockConsPropagationOptimizer(cb.getCfgMap(), pf);
		dc = new BlockDeadCodeOptimizer(cb.getCfgMap(), pf);
	}
	
	public void optimizeBlocks() {
		cse.performCSE();
		copy.performCopyPropagation();
		cons.performConsPropagation();
		dc.performDeadCodeElimination();
	}
}
