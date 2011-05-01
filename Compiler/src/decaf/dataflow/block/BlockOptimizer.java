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
	private HashMap<String, MethodIR> mMap;
	
	public BlockOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
	}
	
	public void optimizeBlocks(boolean[] opts) {
		if(opts[1]) { // CSE
			cse = new BlockCSEOptimizer(mMap);
			cse.performCSE();
		}
		
		if(opts[3]) { // CONST
			// Do Const Propagation
			cons = new BlockConsPropagationOptimizer(mMap);
			cons.performConsPropagation();
			
			// Do algebriac simplification
			alg = new BlockAlgebriacOptimizer(mMap);
			alg.performAlgebriacSimplification();
		} 
		
		if(opts[2]) { // COPY
			// CP DynamicVarNames
			copy = new BlockTempCPOptimizer(mMap);
			copy.performCopyPropagation();
			
			// CP VarNames and TempNames
			copyVar = new BlockVarCPOptimizer(mMap);
			copyVar.performCopyPropagation();
		} 
		
		if(opts[4]) { // DC
			// DC VarNames and TempNames
			dcVar = new BlockVarDCOptimizer(mMap);
			dcVar.performDeadCodeElimination();
			
			// DC DynamicVarName
			dc = new BlockTempDCOptimizer(mMap);
			dc.performDeadCodeElimination();
		}
	}
}
