package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockDeadCodeOptimizer {
	private HashSet<Name> neededSet;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private ProgramFlattener pf;
	private int tempCount;
	
	public BlockDeadCodeOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.neededSet = new HashSet<Name>();
		this.cfgMap = cfgMap;
		this.pf = pf;
		this.tempCount = 0;
	}

	public void performDeadCodeElimination() {
		for (String s: this.cfgMap.keySet()) {
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
				reset();
			}

			// Fix stack size
			for (CFGBlock block: this.cfgMap.get(s)) {
				if (block.getIndex() == 0) {
					for (LIRStatement stmt: block.getStatements()) {
						if (stmt.getClass().equals(EnterStmt.class)) {
							EnterStmt enter = (EnterStmt) stmt;
							enter.setStackSize(enter.getStackSize() + tempCount);
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
			this.tempCount = 0;
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
			
			// If assigning to a DynamicVarName
			if (dest.getClass().equals(DynamicVarName.class)) {
				if (!neededSet.contains(dest)) {
					this.tempCount--;
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

