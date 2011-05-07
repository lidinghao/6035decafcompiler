package decaf.optimize;

import java.util.HashMap;

import decaf.dataflow.cfg.MethodIR;
import decaf.memory.LoadStmtsAdder;
import decaf.memory.StoreStmtsAdder;

public class ArrayAccessOptimizer {
	private HashMap<String, MethodIR> mMap;
	private BoundCheckDCOptimizer dc; // Single pass
	private BoundCheckCSEOptimizer cse; // Multipass
	
	public ArrayAccessOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.dc = new BoundCheckDCOptimizer(mMap);
		this.cse = new BoundCheckCSEOptimizer(mMap);
	}
	
	public void optimize(boolean[] opts) {
		addExplicitMemoryInstructions(); // Must add explicit load store first!
		
		if(opts[4]) { // DC
			dc.performBoundCheckDC(); // Must perform dc first
		}
		if(opts[1]) { // CSE
			cse.performBoundCheckCSE();
		} 
	}

	public void addExplicitMemoryInstructions() {
		// Add loads
		LoadStmtsAdder la = new LoadStmtsAdder(mMap);
		la.addLoads();
		
		// Add stores		
		StoreStmtsAdder sa = new StoreStmtsAdder(mMap);
		sa.addStores();
		
//		for (CFGBlock b: this.mMap.get("main").getCfgBlocks()) {
//			System.out.println("===================");
//			System.out.println(sa.getSo().getLGS().getUniqueGlobals().get("main") + "\n");
//			System.out.println(b);
//			System.out.println(sa.getSo().getLGS().getCfgBlocksState().get(b));
//		}
	}
}
