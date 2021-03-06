package decaf.memory;

import java.util.HashMap;

import decaf.dataflow.cfg.MethodIR;

public class StoreStmtsAdder {
	private NaiveStoreAdder sa;
	private NaiveStoreOptimizer so;
	
	public StoreStmtsAdder(HashMap<String, MethodIR> mMap) {
		this.sa = new NaiveStoreAdder(mMap);
		this.so = new NaiveStoreOptimizer(mMap);
	}
	
	public void addStores() {
		this.sa.addStores();
		this.so.optimizeStores();
	}

	public NaiveStoreAdder getSa() {
		return sa;
	}

	public void setSa(NaiveStoreAdder sa) {
		this.sa = sa;
	}

	public NaiveStoreOptimizer getSo() {
		return so;
	}

	public void setSo(NaiveStoreOptimizer so) {
		this.so = so;
	}
}
