package decaf.optimize;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;
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
	
	private ProgramFlattener pf;
	private CFGBuilder cb;
	private List<CFGBlock> deadBlocks;
	
	public StaticJumpEvaluator(ProgramFlattener pf, CFGBuilder cb) {
		this.pf = pf;
		this.cb = cb;
		this.deadBlocks = new ArrayList<CFGBlock>();
	}
	
	public void staticEvaluateJumps() {
		cb.setMergeBoundChecks(true);
		cb.generateCFGs();
		
		for (String methodName: this.pf.getLirMap().keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			String prev = "";
			String current = this.pf.getLirMap().get(methodName).toString();
			
			while (!prev.equals(current)) {
				// Regen cfg
				cb.generateCFGs();
				
				System.out.println("BEFORE COND OPTIMIZE");
				this.cb.printCFG(System.out);
				
				optimize(methodName);
				dcBlocksAndFixLIR(methodName);
				
				prev = current;
				current = this.pf.getLirMap().get(methodName).toString();
			}
		}
		
		cb.generateCFGs();
	}

	private void dcBlocksAndFixLIR(String methodName) {
		cb.generateCFGs();
		
		this.deadBlocks.clear();
		

		System.out.println("BEFORE DC CFG");
		this.cb.printCFG(System.out);
		
		// Gen dead CFG block
		getDeadCFGBlocks(methodName);

		System.out.println("BEFORE DC STATEMENTS");
		this.cb.printCFG(System.out);
		
		// Remove from LIR lists	
		removeDeadStmtsFromLIR(methodName);
		
		System.out.println("BEFORE REDUNDANT JMPS");
		this.cb.printCFG(System.out);
		
		// Remove redundant jumps
		removeRedundantJumps(methodName);
		
		// Remove array check left overs
		removeArrayCheckLeftOvers(methodName);
		
		// Peephole jumps
		peepholeJumps(methodName);
		
		// Remove redundant cmps
		removeRedundantCmps(methodName);
	}
	
	private void removeRedundantCmps(String methodName) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (stmt.getClass().equals(CmpStmt.class)) {
				LIRStatement next = this.pf.getLirMap().get(methodName).get(i+1);
				if (!next.getClass().equals(JumpStmt.class)) {
					continue;
				}
			}
			
			newStmts.add(stmt);
		}
		
		this.pf.getLirMap().put(methodName, newStmts);
	}

	private void peepholeJumps(String methodName) {
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (stmt.getClass().equals(JumpStmt.class)) {
				JumpStmt jStmt = (JumpStmt)stmt;
				jStmt.setLabel(getJumpLabel(methodName, jStmt.getLabel()));
			}
		}
	}

	private LabelStmt getJumpLabel(String methodName, LabelStmt label) {
		LabelStmt lastLeader = label;
		int index = getIdForStmt(methodName, label);
		
		LIRStatement stmt = this.pf.getLirMap().get(methodName).get(index);
		while (stmt.getClass().equals(LabelStmt.class)) {
			if (stmt.isLeader()) {
				lastLeader = (LabelStmt) stmt;
			}
			
			index++;
			stmt = this.pf.getLirMap().get(methodName).get(index);
		}
		
		if (stmt.getClass().equals(JumpStmt.class)) {
			JumpStmt jStmt = (JumpStmt) stmt;
			if (jStmt.getCondition() == JumpCondOp.NONE) {
				return jStmt.getLabel();
			}
		}

		return lastLeader;
	}

	private int getIdForStmt(String methodName, LabelStmt label) {
//		System.out.println("B: " + label);
//		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
//			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
//			System.out.println(stmt);
//		}
//		System.out.println("JAJAJAJAJAJJJAJA");
		
		
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (stmt.equals(label)) {
				return i;
			}
		}
		
		return -1;
	}

	private void removeArrayCheckLeftOvers(String methodName) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		boolean inArrayCheck = false;
		boolean add = true;
		boolean removeArrayLabels = false;
		boolean skipJunk = false;
		
		for (int i = 0; i < this.pf.getLirMap().get(methodName).size(); i++) {
			LIRStatement stmt = this.pf.getLirMap().get(methodName).get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				
				if (lStmt.getLabelString().matches(ArrayBeginLabelRegex)) {
					LIRStatement next = this.pf.getLirMap().get(methodName).get(i+1);
					
					inArrayCheck = true;
					
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
					inArrayCheck = false;
					
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
			
			
			// HACK to get rid of some repeated shit
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				if (qStmt.getArg1().getClass().equals(VarName.class)) {
					VarName var = (VarName) qStmt.getArg1();
					
					if (var.isString() && ProgramFlattener.arrayExceptionErrorLabel.equals(var.getId())) {
						if (!inArrayCheck) skipJunk = true;
					}
				}
			}
			
			if (stmt.getClass().equals(CallStmt.class)) {
				CallStmt cStmt = (CallStmt) stmt;
				if (cStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) {
					if (skipJunk) {
						skipJunk = false;
						continue;
					}
				}
			}
			
			if (skipJunk) continue;
			
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
				
				if (jStmt.getLabel().equals(next)) { // Conditional or unconitional jump to very next stmt
					//i++;
					continue;
				}
				
				if (next.getClass().equals(JumpStmt.class)) {
					JumpStmt jStmt2 = (JumpStmt) next;
					if (jStmt2.getCondition() == JumpCondOp.NONE) {
						if (jStmt.getLabel().equals(jStmt2.getLabel())) {
							jStmt.setCondition(JumpCondOp.NONE);
							newStmts.add(jStmt);
							i++;
							continue;
						}
					}
				}
			}
			
			newStmts.add(stmt);
		}
		
		this.pf.getLirMap().put(methodName, newStmts);
		
	}

	private void removeDeadStmtsFromLIR(String methodName) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (LIRStatement stmt: this.pf.getLirMap().get(methodName)) {			
			if (!isDead(stmt)) {
				newStmts.add(stmt);
			}
		}
		
		this.pf.getLirMap().put(methodName, newStmts);
	}

	private void getDeadCFGBlocks(String methodName) {
		for (CFGBlock block: cb.getCfgMap().get(methodName)) {
			if (block.getPredecessors().size() == 0 && block.getIndex() != 0) {
//				System.out.println(block + "\n====");
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
