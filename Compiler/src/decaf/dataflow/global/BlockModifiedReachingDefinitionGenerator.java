package decaf.dataflow.global;

import java.util.ArrayList;
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
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class BlockModifiedReachingDefinitionGenerator {
	private HashMap<String, MethodIR> mMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockReachingDefs;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	// Map from Name to QuadrupletStmt which assign to that Name
	private HashMap<Name, ArrayList<QuadrupletStmt>> nameToQStmts;
	// Map from Name to number of times int was defined
	private HashMap<Name, Integer> nameToInt;
	// Map from bitoffset int to the QuadrupletStmt corresponding to it
	private HashMap<Integer, QuadrupletStmt> intToQStmt;
	private HashSet<Name> disregardName;
	
	private int totalDefinitions;
	
	private List<CFGBlock> cfgBlocks;
	private LoopBlock loopBlock;
	
	public BlockModifiedReachingDefinitionGenerator() {
	}
	
	public void generate() {
		initialize();
		if (totalDefinitions == 0)
			return;
		// Get the first block in the main function - TODO: is there a better way?
		CFGBlock entry = getCfgBlocks().get(0); //this.getBlockById("main", 0);
		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(totalDefinitions);
		calculateGenKillSets(entry, entryBlockFlow);
		entryBlockFlow.setOut(entryBlockFlow.getGen());
		cfgBlocksToProcess.remove(entry);
		blockReachingDefs.put(entry, entryBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {		
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockDataFlowState bFlow = generateForBlock(block);
			blockReachingDefs.put(block, bFlow);
		}
	}
	

	private void setInitVals(){
		nameToQStmts = new HashMap<Name, ArrayList<QuadrupletStmt>>();
		blockReachingDefs = new HashMap<CFGBlock, BlockDataFlowState>();
		this.cfgBlocksToProcess = new HashSet<CFGBlock>();
		this.disregardName = new HashSet<Name>();
		this.intToQStmt = new HashMap<Integer, QuadrupletStmt>();
		totalDefinitions = 0;

		
	}
	
	private void initialize() {
		// QuadrupletStmt IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		setInitVals();
		QuadrupletStmt.setID(0);
			for (CFGBlock block: this.getCfgBlocks()) {
				List<LIRStatement> blockStmts = block.getStatements();	
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.getClass().equals(QuadrupletStmt.class)) {
						QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
						Name dest = qStmt.getDestination();
						if (dest != null) {
							// Destination has to be non-null for this to be a valid definition
							// of something
							qStmt.setMyId();
							if (!nameToQStmts.containsKey(dest)) {
								System.out.println("DEST NAME: " + dest.toString());
								nameToQStmts.put(dest, new ArrayList<QuadrupletStmt>());
							}
							nameToQStmts.get(dest).add(qStmt);
							intToQStmt.put(qStmt.getMyId(), qStmt);
							totalDefinitions++;
						}
					}
				}
				cfgBlocksToProcess.add(block);
			}
	}
	
	private BlockDataFlowState generateForBlock(CFGBlock block) {
		// Get the original out BitSet for this block
		BitSet origOut;
		if (blockReachingDefs.containsKey(block)) {
			origOut = blockReachingDefs.get(block).getOut();
		} else {
			origOut = new BitSet(totalDefinitions);
			// Confluence operator is AND, so initialize out set to all 1s
			//origOut.set(0, totalDefinitions, true);
		}
		// Calculate the in BitSet by making sure that a particular definition was defined only once
		// otherwise set all of its definitions to 0
		BlockDataFlowState bFlow = new BlockDataFlowState(totalDefinitions);
		BitSet in = bFlow.getIn();
		in.set(0, totalDefinitions, true);
		for (CFGBlock pred : block.getPredecessors()) {
			if (blockReachingDefs.containsKey(pred)) {
				in.and(blockReachingDefs.get(pred).getOut()); 
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
				if (!cfgBlocksToProcess.contains(succ) && succ.getIndex() < loopBlock.getEndBlockID() && succ.getIndex() != loopBlock.getStartBlockID()+1) {
					//System.out.println("Successor ID: " +  succ.getIndex() + " parent ID : " + block.getIndex() + " endBlockID: " + loopBlock.getEndBlockID());
					cfgBlocksToProcess.add(succ);
				}
			}
		}
		// Remove this block, since it has been processed
		cfgBlocksToProcess.remove(block);
		return bFlow;
	}
	
	private void calculateGenKillSets(CFGBlock block, BlockDataFlowState bFlow) {
		BitSet gen = bFlow.getGen();
		List<LIRStatement> blockStmts = block.getStatements();
		
		for (LIRStatement stmt : blockStmts) {
			if (stmt.getClass().equals(CallStmt.class)) {
				invalidateFunctionCall(bFlow);
				continue;
			}
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
				Name dest = qStmt.getDestination();
				
				if (dest != null) {
					// Valid reaching definition
					updateKillSet(dest, bFlow);
					// Gen - add current statement id
					if(!this.disregardName.contains(dest)){
						gen.set(qStmt.getMyId(), true);
						this.disregardName.add(dest);
					}
				}
			}
		}
	}
	
	public void invalidateFunctionCall(BlockDataFlowState bFlow) {
		// Invalidate arg registers
		for (int i = 0; i < Register.argumentRegs.length; i++) {
			updateKillSet(new RegisterName(Register.argumentRegs[i]), bFlow);
		}
		
		// Reset symbolic value for %RAX
		updateKillSet(new RegisterName(Register.RAX), bFlow);
		
		// Invalidate global vars;
		for (Name name: this.nameToQStmts.keySet()) {
			if (name.getClass().equals(VarName.class)) {
				VarName var = (VarName) name;
				if (var.getBlockId() == -1) { // Global
					updateKillSet(name, bFlow);
				}
			}
		}
	}
	
	public void updateKillSet(Name newDest, BlockDataFlowState bFlow) {
		BitSet kill = bFlow.getKill();
		BitSet in = bFlow.getIn();
		ArrayList<QuadrupletStmt> qStmtsForDest = nameToQStmts.get(newDest);
		if (qStmtsForDest != null) {
			// Kill if it is part of In
			for (QuadrupletStmt q : qStmtsForDest) {
				int index = q.getMyId();
				if (in.get(index)) {
					kill.set(index, true); // Ensures Kill is always a subset of In
				}
			}
		}
	}
	
	public HashMap<Name, ArrayList<QuadrupletStmt>> getNameToQStmts() {
		return nameToQStmts;
	}

	public void setNameToQStmts(HashMap<Name, ArrayList<QuadrupletStmt>> nameToQStmts) {
		this.nameToQStmts = nameToQStmts;
	}
	
	public HashMap<CFGBlock, BlockDataFlowState> getBlockReachingDefs() {
		return blockReachingDefs;
	}

	public void setBlockReachingDefs(HashMap<CFGBlock, BlockDataFlowState> blockReachingDefs) {
		this.blockReachingDefs = blockReachingDefs;
	}
	
	public int getTotalDefinitions() {
		return totalDefinitions;
	}

	public void setTotalDefinitions(int totalDefinitions) {
		this.totalDefinitions = totalDefinitions;
	}

	public void setCfgBlocks(List<CFGBlock> cfgBlocks) {
		this.cfgBlocks = cfgBlocks;
	}

	public List<CFGBlock> getCfgBlocks() {
		return cfgBlocks;
	}

	public void setLoopBlock(LoopBlock loopBlock) {
		this.loopBlock = loopBlock;
	}

	public LoopBlock getLoopBlock() {
		return loopBlock;
	}

	public void setIntToQStmt(HashMap<Integer, QuadrupletStmt> intToQStmt) {
		this.intToQStmt = intToQStmt;
	}

	public HashMap<Integer, QuadrupletStmt> getIntToQStmt() {
		return intToQStmt;
	}
}
