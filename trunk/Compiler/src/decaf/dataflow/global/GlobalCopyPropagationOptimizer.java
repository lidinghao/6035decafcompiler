package decaf.dataflow.global;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class GlobalCopyPropagationOptimizer {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private BlockAssignmentDefinitionGenerator assignmentDefGenerator;
	
	public GlobalCopyPropagationOptimizer(HashMap<String, List<CFGBlock>> cfgMap) {
		this.cfgMap = cfgMap;
		this.assignmentDefGenerator = new BlockAssignmentDefinitionGenerator(cfgMap);
		this.assignmentDefGenerator.generate();
	}
	
	public void performGlobalCopyProp() {
		if (assignmentDefGenerator.getTotalAssignmentDefinitions() == 0)
			return;

		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			// Optimize blocks
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
			}
		}
	}

	private void optimize(CFGBlock block) {
		BlockDataFlowState bFlow = assignmentDefGenerator.getBlockAssignReachingDefs().get(block);
		QuadrupletStmt qStmt;
		
		for (LIRStatement stmt: block.getStatements()) {
			// Reset kill set
			bFlow.getKill().clear();
			if (stmt.getClass().equals(CallStmt.class)) {
				// Update BlockDataFlowState kill set
				assignmentDefGenerator.invalidateContextSwitch(bFlow);
				// Update BlockDataFlowState in set by using updated kill set
				bFlow.getIn().xor(bFlow.getKill());
			} else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				copyPropagateOnArg(qStmt.getArg1(), qStmt, bFlow);
				copyPropagateOnArg(qStmt.getArg2(), qStmt, bFlow);
				// Update BlockDataFlowState kill set
				assignmentDefGenerator.updateKillGenSet(qStmt.getDestination(), bFlow);
				// Update BlockDataFlowState in set by using updated kill set
				bFlow.getIn().xor(bFlow.getKill());
			}
		}
	}
	
	// For each use of a Name, see all the reaching definitions for that Name
	// If there exists only ONE reaching assignment definition that assigns to Name, 
	// replace Name with definition's LHS
	private void copyPropagateOnArg(Name arg, QuadrupletStmt qStmt, BlockDataFlowState bFlow) {
		if (arg != null) {
			HashMap<Name, HashSet<QuadrupletStmt>> nameToStmtsThatAssignIt = 
				assignmentDefGenerator.getNameToQStmtsThatAssignIt();
			BitSet in = bFlow.getIn();
			HashSet<QuadrupletStmt> stmtsAssigningName;
			QuadrupletStmt reachingAssignmentStmt = null;
			int numReachingDefs;
			
			stmtsAssigningName = nameToStmtsThatAssignIt.get(arg);
			numReachingDefs = 0;
			if (stmtsAssigningName != null) {
				for (QuadrupletStmt qs : stmtsAssigningName) {
					if (in.get(qs.getMyId())) {
						numReachingDefs++;
						reachingAssignmentStmt = qs;
					}
				}
				if (numReachingDefs == 1) {
					// There is exactly one assignment statement reaching this block
					// so we can safely modify the statement
					qStmt.setArg1(reachingAssignmentStmt.getArg1());
				}
			}
		}
	}
	
	public HashMap<String, List<CFGBlock>> getCfgMap() {
		return cfgMap;
	}

	public BlockAssignmentDefinitionGenerator getAssignmentDefGenerator() {
		return assignmentDefGenerator;
	}
}
