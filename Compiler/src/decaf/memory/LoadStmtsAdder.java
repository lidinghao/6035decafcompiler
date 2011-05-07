package decaf.memory;

import java.util.HashMap;

import decaf.dataflow.cfg.MethodIR;

public class LoadStmtsAdder {
	private NaiveLoadAdder la;
	private NaiveLoadOptimizer lo;
	
	public LoadStmtsAdder(HashMap<String, MethodIR> mMap) {
		this.la = new NaiveLoadAdder(mMap);
		this.lo = new NaiveLoadOptimizer(mMap);
	}
	
	public void addLoads() {
		this.la.addLoads();
		this.lo.optimizeLoads();
	}
}
