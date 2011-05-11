package decaf.dataflow.cfg;

import java.util.HashMap;

import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.block.BlockOptimizer;
import decaf.dataflow.global.GlobalOptimizer;

public class CFGDataflowOptimizer {
	private HashMap<String, MethodIR> mMap;
	private HashMap<CFGBlock, String> blockState;
	private HashMap<CFGBlock, String> globalBlockState;
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
			updateLocalBlockState();
			updateGlobalBlockState();
			
			int j = 0;
			while (j < 100) {
				updateLocalBlockState();
				bo.optimizeBlocks(opts);
				if (!isLocalChanged()) {
					break;
				}
				j++;
			}
			
			int k = 0;
			while (k < 100) {
				updateLocalBlockState();
				go.optimizeBlocks(opts);
				if (!isLocalChanged()) {
					break;
				}
				k++;
			}
			
			
			System.out.println("GLOBAL PASS " + i);
			pf.printLIR(System.out);
			
			if (!isGlobalChanged()) {
				break;
			}
			i++;
		}
	}
	
	private void updateLocalBlockState() {
		for (String s : this.mMap.keySet()) {
			for (CFGBlock block : this.mMap.get(s).getCfgBlocks()) {
				this.globalBlockState.put(block, block.toString());
			}
		}
	}
	
	private void updateGlobalBlockState() {
		for (String s : this.mMap.keySet()) {
			for (CFGBlock block : this.mMap.get(s).getCfgBlocks()) {
				this.blockState.put(block, block.toString());
			}
		}
	}
	
	private boolean isLocalChanged() {
		for (String s : this.mMap.keySet()) {
			for (CFGBlock block : this.mMap.get(s).getCfgBlocks()) {
				if (!block.toString().equals(this.blockState.get(block).toString())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isGlobalChanged() {
		for (String s : this.mMap.keySet()) {
			for (CFGBlock block : this.mMap.get(s).getCfgBlocks()) {
				if (!block.toString().equals(this.globalBlockState.get(block).toString())) {
					return true;
				}
			}
		}
		return false;
	}
}
