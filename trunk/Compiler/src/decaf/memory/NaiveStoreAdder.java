package decaf.memory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.StoreStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class NaiveStoreAdder {
	private LiveGlobalStores lgs;
	private HashMap<String, MethodIR> mMap;
	private HashSet<Name> globalsSavedInBlock;
	private boolean seenCall;
	private HashMap<CFGBlock, String> blockState;

	public NaiveStoreAdder(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.lgs = new LiveGlobalStores(mMap);
		this.blockState = new HashMap<CFGBlock, String>();
		this.globalsSavedInBlock = new HashSet<Name>();
		this.seenCall = false;
	}

	public void addStores() {
		QuadrupletStmt.setID(0);
		
		this.lgs.analyze();

		for (String methodName : this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			processMethod(methodName);
			this.mMap.get(methodName).regenerateStmts();
		}
	}

	public void processMethod(String methodName) {
		this.blockState.clear();

		int i = 100;
		while (true) {
			for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
				this.blockState.put(block, block.toString());
			}
			
			for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
				if (processBlock(block)) { // Returns true if added some load stmt
					break;
				}
			}

			this.lgs.analyze();
			this.mMap.get(methodName).regenerateStmts();

			boolean isChanged = false;
			for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
				if (!block.toString().equals(this.blockState.get(block).toString())) {
					isChanged = true;
					break;
				}
			}

			if (!isChanged) {
				break;
			}
			i--;
			
		}
	}

	private boolean processBlock(CFGBlock block) {
		this.globalsSavedInBlock.clear();
		this.seenCall = false;
		
		for (int i = block.getStatements().size()-1; i >= 0 ; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(StoreStmt.class)) {
				this.globalsSavedInBlock.add(((StoreStmt)stmt).getVariable());
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {		
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				
				killLocalGlobals(qStmt);
				
				// Add store stmt if needed
				if (processName(qStmt.getDestination(), block, i)) {
					return true;
				}
				
				if (qStmt.getDestination().isGlobal()) {
					this.globalsSavedInBlock.add(qStmt.getDestination());
				}				
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				if (((CallStmt)stmt).getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				this.globalsSavedInBlock.clear();
				this.seenCall = true;
			}
		}

		return false;
	}

	private void killLocalGlobals(QuadrupletStmt qStmt) {
		HashSet<Name> remove = new HashSet<Name>();
		for (Name name: this.globalsSavedInBlock) {
			boolean resetName = false;
			
			if (name.isArray()) {
				Name myName = name;
				
				do {
					ArrayName array = (ArrayName) myName;
					if (array.getIndex().equals(qStmt.getDestination())) { // Index being reassigned, KILL!
						resetName = true;
					}
					
					myName = array.getIndex();
					
				} while (myName.isArray());
				
				if (qStmt.getDestination().isArray()) {
					ArrayName dest = (ArrayName) qStmt.getDestination();
					ArrayName arrName = (ArrayName) name;
					if (!arrName.getIndex().getClass().equals(ConstantName.class)) {
						if (arrName.getId().equals(dest.getId())) {
							resetName = true;
						}
					}
				}
			}
			
			if (resetName) {
				remove.add(name);
			}
		}
		
		this.globalsSavedInBlock.removeAll(remove);
	}

	private boolean processName(Name name, CFGBlock block, int index) {
		if (name.getClass().equals(VarName.class)) {
			VarName var = (VarName)name;
			if (var.isString()) return false;
		}
		
		if (this.globalsSavedInBlock.contains(name)) return false;
		
		List<Name> uniqueGlobals = this.lgs.getUniqueGlobals().get(block.getMethodName());
		int i = uniqueGlobals.indexOf(name);
		
		int succCount = 0;
		for (CFGBlock b: block.getSuccessors()) {
			BlockDataFlowState state = this.lgs.getCfgBlocksState().get(b);
			if (!state.getIn().get(i)) {
				succCount++;
			}
		}
		
		if (this.seenCall || 
				block.getPredecessors().size() == 0 ||
				succCount != 0) {
			StoreStmt store = new StoreStmt(name);
			store.setDepth(block.getStatements().get(index).getDepth()); // set depth
			store.setMyId();
			
			block.getStatements().add(index+1, store);
			
			this.globalsSavedInBlock.add(name);
			
			return true;
		}
		
		return false;
	}

	public void setGlobalDefAnalyzer(LiveGlobalStores lgs) {
		this.lgs = lgs;
	}

	public LiveGlobalStores getDf() {
		return lgs;
	}
}
