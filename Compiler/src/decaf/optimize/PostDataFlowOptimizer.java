package decaf.optimize;

import java.util.HashMap;
import java6035.tools.CLI.CLI;

import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.CFGBuilder;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.LoopInvariantOptimizer;
import decaf.dataflow.global.LoopStrengthReductionOptimizer;

public class PostDataFlowOptimizer {
	private ArrayAccessOptimizer arrOpt;
	private StaticJumpEvaluator sje;
	private HashMap<String, MethodIR> mMap;
	private ProgramFlattener pf;
	private CFGBuilder cb;
	private LoopInvariantOptimizer loopInvariants;
	private LoopStrengthReductionOptimizer loopStrRed;
	private HashMap<CFGBlock, String> blockState;
	
	public PostDataFlowOptimizer(ProgramFlattener pf, CFGBuilder cb) {
		this.pf = pf;
		this.cb = cb;
		this.blockState = new HashMap<CFGBlock, String>();
		
		init();
	}
	
	private void init() {
		this.cb.setMergeBoundChecks(true);
		this.cb.generateCFGs();
		this.mMap = MethodIR.generateMethodIRs(pf, cb);
	}
	
	public void optimize() {
		int i = 0;
		while (i < 25) {
			updateBlockState();			
//			System.out.println("BEFORE LOOP STRENGTH REDUCTION");
//			this.cb.printCFG(System.out);
//			
//			this.loopStrRed = new LoopStrengthReductionOptimizer(this.mMap);
//			loopStrRed.performStrengthReductionOptimization();
//			cb.generateCFGs();
			
//			System.out.println("BEFORE LOOP INVARIANTS");
			this.cb.printCFG(System.out);
			
			this.loopInvariants = new LoopInvariantOptimizer(this.mMap);
			loopInvariants.performLoopInvariantOptimization();
			cb.generateCFGs();
			
//			System.out.println("AFTER LOOP INVARIANTS");
			this.cb.printCFG(System.out);
			
			if (!isChanged()) {
				break;
			}
				
			i++;
		}
		
		this.arrOpt = new ArrayAccessOptimizer(this.mMap);
		arrOpt.optimize(CLI.opts);
		
//		System.out.println("BEFORE SJE");
		this.cb.printCFG(System.out);

		this.sje = new StaticJumpEvaluator(pf, cb);
		sje.staticEvaluateJumps();
		this.mMap = MethodIR.generateMethodIRs(pf, cb);
	}

	private boolean isChanged() {
		for (String methodName: this.cb.getCfgMap().keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cb.getCfgMap().get(methodName)) {
				if (!block.toString().equals(this.blockState.get(block))) {
//					System.out.println("=========================");
//					System.out.println("CHECKING: " + block.getMethodName() + ":" + block.getIndex() + "; " + block.hashCode());
//					System.out.println("IS IN MAP: " + this.blockState.containsKey(block));
//					for (CFGBlock b: this.blockState.keySet()) {
//						System.out.println("CONTAINS: " + b.getMethodName() + ":" + b.getIndex() + "; " + b.hashCode());
//					}
//					System.out.println(block.toString());
//					System.out.println("************************");
//					System.out.println(this.blockState.get(block));
					return true;
				}
			}
		}
		
		return false;
	}

	private void updateBlockState() {
		for (String methodName: this.cb.getCfgMap().keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cb.getCfgMap().get(methodName)) {
//				System.out.println("ADDING: " + block.getMethodName() + ":" + block.getIndex());
				this.blockState.put(block, block.toString());
//				System.out.println("ADDING CHECK: " + this.blockState.containsKey(block) + "; " + block.hashCode());
			}
		}
	}
}
