package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockDeadCodeOptimizer {
	public Class<?> cpType;
	private Set<Name> neededSet;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private Set<Name> deletedVars;
	private ProgramFlattener pf;
	
	public BlockDeadCodeOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.neededSet = new HashSet<Name>();
		this.cfgMap = cfgMap;
		this.pf = pf;
		this.deletedVars = new HashSet<Name>();
	}

	public void performDeadCodeElimination() {
		this.deletedVars.clear();
		reset();
		
		for (String s: this.cfgMap.keySet()) {
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
				reset();
			}
			
			int tempRemoved = getTempsRemoved(s);

			// Fix stack size
			for (CFGBlock block: this.cfgMap.get(s)) {
				if (block.getIndex() == 0) {
					for (LIRStatement stmt: block.getStatements()) {
						if (stmt.getClass().equals(EnterStmt.class)) {
							EnterStmt enter = (EnterStmt) stmt;
							enter.setStackSize(enter.getStackSize() - this.deletedVars.size());
						}
					}
				}
			}
			
			// Update statements in program flattener
			List<LIRStatement> stmts = new ArrayList<LIRStatement>();
			
			for (int i = 0; i < this.cfgMap.get(s).size(); i++) {
				stmts.addAll(getBlockWithIndex(i, this.cfgMap.get(s)).getStatements());
			}
			
			pf.getLirMap().put(s, stmts);
			this.deletedVars.clear();
		}
	}
	
	private int getTempsRemoved(String s) {
		for (CFGBlock block: this.cfgMap.get(s)) {
			for (LIRStatement stmt: block.getStatements()) {
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
					if (qStmt.getArg1() != null) {
						this.deletedVars.remove(qStmt.getArg1());
					}
					if (qStmt.getArg2() != null) {
						this.deletedVars.remove(qStmt.getArg2());
					}
					if (qStmt.getDestination() != null) {
						this.deletedVars.remove(qStmt.getDestination());
					}
				}
			}
		}
		
		return this.deletedVars.size();
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
		int totalStmts = block.getStatements().size();
		List<LIRStatement> blockStmts = block.getStatements();
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (int i = totalStmts - 1; i >= 0; i--) {
			LIRStatement stmt = blockStmts.get(i);
			if (!stmt.isExpressionStatement()) {
				newStmts.add(0, stmt);
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
			Name dest = qStmt.getDestination();
			
			// If assigning to a required Name
			if (dest.getClass().equals(cpType)) {
				if (!neededSet.contains(dest)) {
					deletedVars.add(dest);
					continue;
				}
			}

			addToNeededSet(qStmt.getArg1());
			addToNeededSet(qStmt.getArg2());
			
			// Add to the beginning since we are going backwards
			newStmts.add(0, stmt);
		}
		
		block.setStatements(newStmts);
	}
	
	public void addToNeededSet(Name arg) {
		if (arg != null) {
			this.neededSet.add(arg);
		}
	}
	
	public void reset() {
		this.neededSet.clear();
	}
}

