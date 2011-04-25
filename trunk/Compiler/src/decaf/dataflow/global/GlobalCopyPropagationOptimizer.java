package decaf.dataflow.global;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
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
		PopStmt popStmt;
		PushStmt pushStmt;
		CmpStmt cStmt;
		Name newArg1, newArg2, dest;

		for (LIRStatement stmt: block.getStatements()) {
			// Reset kill set
			bFlow.getKill().clear();
			if (stmt.getClass().equals(CallStmt.class)) {
				// Update BlockDataFlowState kill set
				assignmentDefGenerator.invalidateFunctionCall(bFlow);
				// Update BlockDataFlowState in set by using updated kill set
				bFlow.getIn().xor(bFlow.getKill());
				
			} else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				newArg1 = copyPropagateOnArg(qStmt.getArg1(), bFlow);
				if (newArg1 != null) {
					qStmt.setArg1(newArg1);
				}
				newArg2 = copyPropagateOnArg(qStmt.getArg2(), bFlow);
				if (newArg2 != null) {
					qStmt.setArg2(newArg2);
				}
				dest = qStmt.getDestination();
				if (dest != null) {
					// Check if dest is ArrayName and try to optimize index
					if (dest.getClass().equals(ArrayName.class)) {
						Name arrIndex = ((ArrayName)dest).getIndex();
						Name propagatedName = copyPropagateOnArg(arrIndex, bFlow);
						if (propagatedName != null)
							((ArrayName)dest).setIndex(propagatedName);
					}
				}
				
				// Update BlockDataFlowState kill set
				assignmentDefGenerator.updateKillGenSet(dest, bFlow);
				// Update BlockDataFlowState in set by using updated kill set
				bFlow.getIn().xor(bFlow.getKill());
				
			// Optimize PopStmt
			} else if (stmt.getClass().equals(PopStmt.class)) {
				popStmt = (PopStmt)stmt;
				newArg1 = copyPropagateOnArg(popStmt.getName(), bFlow);
				if (newArg1 != null) {
					popStmt.setName(newArg1);
				}
			
			// Optimize PushStmt
			} else if (stmt.getClass().equals(PushStmt.class)) {
				pushStmt = (PushStmt)stmt;
				newArg1 = copyPropagateOnArg(pushStmt.getName(), bFlow);
				if (newArg1 != null) {
					pushStmt.setName(newArg1);
				}
				
			// Optimize CmpStmt
			} else if (stmt.getClass().equals(CmpStmt.class)) {
				cStmt = (CmpStmt)stmt;
				newArg1 = copyPropagateOnArg(cStmt.getArg1(), bFlow);
				if (newArg1 != null) {
					cStmt.setArg1(newArg1);
				}
				newArg2 = copyPropagateOnArg(cStmt.getArg2(), bFlow);
				if (newArg2 != null) {
					cStmt.setArg2(newArg2);
				}
			}
		}
	}
	
	// For each use of a Name, see all the reaching definitions for that Name
	// If there exists only ONE reaching assignment definition that assigns to Name, 
	// replace Name with definition's LHS
	private Name copyPropagateOnArg(Name arg, BlockDataFlowState bFlow) {
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
					// so we can perform copy propagation
					// Return the name which we should replace the arg with
					return reachingAssignmentStmt.getArg1();
				}
			}
			// Check if Name is ArrayName and try to optimize index
			if (arg.getClass().equals(ArrayName.class)) {
				Name arrIndex = ((ArrayName)arg).getIndex();
				Name propagatedName = copyPropagateOnArg(arrIndex, bFlow);
				if (propagatedName != null)
					((ArrayName)arg).setIndex(propagatedName);
			}
		}
		return null;
	}
	
	public HashMap<String, List<CFGBlock>> getCfgMap() {
		return cfgMap;
	}

	public BlockAssignmentDefinitionGenerator getAssignmentDefGenerator() {
		return assignmentDefGenerator;
	}
}
