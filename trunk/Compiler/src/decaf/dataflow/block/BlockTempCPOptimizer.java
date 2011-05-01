package decaf.dataflow.block;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class BlockTempCPOptimizer {
	private HashMap<Name, Name> tempToName;
	private HashMap<Name, HashSet<Name>> varToTemps;
	private HashMap<String, MethodIR> mMap;
	
	public BlockTempCPOptimizer(HashMap<String, MethodIR> mMap) {
		this.tempToName = new HashMap<Name, Name>();
		this.varToTemps = new HashMap<Name, HashSet<Name>>();
		this.mMap = mMap;
	}
	
	public void performCopyPropagation() {
		reset();
		
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				optimize(block);
				reset();
			}

			this.mMap.get(s).regenerateStmts();
		}
	}
	
	public void optimize(CFGBlock block) {
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.getClass().equals(QuadrupletStmt.class)) {
				// TODO: May have to change this after RegisterAllocator is implemented
				if (stmt.getClass().equals(CallStmt.class)) {
					CallStmt callStmt = (CallStmt) stmt;
					if (callStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
					
					RegisterName reg;
					
					// Invalidate arg registers
					for (int i = 0; i < Register.argumentRegs.length; i++) {
						reg = new RegisterName(Register.argumentRegs[i]);
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
		HashSet<Name> tempsToAssignedName = this.varToTemps.get(dest);
		if (tempsToAssignedName != null) {
			Iterator<Name> it = tempsToAssignedName.iterator();
			while (it.hasNext()) {
				Name temp = it.next();
				this.tempToName.put(temp, temp);
			}
			
			this.varToTemps.get(dest).clear();
		}
	}
	
	public void processStatement(QuadrupletStmt qStmt) {
		Name dest = qStmt.getDestination();
		
		// If the Name being assigned is a DynamicVarName
		if (dest.getClass().equals(DynamicVarName.class) && qStmt.getOperator().equals(QuadrupletOp.MOVE)) {
			DynamicVarName dVar = (DynamicVarName) dest;
			// Dont mess with DynamicVarNames that are gtmp
			if (!dVar.isForGlobal()) {
				// Invariant: This statement has to be of the form [DynamicVarName = Name]
				Name arg1 = qStmt.getArg1();
				this.tempToName.put(dest, arg1);
				if (!this.varToTemps.containsKey(arg1)) {
					this.varToTemps.put(arg1, new HashSet<Name>());
				}
				this.varToTemps.get(arg1).add(dest);
			}
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
				DynamicVarName dVar = (DynamicVarName) arg1;
				
				// Dont mess with DynamicVarNames that are gtmp
				if (!dVar.isForGlobal()) {
					if (this.tempToName.containsKey(arg1)) {
						return this.tempToName.get(arg1);
					}
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
