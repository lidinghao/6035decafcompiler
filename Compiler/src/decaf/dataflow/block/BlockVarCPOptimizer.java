package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ExpressionFlattenerVisitor;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockVarCPOptimizer {
	private HashMap<Name, Name> varToVar;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private ProgramFlattener pf;
	
	public BlockVarCPOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.varToVar = new HashMap<Name, Name>();
		this.cfgMap = cfgMap;
		this.pf = pf;
	}
	
	public void performCopyPropagation() {
		reset();
		
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
				reset();
			}

			// Update statements in program flattener
			List<LIRStatement> stmts = new ArrayList<LIRStatement>();
			
			for (int i = 0; i < this.cfgMap.get(s).size(); i++) {
				stmts.addAll(getBlockWithIndex(i, this.cfgMap.get(s)).getStatements());
			}
			
			pf.getLirMap().put(s, stmts);
		}
	}

	public CFGBlock getBlockWithIndex(int i, List<CFGBlock> list) {
		for (CFGBlock block: list) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		
		return null;
	}

	public void optimize(CFGBlock block) {
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.isUseStatement()) {
				if (stmt.getClass().equals(CallStmt.class)) {
					RegisterName reg;
					
					// TODO: May have to change this after RegisterAllocator is implemented
					// Invalidate arg registers
					for (int i = 0; i < ExpressionFlattenerVisitor.argumentRegs.length; i++) {
						reg = new RegisterName(ExpressionFlattenerVisitor.argumentRegs[i]);
						resetVariable(reg);
					}
					
					// Invalidate %RAX
					reg = new RegisterName(Register.RAX);
					resetVariable(reg);
					
					// Invalidate global vars and Vars to Registers
					List<Name> resetName = new ArrayList<Name>();
					for (Name name: this.varToVar.keySet()) {
						if (name.getClass().equals(VarName.class)) {
							VarName var = (VarName) name;
							if (var.getBlockId() == -1) { // Global
								resetName.add(name);
							}
						}
						
						// TODO: Might change this after Register Allocator
						if (this.varToVar.get(name).getClass().equals(RegisterName.class)) {
							resetName.add(name);
						}
					}
					
					for (Name name: resetName) {
						resetVariable(name);
					}
				}
				continue;
			}
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				processStatement(qStmt);
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				cStmt.setArg1(processArgument(cStmt.getArg1()));
				cStmt.setArg2(processArgument(cStmt.getArg2()));
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;
				pStmt.setName(processArgument(pStmt.getName()));
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;
				pStmt.setName(processArgument(pStmt.getName()));
			}
		}
	}

	private void processStatement(QuadrupletStmt qStmt) {
		Name dest = qStmt.getDestination();
		
		// If the Name being assigned is a not a DynamicVarName
		if (qStmt.getOperator().equals(QuadrupletOp.MOVE)) {
			// Invariant: This statement has to be of the form [DynamicVarName = Name]
			Name newArg1 = processArgument(qStmt.getArg1());
			qStmt.setArg1(newArg1);
			
			resetVariable(dest);
			this.varToVar.put(dest, newArg1);
		} else {
			// Check the operands, if any of them are DynamicVarName, replace with Name from the tempToName map
			Name newArg1 = processArgument(qStmt.getArg1());
			Name newArg2 = processArgument(qStmt.getArg2());
			qStmt.setArg1(newArg1);
			qStmt.setArg2(newArg2);
			
			// Clear varToVar for the Name that was assigned, make the temps point to themselves
			resetVariable(dest);
		}		
	}

	private Name processArgument(Name arg1) {
		if (arg1 != null) {
			if (this.varToVar.containsKey(arg1)) {
				return this.varToVar.get(arg1);
			}
		}
		return arg1;
	}

	private void resetVariable(Name name) {
		if (name == null) return;
		
		this.varToVar.remove(name);	
		
		List<Name> varsToRemove = new ArrayList<Name>();
		for (Name key: this.varToVar.keySet()) {
			if (this.varToVar.get(key).equals(name)) {
				varsToRemove.add(key);
			}
		}
		
		for (Name n: varsToRemove) {
			this.varToVar.remove(n);
		}
	}

	private void reset() {
		this.varToVar.clear();		
	}
}
