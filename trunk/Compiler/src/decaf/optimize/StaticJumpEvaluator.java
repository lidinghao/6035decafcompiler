package decaf.optimize;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.CFGBuilder;

/**
 * MUST REBUILD CFG
 * @author usmanm
 *
 */
public class StaticJumpEvaluator {
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
	private static String ArrayFailLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.fail";
	private static String IfTestLabelRegex = "[a-zA-z_]\\w*.if\\d+.test";
	private static String IfEndLabelRegex = "[a-zA-z_]\\w*.if\\d+.end";
	
	private ProgramFlattener pf;
	private CFGBuilder cb;
	private List<CFGBlock> deadBlocks;
	
	public StaticJumpEvaluator(ProgramFlattener pf, CFGBuilder cb) {
		this.pf = pf;
		this.cb = cb;
		this.deadBlocks = new ArrayList<CFGBlock>();
	}
	
	public void staticEvaluateJumps() {
		for (String methodName: this.pf.getLirMap().keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			optimize(methodName);
		}
		
		dcBlocksAndFixLIR();
	}

	private void dcBlocksAndFixLIR() {
		cb.setMergeBoundChecks(true);
		cb.generateCFGs();
		
		for (String methodName: this.pf.getLirMap().keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;

			this.deadBlocks.clear();
			
			// Gen dead block
			getDeadBlocks(methodName);
			
			// Remove from LIR lists	
			removeDeadStmtsFromLIR(methodName);
			
			// Peephole to remove jumps
			removeRedundantJumps(methodName);
			
			// Remove array check left overs
			removeArrayCheckLeftOvers(methodName);
			
			// Remove if condition left overs
			removeConditionalLeftOvers(methodName);
		}
	}

	private void removeConditionalLeftOvers(String methodName) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		boolean add = true;
		
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;

				if (lStmt.getLabelString().matches(IfTestLabelRegex)) {
					LIRStatement next = this.pf.getLirMap().get(methodName).get(i+1);
					
					if (!next.getClass().equals(CmpStmt.class)) {
						add = false;
						continue;
					}
						
				}
				else if (lStmt.getLabelString().matches(IfEndLabelRegex)) {
					if (!add) {
						add = true;
						continue;
					}
				}
				
			}
			
			if (add) {
				newStmts.add(stmt);
			}
		}
		
		this.pf.getLirMap().put(methodName, newStmts);
	}

	private void removeArrayCheckLeftOvers(String methodName) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		boolean add = true;
		boolean removeArrayLabels = false;
		
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				
				if (lStmt.getLabelString().matches(ArrayBeginLabelRegex)) {
					LIRStatement next = this.pf.getLirMap().get(methodName).get(i+1);
					
					if (next.getClass().equals(JumpStmt.class)) {
						JumpStmt jStmt = (JumpStmt) next;
						if (jStmt.getLabel().getLabelString().matches(ArrayPassLabelRegex)) {
							// Dead code entire thing!
							add = false;
						}
						else {
							// Throw exception! Dead code everything except exception shit
							removeArrayLabels = true;
							i = i + 2;
							continue;
						}
					}
				}
				else if (lStmt.getLabelString().matches(ArrayPassLabelRegex)) {
					if (!add) {
						add = true;
						continue;
					}
					
					if (removeArrayLabels) {
						removeArrayLabels = false;
						continue;
					}
				}
				else if (lStmt.getLabelString().matches(ArrayFailLabelRegex)) {
					if (removeArrayLabels) continue;
				}
				
			}
			
			if (add) {
				newStmts.add(stmt);
			}
		}
		
		this.pf.getLirMap().put(methodName, newStmts);
	}

	private void removeRedundantJumps(String methodName) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (stmt.getClass().equals(JumpStmt.class)) {
				JumpStmt jStmt = (JumpStmt) stmt;
				LIRStatement next = this.pf.getLirMap().get(methodName).get(i+1);
				
				if (jStmt.getLabel().equals(next)) { // Conditional or unconitional jump to next stmt
					i++;
					continue;
				}
			}
			
			newStmts.add(stmt);
		}
		
		this.pf.getLirMap().put(methodName, newStmts);
		
	}

	private void removeDeadStmtsFromLIR(String methodName) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (!isDead(stmt)) {
				newStmts.add(stmt);
			}
		}
		
		this.pf.getLirMap().put(methodName, newStmts);
	}

	private void getDeadBlocks(String methodName) {
		for (CFGBlock block: cb.getCfgMap().get(methodName)) {
			if (block.getPredecessors().size() == 0 && block.getIndex() != 0) {
				this.deadBlocks.add(block);
			}
		}
	}
	
	private boolean isDead(LIRStatement stmt) {
		for (CFGBlock block: this.deadBlocks) {
			for (LIRStatement s: block.getStatements()) {
				if (stmt == s) return true;
			}
		}
		
		return false;
	}

	private void optimize(String methodName) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (stmt.getClass().equals(CmpStmt.class)) {
				i = optimizeConditionalJump(methodName, newStmts, i);
			}
			else {
				newStmts.add(stmt);
			}
		}
		
		this.pf.getLirMap().put(methodName, newStmts);
	}

	private int optimizeConditionalJump(String methodName, List<LIRStatement> newStmts, int i) {
		CmpStmt cStmt = (CmpStmt) this.pf.getLirMap().get(methodName).get(i);
		if (cStmt.getArg1().getClass().equals(ConstantName.class) 
				&& cStmt.getArg2().getClass().equals(ConstantName.class)) {
			int arg1 = Integer.parseInt(((ConstantName)cStmt.getArg1()).getValue());
			int arg2 = Integer.parseInt(((ConstantName)cStmt.getArg2()).getValue());
			
			if (this.pf.getLirMap().get(methodName).get(i+1).getClass().equals(JumpStmt.class)) {
				JumpStmt jStmt = (JumpStmt) this.pf.getLirMap().get(methodName).get(i+1);
				
				if (jStmt.getCondition() != JumpCondOp.NONE) { // Unconditional jump, can't optimize
				
					boolean takeJump = evaluateJump(arg1, arg2, jStmt.getCondition());
					
					if (takeJump) {
						newStmts.add(new JumpStmt(JumpCondOp.NONE, jStmt.getLabel())); // Make it unconditional, else remove
					}
					
					return i + 1;
				}
			}
		}
		
		newStmts.add(cStmt);
		
		return i;
	}

	private boolean evaluateJump(int arg1, int arg2, JumpCondOp condition) {
		switch (condition) {
			case EQ:
				return arg1 == arg2;
			case NEQ:
				return arg1 != arg2;
			case ZERO:
				return arg1 == arg2; // jz is identical to je.
			case GT:
				return arg1 > arg2;
			case GTE:
				return arg1 >= arg2;
			case LT:
				return arg1 < arg2;
			case LTE:
				return arg1 <= arg2;
		}
		
		return false;
	}
}
