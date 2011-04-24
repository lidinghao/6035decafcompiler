package decaf.dataflow.global;

import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBuilder;

public class GlobalOptimizer {
	private GlobalCSEOptimizer cse;
	private GlobalConstantPropagationOptimizer constant;
	private GlobalCopyPropagationOptimizer copy;
	private GlobalDeadCodeOptimizer dc;
	private CFGBuilder cb;
	private ProgramFlattener pf;

	public GlobalOptimizer(CFGBuilder cb, ProgramFlattener pf) {
		this.cb = cb;
		this.pf = pf;
	}
	
	public void optimizeBlocks(boolean[] opts) {
		if(opts[1]) { // CSE
			cse = new GlobalCSEOptimizer(cb.getCfgMap(), pf);
			cse.performGlobalCSE();
		} 
		if(opts[2]) { // COPY
			copy = new GlobalCopyPropagationOptimizer(cb.getCfgMap());
			copy.performGlobalCopyProp();
		} 
		if(opts[3]) { // CONST
			constant = new GlobalConstantPropagationOptimizer(cb.getCfgMap());
			constant.performGlobalConstantProp();
		} 
		if(opts[4]) { // DC
			dc = new GlobalDeadCodeOptimizer(cb.getCfgMap(), pf);
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

