package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class BlockVarDCOptimizer {
	private HashMap<Name, QuadrupletStmt> definitionMap;
	private HashMap<Name, Boolean> lastDefUsed;
	private HashMap<String, MethodIR> mMap;
	
	public BlockVarDCOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.definitionMap = new HashMap<Name, QuadrupletStmt>();
		this.lastDefUsed = new HashMap<Name, Boolean>();
	}
	
	public void performDeadCodeElimination() {
		reset();
		
		for (String s: this.mMap.keySet()) {
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				optimize(block);
				reset();
			}
			
			this.mMap.get(s).regenerateStmts();
		}
	}
	
	private void optimize(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (LIRStatement stmt: block.getStatements()) {
			newStmts.add(stmt);
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				
				if (qStmt.getArg1() != null) {
					markUsed(qStmt.getArg1());
				}
				
				if (qStmt.getArg2() != null) {
					markUsed(qStmt.getArg2());
				}
				
				if (this.definitionMap.containsKey(qStmt.getDestination())) {
					if (this.lastDefUsed.get(qStmt.getDestination()) == false) {
						// Last definition was not used (Registers assignments will not be removed as they're for calls)
						if (!qStmt.getDestination().getClass().equals(RegisterName.class)) {
							newStmts.remove(this.definitionMap.get(qStmt.getDestination()));
						}
						
						// Reset state for Dest Name
						this.definitionMap.remove(qStmt.getDestination());
						this.lastDefUsed.remove(qStmt.getDestination());
					}
				}
				
				// New definition
				this.definitionMap.put(qStmt.getDestination(), qStmt);
				this.lastDefUsed.put(qStmt.getDestination(), false); // Not used so far
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				
				if (cStmt.getArg1() != null) {
					markUsed(cStmt.getArg1());
				}
				
				if (cStmt.getArg2() != null) {
					markUsed(cStmt.getArg2());
				}
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;
				
				if (pStmt.getName() != null) {
					markUsed(pStmt.getName());
				}
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;
				
				if (pStmt.getName() != null) {
					markUsed(pStmt.getName());
				}
			}
		}
		
		block.setStatements(newStmts);
	}
	
	private void markUsed(Name name) {
		this.lastDefUsed.put(name, true);
		
		if (name.isArray()) {
			ArrayName arrName = (ArrayName) name;
			for (Name n: this.lastDefUsed.keySet()) {
				if (n.isArray()) {
					ArrayName var = (ArrayName) n;
					if (var.getId().equals(arrName.getId()) && !var.getIndex().getClass().equals(ConstantName.class)) {
						this.lastDefUsed.put(n, true);
					}
				}
			}
		}
	}
	
	private void reset() {
		this.definitionMap.clear();
		this.lastDefUsed.clear();
	}
}
