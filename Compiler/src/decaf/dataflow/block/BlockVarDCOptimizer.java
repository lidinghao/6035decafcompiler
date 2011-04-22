package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockVarDCOptimizer {
	private HashMap<Name, QuadrupletStmt> definitionMap;
	private HashMap<Name, Boolean> lastDefUsed;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private ProgramFlattener pf;
	
	public BlockVarDCOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.cfgMap = cfgMap;
		this.pf = pf;
		this.definitionMap = new HashMap<Name, QuadrupletStmt>();
		this.lastDefUsed = new HashMap<Name, Boolean>();
	}
	
	public void performDeadCodeElimination() {
		reset();
		
		for (String s: this.cfgMap.keySet()) {
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
	
	private void optimize(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (LIRStatement stmt: block.getStatements()) {
			newStmts.add(stmt);
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				
				if (qStmt.getArg1() != null) {
					this.lastDefUsed.put(qStmt.getArg1(), true);
				}
				
				if (qStmt.getArg2() != null) {
					this.lastDefUsed.put(qStmt.getArg2(), true);
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
		}
		
		block.setStatements(newStmts);
	}
	
	private CFGBlock getBlockWithIndex(int i, List<CFGBlock> list) {
		for (CFGBlock block: list) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		
		return null;
	}
	
	private void reset() {
		this.definitionMap.clear();
		this.lastDefUsed.clear();
	}
}
