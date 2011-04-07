package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockTempDCOptimizer {
	private Set<Name> neededSet;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private ProgramFlattener pf;
	
	public BlockTempDCOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.neededSet = new HashSet<Name>();
		this.cfgMap = cfgMap;
		this.pf = pf;
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

	private CFGBlock getBlockWithIndex(int i, List<CFGBlock> list) {
		for (CFGBlock block: list) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		
		return null;
	}
	
	private void optimize(CFGBlock block) {
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
			
			// Don't add statements like x = x
			if (qStmt.getOperator() == QuadrupletOp.MOVE) {
				if (qStmt.getDestination().equals(qStmt.getArg1())) continue;
			}
			
			Name dest = qStmt.getDestination();
			
			// If assigning to a required Name
			if (dest.getClass().equals(DynamicVarName.class)) {
				if (!neededSet.contains(dest)) {
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

