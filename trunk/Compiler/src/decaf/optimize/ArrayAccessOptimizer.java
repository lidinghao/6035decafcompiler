package decaf.optimize;

import java.util.HashMap;

import decaf.dataflow.cfg.MethodIR;
import decaf.memory.LoadStmtsAdder;
import decaf.memory.NaiveStoreAdder;

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
		NaiveStoreAdder ns = new NaiveStoreAdder(mMap);
		ns.addStores();
	}
}
