package decaf.dataflow.block;

import java.util.HashMap;

import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBuilder;
import decaf.dataflow.cfg.MethodIR;

public class BlockOptimizer {
	private BlockCSEOptimizer cse;
	private BlockTempCPOptimizer copy;
	private BlockConsPropagationOptimizer cons;
	private BlockAlgebriacOptimizer alg;
	private BlockTempDCOptimizer dc;
	private BlockVarCPOptimizer copyVar;
	private BlockVarDCOptimizer dcVar;
	
	public BlockOptimizer(HashMap<String, MethodIR> mMap) {
		cse = new BlockCSEOptimizer(mMap);	
		
		copy = new BlockTempCPOptimizer(mMap);
		copyVar = new BlockVarCPOptimizer(mMap);
		
		cons = new BlockConsPropagationOptimizer(mMap);
		alg = new BlockAlgebriacOptimizer(mMap);
		
		dc = new BlockTempDCOptimizer(mMap);
		dcVar = new BlockVarDCOptimizer(mMap);
	}
	
	public void optimizeBlocks(boolean[] opts) {
		if(opts[1]) { // CSE
			cse.performCSE();
		}
		
		if(opts[3]) { // CONST
			// Do Const Propagation
			cons.performConsPropagation();
			
			// Do algebriac simplification
			alg.performAlgebriacSimplification();
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
