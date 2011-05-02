package decaf.dataflow.cfg;

import java.util.HashMap;

import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.block.BlockOptimizer;
import decaf.dataflow.global.GlobalOptimizer;

public class CFGDataflowOptimizer {
	private HashMap<String, MethodIR> mMap;
	private HashMap<CFGBlock, String> blockState;
	private BlockOptimizer bo;
	private ProgramFlattener pf;
	private GlobalOptimizer go;
	private boolean[] opts;
	
	public CFGDataflowOptimizer(HashMap<String, MethodIR> mMap, 
			ProgramFlattener pf, BlockOptimizer bo, GlobalOptimizer go, boolean[] opts) {
		this.mMap = mMap;
		this.bo = bo;
		this.go = go;
		this.pf = pf;
		this.blockState = new HashMap<CFGBlock, String>();
		this.opts = opts;
	}
	
	public void optimizeCFGDataflow() {
		int i = 0;
		while (i < 100) {
			updateBlockState();
			
			bo.optimizeBlocks(opts);
			go.optimizeBlocks(opts);
			
			System.out.println("PASS " + i);
			pf.printLIR(System.out);
			
			if (!isChanged()) {
				break;
			}
			i++;
		}
	}
	
	private void updateBlockState() {
		for (String s : this.mMap.keySet()) {
			for (CFGBlock block : this.mMap.get(s).getCfgBlocks()) {
				this.blockState.put(block, block.toString());
			}
		}
	}
	
	private boolean isChanged() {
		for (String s : this.mMap.keySet()) {
			for (CFGBlock block : this.mMap.get(s).getCfgBlocks()) {
				if (!block.toString().equals(this.blockState.get(block).toString())) {
					return true;
				}
			}
		}
		return false;
	}
}
