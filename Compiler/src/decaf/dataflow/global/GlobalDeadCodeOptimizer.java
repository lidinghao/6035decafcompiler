package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class GlobalDeadCodeOptimizer {
	private HashMap<String, MethodIR> mMap;
	private BlockLivenessGenerator livenessGenerator;
	private HashMap<CFGBlock, BlockDataFlowState> blockLiveVars;
	private HashMap<Name, Variable> nameToVar;
	
	public GlobalDeadCodeOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.livenessGenerator = new BlockLivenessGenerator(mMap);
		this.livenessGenerator.generate();
		this.blockLiveVars = livenessGenerator.getBlockLiveVars();
		this.nameToVar = livenessGenerator.getNameToVar();
	}
	
	public void performDeadCodeElimination(){
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				optimize(block);
			}
			
			this.mMap.get(s).regenerateStmts();
		}
	}
	
	private void optimize(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		BlockDataFlowState bFlow = blockLiveVars.get(block);
		Integer varId; 
		LIRStatement stmt;
		PopStmt popStmt;
		PushStmt pushStmt;
		CmpStmt cStmt;
		QuadrupletStmt qStmt;
		Name arg1 = null, arg2 = null, dest = null, arrIndex = null;
		Variable arg1Var, arg2Var;
		
		// Only QuadrupletStmt is dead code eliminated
		for (int i = block.getStatements().size()-1; i >= 0 ; i--) {
			stmt = block.getStatements().get(i);
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				if (!assigningRegisters(qStmt)) {
					// Only dead code eliminate statements that don't assign registers
					dest = qStmt.getDestination();
					if (nameToVar.containsKey(dest)) {
						varId = nameToVar.get(dest).getMyId();
						if (isDead(varId, bFlow.getOut())) {
							// Don't add statement
							System.out.println("Stmt REMOVED: " + stmt);
							continue;
						}
					}
				}
			}
			// Add the arguments to the out set for different types of statements
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				arg1 = qStmt.getArg1();
				arg2 = qStmt.getArg2();
				
			} else if (stmt.getClass().equals(PopStmt.class)) {
				popStmt = (PopStmt)stmt;
				arg1 = popStmt.getName();
				
			} else if (stmt.getClass().equals(PushStmt.class)) {
				pushStmt = (PushStmt)stmt;
				arg1 = pushStmt.getName();
				
			} else if (stmt.getClass().equals(CmpStmt.class)) {
				cStmt = (CmpStmt)stmt;
				arg1 = cStmt.getArg1();
				arg2 = cStmt.getArg2();
				
			}
			// This ensures correctness within each block so statements that are not used
			// OUT of the block BUT are used in the future WITHIN the block are still
			// retained
			if (dest != null) {
				// If dest is a ArrayName, process the index Name
				if (dest.getClass().equals(ArrayName.class)) {
					arrIndex = ((ArrayName)dest).getIndex();
					arg1Var = nameToVar.get(arrIndex);
					if (arg1Var != null) {	
						bFlow.getOut().set(arg1Var.getMyId());
					}
				}
				// If dest is not ArrayName, we don't care about it
			}
			if (arg1 != null) {
				// Set arg1 -> id to true in current use set
				arg1Var = nameToVar.get(arg1);
				if (arg1Var != null) {	
					bFlow.getOut().set(arg1Var.getMyId());
				}
				// If arg1 is a ArrayName, process the index Name
				if (arg1.getClass().equals(ArrayName.class)) {
					arrIndex = ((ArrayName)arg1).getIndex();
					arg1Var = nameToVar.get(arrIndex);
					if (arg1Var != null) {	
						bFlow.getOut().set(arg1Var.getMyId());
					}
				}
			}
			if (arg2 != null) {
				// Set arg2 -> id to true in current use set
				arg2Var = nameToVar.get(arg2);
				if (arg2Var != null) {
					bFlow.getOut().set(arg2Var.getMyId());
				}
				// If arg2 is a ArrayName, process the index Name
				if (arg2.getClass().equals(ArrayName.class)) {
					arrIndex = ((ArrayName)arg2).getIndex();
					arg2Var = nameToVar.get(arrIndex);
					if (arg2Var != null) {	
						bFlow.getOut().set(arg2Var.getMyId());
					}
				}
			}
			// Add to beginning
			newStmts.add(0, stmt);
		}
		block.setStatements(newStmts);
	}
	
	private boolean assigningRegisters(QuadrupletStmt qStmt) {
		Name dest = qStmt.getDestination();
		if (dest != null) {
			return dest.getClass().equals(RegisterName.class);
		}
		return false;
	}
	
	private boolean isDead(int varId, BitSet out){
		// Check if variable Id is true (live) in the outset
		// If it is not live, then it is redefined (or not used) after this block, 
		// so anything assigning the corresponding variable can be eliminated
		return !out.get(varId);
	}
}