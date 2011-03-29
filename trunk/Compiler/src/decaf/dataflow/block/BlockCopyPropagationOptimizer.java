package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.DataStmt;
import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ExpressionFlattenerVisitor;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockCopyPropagationOptimizer {
	private HashMap<DynamicVarName, Name> tempToName;
	private HashMap<Name, HashSet<DynamicVarName>> varToTemps;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private ProgramFlattener pf;
	
	public BlockCopyPropagationOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.tempToName = new HashMap<DynamicVarName, Name>();
		this.varToTemps = new HashMap<Name, HashSet<DynamicVarName>>();
		this.cfgMap = cfgMap;
		this.pf = pf;
	}
	
	public void performCopyProp() {
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
			if (!stmt.isExpressionStatement()) {
				// TODO: May have to change this after RegisterAllocator is implemented
				if (stmt.getClass().equals(CallStmt.class)) {
					RegisterName reg;
					
					// Invalidate arg registers
					for (int i = 0; i < ExpressionFlattenerVisitor.argumentRegs.length; i++) {
						reg = new RegisterName(ExpressionFlattenerVisitor.argumentRegs[i]);
						resetVariable(reg);
					}
					
					// Invalidate %RAX
					reg = new RegisterName(Register.RAX);
					resetVariable(reg);
					
					// Invalidate global vars;
					for (Name name: this.varToTemps.keySet()) {
						if (name.getClass().equals(VarName.class)) {
							VarName var = (VarName) name;
							if (var.getBlockId() == -1) { // Global
								resetVariable(name);
							}
						}
					}
				}
				continue;
			}
			QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
			processStatement(qStmt);
		}
	}

	private void resetVariable(Name dest) {
		HashSet<DynamicVarName> tempsToAssignedName = this.varToTemps.get(dest);
		if (tempsToAssignedName != null) {
			Iterator<DynamicVarName> it = tempsToAssignedName.iterator();
			while (it.hasNext()) {
				DynamicVarName temp = it.next();
				this.tempToName.put(temp, temp);
			}
			
			this.varToTemps.get(dest).clear();
		}
	}
	
	public void processStatement(QuadrupletStmt qStmt) {
		Name dest = qStmt.getDestination();
		
		// If the Name being assigned is a DynamicVarName
		if (dest.getClass().equals(DynamicVarName.class)) {
			
			// Invariant: This statement has to be of the form [DynamicVarName = Name]
			Name arg1 = qStmt.getArg1();
			this.tempToName.put((DynamicVarName)dest, arg1);
			if (!this.varToTemps.containsKey(arg1)) {
				this.varToTemps.put(arg1, new HashSet<DynamicVarName>());
			}
			this.varToTemps.get(arg1).add((DynamicVarName)dest);
			
		} else {
			// Check the operands, if any of them are DynamicVarName, replace with Name from the tempToName map
			Name newArg1 = processArgument(qStmt.getArg1());
			Name newArg2 = processArgument(qStmt.getArg2());
			qStmt.setArg1(newArg1);
			qStmt.setArg2(newArg2);
			
			// Clear varToTemps for the Name that was assigned, make the temps point to themselves
			resetVariable(dest);
		}
	}

	public Name processArgument(Name arg1) {
		if (arg1 != null) {
			if (arg1.getClass().equals(DynamicVarName.class)) {
				if (this.tempToName.containsKey((DynamicVarName)arg1)) {
					return this.tempToName.get((DynamicVarName)arg1);
				}
			}
		}
		return arg1;
	}
	
	public void reset() {
		tempToName.clear();
		varToTemps.clear();
	}
}
