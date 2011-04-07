package decaf.dataflow.block;

import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBuilder;

public class BlockOptimizer {
	private BlockCSEOptimizer cse;
	private BlockTempCPOptimizer copy;
	private BlockConsPropagationOptimizer cons;
	private BlockTempDCOptimizer dc;
	private BlockVarCPOptimizer copyVar;
	private BlockVarDCOptimizer dcVar;
	
	public BlockOptimizer(CFGBuilder cb, ProgramFlattener pf) {
		cse = new BlockCSEOptimizer(cb.getCfgMap(), pf);	
		
		copy = new BlockTempCPOptimizer(cb.getCfgMap(), pf);
		copyVar = new BlockVarCPOptimizer(cb.getCfgMap(), pf);
		
		cons = new BlockConsPropagationOptimizer(cb.getCfgMap(), pf);
		
		dc = new BlockTempDCOptimizer(cb.getCfgMap(), pf);
		dcVar = new BlockVarDCOptimizer(cb.getCfgMap(), pf);
	}
	
	public void optimizeBlocks(boolean[] opts) {
		if(opts[3]) { // CONST
			// Do Const Propagation
			cons.performConsPropagation();
			
			// Do algebriac simplification
		} 
		
		if(opts[1]) { // CSE
			cse.performCSE();
		}
		
		if(opts[2]) { // COPY
			// CP DynamicVarNames
			copy.performCopyPropagation();
			
			// CP VarNames and TempNames
			copyVar.performCopyPropagation();
		} 
		
		if(opts[4]) { // DC
			// DC VarNames and TempNames
			dcVar.performDeadCodeElimination();
			
			// DC DynamicVarName
			dc.performDeadCodeElimination();
		}
	}
}
