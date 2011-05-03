package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class LoopOptimizer {
	private HashMap<String, MethodIR> mMap;
	private List<CFGBlock> cfgBlocks;
	private List<LoopBlock> loopBlocks;
	private BlockLivenessGenerator livenessGenerator;
	private HashMap<CFGBlock, BlockDataFlowState> blockLiveVars;
	private HashMap<CFGBlock, BlockDataFlowState> blockReachingDefs;
	private HashMap<Integer, Variable> intToVar;
	private BlockModifiedReachingDefinitionGenerator reachingDefinitionsGenerator;
	
	
	public LoopOptimizer(HashMap<String, MethodIR> mMap){
		this.mMap = mMap;
		this.livenessGenerator = new BlockLivenessGenerator(mMap);
		reachingDefinitionsGenerator = new BlockModifiedReachingDefinitionGenerator();
	}
	
	public void performLoopOptimization(){
		this.livenessGenerator.generate(); //TODO:might cause a problem if defined only once!
		blockLiveVars = livenessGenerator.getBlockLiveVars();
		intToVar = livenessGenerator.getIntToVar();
		for(String methodName : this.mMap.keySet()){
			optimizeMethod(methodName);
			this.mMap.get(methodName).regenerateStmts();
		}
	}
	
	private void optimizeMethod(String methodName){
		
		this.cfgBlocks = this.mMap.get(methodName).getCfgBlocks();
		loopBlocks = new ArrayList<LoopBlock>();
		
		//create a list of LoopBlocks. each LoopBlock represents blocks that contain a particular loop
		//loopBlocks are in ordered manner. First loops come before later loops, inner loops though come before outer loops
		for(CFGBlock block : this.cfgBlocks){
			for(LIRStatement stmt: block.getStatements()){
				if(stmt.getClass().equals(LabelStmt.class) && stmt.toString().contains("for") && stmt.toString().contains("init")){
					int startBlockID, endBlockID = 0;
					startBlockID = block.getIndex();
					//get index of a block with the end of for loop
					for(CFGBlock succBlock : block.getSuccessors().get(0).getSuccessors()) //successor of a successor	
					{
						if(endBlockID < succBlock.getIndex()){
							endBlockID = succBlock.getIndex();
						}
					}
					LoopBlock loopBlock = new LoopBlock(startBlockID, endBlockID);
					
					//insert in the order of loops i.e. inner loop goes before outer loop
					if(this.loopBlocks.size() == 0){
						this.loopBlocks.add(loopBlock);
					}else{
						
						for(int i = 0; i < this.loopBlocks.size(); i++){
							if(this.loopBlocks.get(i).getEndBlockID() > endBlockID){
								this.loopBlocks.add(i, loopBlock);
								break;
							}else if(i == (this.loopBlocks.size()-1)){
								this.loopBlocks.add(loopBlock);
								break;
							}
						}
					}
					
				}
			}
		}
		
		
		for(LoopBlock loopBlock : this.loopBlocks){
			optimizeLoopBlock(loopBlock, methodName);
		}
		
		
	}
	
	private void optimizeLoopBlock(LoopBlock loopBlock, String methodName){
		int startBlockID, endBlockID; //start and end block index of the for loop
		BlockDataFlowState bdfState;  //block data flow state from reaching defs
		BitSet endBlockBitSet, initBlockBitSet;  //blocksets for reaching defs and for livevars
		QuadrupletStmt qStmtPotential;  
		HashMap<Name, QuadrupletStmt> nameToQStmt = new HashMap<Name, QuadrupletStmt>();
		List<CFGBlock> loopCFGBlocks = new ArrayList<CFGBlock>();
		List<LIRStatement> newQStmts = new ArrayList<LIRStatement>();
		List<LIRStatement> movedQStmts = new ArrayList<LIRStatement>();
		List<LIRStatement> newBlockStmts;
		HashMap<Integer, QuadrupletStmt> intToQStmt;
		HashMap<Name, Integer> nameToDefinedTimes; //number of tiems a name was defined in a loop

		
		
		startBlockID = loopBlock.getStartBlockID()+1; 
		endBlockID = loopBlock.getEndBlockID() -1 ;
		loopCFGBlocks = cfgBlocks.subList(startBlockID, endBlockID+1); 

		
		
		//find reaching definitions
		System.out.println("start block: " + startBlockID + " end block: "+ endBlockID);
		this.reachingDefinitionsGenerator.setCfgBlocks(loopCFGBlocks);
		this.reachingDefinitionsGenerator.setLoopBlock(loopBlock);
		this.reachingDefinitionsGenerator.generate();
		this.blockReachingDefs = reachingDefinitionsGenerator.getBlockReachingDefs();
		intToQStmt = this.reachingDefinitionsGenerator.getIntToQStmt();
		bdfState = this.blockReachingDefs.get(this.mMap.get(methodName).getCfgBlocks().get(endBlockID));
		endBlockBitSet = bdfState.getOut();
		

		//go through BitSet of the endBlock and check which QStmts reached till that point
		for(int i=0; i < endBlockBitSet.length(); i++){
			if(endBlockBitSet.get(i)){
				//add olny qStmts that were defined only once!
				
				qStmtPotential = intToQStmt.get(i);
				if(!nameToQStmt.containsKey(qStmtPotential.getDestination())){
					nameToQStmt.put(qStmtPotential.getDestination(), qStmtPotential);
				}
			}
		}
		
		//definedNames = getDefinedNames(loopCFGBlocks); //get the set of defined names
		nameToDefinedTimes = getDefinedNamesTimes(loopCFGBlocks);
		
		Name dest = null, arg1 = null, arg2 = null;
		QuadrupletStmt qStmt;
		
		//go through the shit and do it
		initBlockBitSet = blockLiveVars.get(this.mMap.get(methodName).getCfgBlocks().get(startBlockID-1)).getOut();
		HashSet<Name> liveVars = new HashSet<Name>();
		for(int i = 0; i < initBlockBitSet.length(); i++){
			if(initBlockBitSet.get(i)){
				System.out.println(initBlockBitSet.get(i));
				System.out.println("VAR: " + intToVar.get(i).getVar() + " ADDED");
				liveVars.add(intToVar.get(i).getVar());
			}
		}
		

		for(int i = startBlockID; i <= endBlockID; i ++){
			CFGBlock cfgBlock = this.mMap.get(methodName).getCfgBlocks().get(i);
			newBlockStmts = new ArrayList<LIRStatement>();
			for(LIRStatement stmt : cfgBlock.getStatements()){
				if(stmt.getClass().equals(QuadrupletStmt.class)){
					qStmt = (QuadrupletStmt)stmt;
					dest = qStmt.getDestination();
					arg1 = qStmt.getArg1();
					arg2 = qStmt.getArg2();
					if((arg1 != null && nameToDefinedTimes.containsKey(arg1)) ||(arg2 != null && nameToDefinedTimes.containsKey(arg2)) || !nameToQStmt.containsKey(dest) 
								|| dest.getClass().equals(RegisterName.class)){
						newBlockStmts.add(stmt);
					}
					else if(liveVars.contains(dest)){
					/*	if(nameToDefinedTimes.get(dest) != 1)
							newBlockStmts.add(stmt);
						else{
							//TODO: get this to work
							newQStmts.add(stmt);
						}*/
						newBlockStmts.add(stmt);
					}else{
						if(nameToDefinedTimes.get(dest) != 1)
							newBlockStmts.add(stmt);
						else{
							movedQStmts.add(stmt);
						}
					}
				}else{
					newBlockStmts.add(stmt);
				}
			}
			cfgBlock.setStatements(newBlockStmts);
		}
		//TODO: add the live vars later
		CFGBlock cfgBlock = cfgBlocks.get(startBlockID-1);
		
		for(LIRStatement stmt : movedQStmts){
			System.out.println("MOVED STATEMENT: " + stmt.toString());
			cfgBlock.addStatement(stmt);
		}
		
		cfgBlocks.add(startBlockID-1, cfgBlock);
		cfgBlocks.remove(startBlockID);
		this.mMap.get(methodName).setCfgBlocks(cfgBlocks);
		
	}

	
	private HashMap<Name, Integer> getDefinedNamesTimes(List<CFGBlock> forLoopBlocks){
		HashMap<Name, Integer> namesToNumTimes = new HashMap<Name, Integer>();
		for(CFGBlock block: forLoopBlocks){
			List<LIRStatement> blockStmts = block.getStatements();
			for (int i = 0; i < blockStmts.size(); i++) {
				LIRStatement stmt = blockStmts.get(i);
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					if(qStmt.getDestination() != null)
						if(!namesToNumTimes.containsKey(qStmt.getDestination()))
							namesToNumTimes.put(qStmt.getDestination(), 1);
						else
							namesToNumTimes.put(qStmt.getDestination(),namesToNumTimes.get(qStmt.getDestination()) +1);
				}
			}

		}
		return namesToNumTimes;
	}

	public void setReachingDefinitions(BlockModifiedReachingDefinitionGenerator reachingDefinitions) {
		this.reachingDefinitionsGenerator = reachingDefinitions;
	}

	public BlockModifiedReachingDefinitionGenerator getReachingDefinitions() {
		return reachingDefinitionsGenerator;
	}
	
	

}
