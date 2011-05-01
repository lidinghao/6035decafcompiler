package decaf.dataflow.global;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

// This keeps track of how many assignment definitions reach a given block
// An assignment definition reaches a block if:
// 	1. The definition is like a = b
// 	2. 'b' is not re-assigned in ANY execution path from the assignment definition
// 	3. 'a' is not re-assigned in ANY execution path from the assignment definition
// If 'a' is used in some definition, and if there exists only ONE reaching assignment 
// definition that assigns to 'a', replace 'a' with definition's LHS

public class BlockAssignmentDefinitionGenerator {
	private HashMap<String, MethodIR> mMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockAssignReachingDefs;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<QuadrupletStmt, Integer> uniqueAssignmentStmts;
	// Map from Name to set of QuadrupletStmts which assign to that Name
	private HashMap<Name, HashSet<QuadrupletStmt>> nameToQStmtsThatAssignIt;
	// Map from Name to set of QuadrupletStmts which use that Name to assign something
	private HashMap<Name, HashSet<QuadrupletStmt>> nameToQStmtsWhichItAssigns;
	private int totalAssignmentDefinitions;
	
	public BlockAssignmentDefinitionGenerator(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
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
		CFGBlock entry = this.getBlockById("main", 0);
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
	
	private CFGBlock getBlockById(String name, int i) {
		if (this.mMap.containsKey(name)) {
			for (CFGBlock b: this.mMap.get(name).getCfgBlocks()) {
				if (b.getIndex() == i) return b;
			}
		}
		
		return null;
	}
	
	// Each unique 'a = b' will have an id
	private void initialize() {
		// QuadrupletStmt IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		QuadrupletStmt.setID(0);
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
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
			// Confluence operator is AND, so initialize out set to all 1s
			origOut.set(0, totalAssignmentDefinitions, true);
		}
		// Calculate the in BitSet by taking union of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(totalAssignmentDefinitions);
		// If there exists at least one predecessor, set In to all True
		if (block.getPredecessors().size() > 0) {
			bFlow.getIn().set(0, totalAssignmentDefinitions);
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
				CallStmt callStmt = (CallStmt) stmt;
				if (callStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				
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
			if (name.getClass().equals(ArrayName.class)) {
				updateKillGenSet(name, bFlow);
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
			if (name.getClass().equals(ArrayName.class)) {
				updateKillGenSet(name, bFlow);
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
		Set<QuadrupletStmt> allStmts = uniqueAssignmentStmts.keySet();
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
		HashSet<QuadrupletStmt> stmtsThatAssignIt = nameToQStmtsThatAssignIt.get(dest);
		if (stmtsThatAssignIt != null) {
			for (QuadrupletStmt qStmt : stmtsThatAssignIt) {
				if (bFlow.getIn().get(qStmt.getMyId())) {
					kill.set(qStmt.getMyId(), true);
				}
				gen.set(qStmt.getMyId(), false);
			}
		}
		// The dest could be an index of some ArrayName used in an assignment statement
		// (either as dest or arg1) so invalidate such statements
		if (allStmts != null) {
			for (QuadrupletStmt q : allStmts) {
				if (isAssignStmtUsingArrayIndex(q, dest)) {
					if (bFlow.getIn().get(q.getMyId())) {
						kill.set(q.getMyId(), true);
					}
					gen.set(q.getMyId(), false);
				}
			}
		}
		// If dest is ArrayName, check index variable
		// If index variable is not a ConstantName, invalidate all previous assignments
		// which have arg1/dest as some ArrayName
		// If index variable is a ConstantName, invalidate all previous assignments
		// which have arg1/dest as some ArrayName with variable index variable
		if (dest.getClass().equals(ArrayName.class)) {
			String id = ((ArrayName)dest).getId();
			Name arrIndex = ((ArrayName)dest).getIndex();
			if (arrIndex.getClass().equals(ConstantName.class)) {
				if (allStmts != null) {
					for (QuadrupletStmt qStmt : allStmts) {
						// Check id and non-constant index
						if (isArrayNameWithIdAndVariableIndex(qStmt.getArg1(), id) || 
								isArrayNameWithIdAndVariableIndex(qStmt.getDestination(), id)) {
							if (bFlow.getIn().get(qStmt.getMyId())) {
								kill.set(qStmt.getMyId(), true);
							}
							gen.set(qStmt.getMyId(), false);
						}
					}
				}
			} else {
				if (allStmts != null) {
					for (QuadrupletStmt qStmt : allStmts) {
						// Just check id
						if (isArrayNameWithId(qStmt.getArg1(), id) || 
								isArrayNameWithId(qStmt.getDestination(), id)) {
							if (bFlow.getIn().get(qStmt.getMyId())) {
								kill.set(qStmt.getMyId(), true);
							}
							gen.set(qStmt.getMyId(), false);
						}
					}
				}
			}
		}
	}
	
	private boolean isArrayNameWithIdAndVariableIndex(Name name, String id) {
		if (name == null)
			return false;
		if (name.getClass().equals(ArrayName.class)) {
			if (isArrayNameWithId(name, id)) {	
				Name arrIndex = ((ArrayName)name).getIndex();
				if (!(arrIndex.getClass().equals(ConstantName.class))) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isArrayNameWithId(Name name, String id) {
		if (name == null) 
			return false;
		if (name.getClass().equals(ArrayName.class)) {
			if (((ArrayName)name).getId().equals(id)) {
				return true;
			}
		}
		return false;
	}
	
	// Assumes stmt is of the form a = b
	private boolean isAssignStmtUsingArrayIndex(QuadrupletStmt stmt, Name index) {
		Name dest = stmt.getDestination();
		Name arg1 = stmt.getArg1();
		Name arrIndex;
		if (dest.getClass().equals(ArrayName.class)) {
			arrIndex = ((ArrayName)dest).getIndex();
			if (arrIndex.equals(index))
				return true;
		}
		if (arg1.getClass().equals(ArrayName.class)) {
			arrIndex = ((ArrayName)arg1).getIndex();
			if (arrIndex.equals(index))
				return true;
		}
		return false;
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
