package decaf.dataflow.global;

import java.util.HashMap;

import decaf.dataflow.cfg.MethodIR;

public class GlobalOptimizer {
	private GlobalCSEOptimizer cse;
	private GlobalConstantPropagationOptimizer constant;
	private GlobalCopyPropagationOptimizer copy;
	private GlobalDeadCodeOptimizer dc;
	private GlobalConstProp newConst;
	private HashMap<String, MethodIR> mMap;

	public GlobalOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
	}
	
	public void optimizeBlocks(boolean[] opts) {
		if(opts[1]) { // CSE
			cse = new GlobalCSEOptimizer(mMap);
			cse.performGlobalCSE();
		} 
		if(opts[2]) { // COPY
			copy = new GlobalCopyPropagationOptimizer(mMap);
			copy.performGlobalCopyProp();
		} 
		if(opts[3]) { // CONST
//			constant = new GlobalConstantPropagationOptimizer(mMap);
//			constant.performGlobalConstantProp();
			newConst = new GlobalConstProp(mMap);
			newConst.performConstProp();
		}
		if(opts[4]) { // DC
			dc = new GlobalDeadCodeOptimizer(mMap);
			dc.performDeadCodeElimination();
		}
	}
	
	public GlobalCSEOptimizer getCse() {
		return cse;
	}

	public void setCse(GlobalCSEOptimizer cse) {
		this.cse = cse;
	}
	
	public GlobalConstantPropagationOptimizer getConstant() {
		return constant;
	}

	public void setConstant(GlobalConstantPropagationOptimizer constant) {
		this.constant = constant;
	}

	public GlobalCopyPropagationOptimizer getCopy() {
		return copy;
	}

	public void setCopy(GlobalCopyPropagationOptimizer copy) {
		this.copy = copy;
	}

	public GlobalDeadCodeOptimizer getDc() {
		return dc;
	}

	public void setDc(GlobalDeadCodeOptimizer dc) {
		this.dc = dc;
	}
}

