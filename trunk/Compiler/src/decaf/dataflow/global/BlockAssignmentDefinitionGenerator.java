package decaf.dataflow.global;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ExpressionFlattenerVisitor;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

// This keeps track of how many assignment definitions reach a given block
// An assignment definition reaches a block if:
// 	1. The definition is like a = b
// 	2. 'b' is not re-assigned in ANY execution path from the assignment definition
// 	3. 'a' is not re-assigned in ANY execution path from the assignment definition
// If 'a' is used in some definition, and if there exists only ONE reaching assignment 
// definition that assigns to 'a', replace 'a' with definition's LHS

public class BlockAssignmentDefinitionGenerator {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockAssignReachingDefs;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<QuadrupletStmt, Integer> uniqueAssignmentStmts;
	// Map from Name to set of QuadrupletStmts which assign to that Name
	private HashMap<Name, HashSet<QuadrupletStmt>> nameToQStmtsThatAssignIt;
	// Map from Name to set of QuadrupletStmts which use that Name to assign something
	private HashMap<Name, HashSet<QuadrupletStmt>> nameToQStmtsWhichItAssigns;
	private int totalAssignmentDefinitions;
	
	public BlockAssignmentDefinitionGenerator(HashMap<String, List<CFGBlock>> cMap) {
		cfgMap = cMap;
		nameToQStmtsThatAssignIt = new HashMap<Name, HashSet<QuadrupletStmt>>();
		nameToQStmtsWhichItAssigns = new HashMap<Name, HashSet<QuadrupletStmt>>();
		uniqueAssignmentStmts = new HashMap<QuadrupletStmt, Integer>();
		blockAssignReachingDefs = new HashMap<CFGBlock, BlockDataFlowState>();
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		totalAssignmentDefinitions = 0;
	}
	
	public void generate() {
		initialize();
		if (totalAssignmentDefinitions == 0) 
			return;
		// Get the first block in the main function - TODO: is there a better way?
		CFGBlock entry = cfgMap.get("main").get(0);
		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(totalAssignmentDefinitions);
		calculateGenKillSets(entry, entryBlockFlow);
		entryBlockFlow.setOut(entryBlockFlow.getGen());
		cfgBlocksToProcess.remove(entry);
		blockAssignReachingDefs.put(entry, entryBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockDataFlowState bFlow = generateForBlock(block);
			blockAssignReachingDefs.put(block, bFlow);
		}
	}
	
	// Each unique 'a = b' will have an id
	private void initialize() {
		// QuadrupletStmt IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		QuadrupletStmt.setID(0);
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.getClass().equals(QuadrupletStmt.class)) {
						QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
						if (uniqueAssignmentStmts.containsKey(qStmt)) {
							// We have seen this assignment statement before, assign it the same ID
							// as we did before so all unique statements of the form 'a = b' have the same ID
							int origId = QuadrupletStmt.getID();
							QuadrupletStmt.setID(uniqueAssignmentStmts.get(qStmt));
							qStmt.setMyId();
							// Restore the original ID count for QuadrupletStmt
							QuadrupletStmt.setID(origId);
						} else {
							if (qStmt.isAssignmentStatement()) {
								// First time seeing this assignment statement
								qStmt.setMyId();
								Name dest = qStmt.getDestination();
								Name arg1 = qStmt.getArg1();
								// If argument is Register Name, ignore - this indirectly prevents copy propagation
								// in the following scenario:
								// a = %reg
								// b = a
								// We will not change the above to b = %reg since the register allocator will take care of this
								if (arg1.getClass().equals(RegisterName.class))
									continue;
								if (!nameToQStmtsThatAssignIt.containsKey(dest)) {
									nameToQStmtsThatAssignIt.put(dest, new HashSet<QuadrupletStmt>());
								}
								if (!nameToQStmtsWhichItAssigns.containsKey(arg1)) {
									nameToQStmtsWhichItAssigns.put(arg1, new HashSet<QuadrupletStmt>());
								}
								// Update map: name -> qStmts which assign to that name
								nameToQStmtsThatAssignIt.get(dest).add(qStmt);
								// Update map: name -> qStmts in which it is used to assign
								nameToQStmtsWhichItAssigns.get(arg1).add(qStmt);
								// Update map: unique qStmt -> id
								uniqueAssignmentStmts.put(qStmt, qStmt.getMyId());
								totalAssignmentDefinitions++;
							}
						}
					}
				}
				cfgBlocksToProcess.add(block);
			}
		}
	}
	
	private BlockDataFlowState generateForBlock(CFGBlock block) {
		// Get the original out BitSet for this block
		BitSet origOut;
		if (blockAssignReachingDefs.containsKey(block)) {
			origOut = blockAssignReachingDefs.get(block).getOut();
		} else {
			origOut = new BitSet(totalAssignmentDefinitions);
		}
		// Calculate the in BitSet by taking union of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(totalAssignmentDefinitions);
		// If there exists at least one predecessor, set In to all True
		if (block.getPredecessors().size() > 0) {
			bFlow.getIn().set(0, totalAssignmentDefinitions-1);
		}
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (blockAssignReachingDefs.containsKey(pred)) {
				in.and(blockAssignReachingDefs.get(pred).getOut());
			}
		}
		calculateGenKillSets(block, bFlow);
		// Calculate Out
		BitSet out = bFlow.getOut();
		out.or(in);
		out.xor(bFlow.getKill()); // Invariant: kill is a subset of in
		out.or(bFlow.getGen());
		if (!out.equals(origOut)) {
			// Add successors to cfgBlocks list
			for (CFGBlock succ : block.getSuccessors()) {
				if (!cfgBlocksToProcess.contains(succ)) {
					cfgBlocksToProcess.add(succ);
				}
			}
		}
		// Remove this block, since it has been processed
		cfgBlocksToProcess.remove(block);
		return bFlow;
	}
	
	// Need to take two things into account after some a = b:
	// 	1. If 'a' is re-assigned (e.g a = x + y) 
	//		2. If 'b' is re-assigned (e.g b = x + y)
	// Both cases should invalidate the a = b statement
	private void calculateGenKillSets(CFGBlock block, BlockDataFlowState bFlow) {
		List<LIRStatement> blockStmts = block.getStatements();
		QuadrupletStmt qStmt;
		
		for (LIRStatement stmt : blockStmts) {
			if (stmt.getClass().equals(CallStmt.class)) {
				invalidateFunctionCall(bFlow);
			}
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				updateKillGenSet(qStmt.getDestination(), bFlow);
				if (qStmt.isAssignmentStatement()) {
					bFlow.getGen().set(qStmt.getMyId(), true);
				}
			}
		}
	}
	
	public void invalidateFunctionCall(BlockDataFlowState bFlow) {
		// Invalidate arg registers
		for (int i = 0; i < Register.argumentRegs.length; i++) {
			updateKillGenSet(new RegisterName(Register.argumentRegs[i]), bFlow);
		}
		// Reset symbolic value for %RAX
		updateKillGenSet(new RegisterName(Register.RAX), bFlow);
		
		// Invalidate global vars
		for (Name name: this.nameToQStmtsWhichItAssigns.keySet()) {
			if (name.getClass().equals(VarName.class)) {
				VarName var = (VarName) name;
				if (var.getBlockId() == -1) { // Global
					updateKillGenSet(name, bFlow);
				}
			}
		}
		// Invalidate global vars
		for (Name name: this.nameToQStmtsThatAssignIt.keySet()) {
			if (name.getClass().equals(VarName.class)) {
				VarName var = (VarName) name;
				if (var.getBlockId() == -1) { // Global
					updateKillGenSet(name, bFlow);
				}
			}
		}
	}
	
	// Any QuadrupletStmt that is reaching or in the current gen set which 
	// contains the given Name on either side of the assignment statement 
	// should be invalidated
	public void updateKillGenSet(Name dest, BlockDataFlowState bFlow) {
		if (dest == null)
			return;
		BitSet in = bFlow.getIn();
		BitSet gen = bFlow.getGen();
		BitSet kill = bFlow.getKill();
		// Invalidate reaching or previous generated assign statements of form x = dest
		HashSet<QuadrupletStmt> stmtsItAssigns = nameToQStmtsWhichItAssigns.get(dest);
		if (stmtsItAssigns != null) {
			for (QuadrupletStmt qStmt : stmtsItAssigns) {
				if (in.get(qStmt.getMyId())) {
					kill.set(qStmt.getMyId(), true);
				}
				gen.set(qStmt.getMyId(), false);
			}
		}
		// Invalidate reaching or previous generated assign statements of form dest = x
		stmtsItAssigns = nameToQStmtsThatAssignIt.get(dest);
		if (stmtsItAssigns != null) {
			for (QuadrupletStmt qStmt : stmtsItAssigns) {
				if (bFlow.getIn().get(qStmt.getMyId())) {
					kill.set(qStmt.getMyId(), true);
				}
				gen.set(qStmt.getMyId(), false);
			}
		}
	}
	
	public HashMap<String, List<CFGBlock>> getCfgMap() {
		return cfgMap;
	}

	public HashMap<CFGBlock, BlockDataFlowState> getBlockAssignReachingDefs() {
		return blockAssignReachingDefs;
	}

	public HashSet<CFGBlock> getCfgBlocksToProcess() {
		return cfgBlocksToProcess;
	}
	
	public int getTotalAssignmentDefinitions() {
		return totalAssignmentDefinitions;
	}
	
	public HashMap<Name, HashSet<QuadrupletStmt>> getNameToQStmtsThatAssignIt() {
		return nameToQStmtsThatAssignIt;
	}

	public HashMap<Name, HashSet<QuadrupletStmt>> getNameToQstmtsWhichItAssigns() {
		return nameToQStmtsWhichItAssigns;
	}
}
