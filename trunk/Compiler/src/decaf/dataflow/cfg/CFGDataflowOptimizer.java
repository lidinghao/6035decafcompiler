package decaf.dataflow.cfg;

import java.util.HashMap;

import decaf.dataflow.block.BlockOptimizer;
import decaf.dataflow.global.GlobalOptimizer;

public class CFGDataflowOptimizer {
	private HashMap<String, MethodIR> mMap;
	private HashMap<CFGBlock, String> blockState;
	private BlockOptimizer bo;
	private GlobalOptimizer go;
	private boolean[] opts;
	
	public CFGDataflowOptimizer(HashMap<String, MethodIR> mMap, 
			BlockOptimizer bo, GlobalOptimizer go, boolean[] opts) {
		this.mMap = mMap;
		this.bo = bo;
		this.go = go;
		this.blockState = new HashMap<CFGBlock, String>();
		this.opts = opts;
	}
	
	public void optimizeCFGDataflow() {
		while (true) {
			updateBlockState();
			
			bo.optimizeBlocks(opts);
			go.optimizeBlocks(opts);

			if (!isChanged()) {
				break;
			}
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
