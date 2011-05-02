package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.StoreStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class ExplicitStoreAdder {
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	private HashMap<String, MethodIR> mMap;
	private ReachingStoresAnalyzer storeAnalyzer;
	private HashSet<StoreStmt> uniqueStores;
	private HashSet<Name> storesInBlock;
	private HashMap<CFGBlock, String> blockState;
	private boolean seenCall;
	
	public ExplicitStoreAdder(HashMap<String, MethodIR> mMap) {
		this.storeAnalyzer = new ReachingStoresAnalyzer(mMap);
		this.mMap = mMap;
		this.uniqueStores = new HashSet<StoreStmt>();
		this.storesInBlock = new HashSet<Name>();
		this.seenCall = false;
		this.blockState = new HashMap<CFGBlock, String>();
	}
	
	public ReachingStoresAnalyzer getReachingStoreAnalyzer() {
		return storeAnalyzer;
	}
	
	public void execute() {
		this.storeAnalyzer.analyze();
		
		for (String methodName : this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			insertNaiveStores(methodName);
			
			optimizeMethod(methodName);
			
			this.mMap.get(methodName).regenerateStmts();
		}
	}

	private void insertNaiveStores(String methodName) {
		QuadrupletStmt.setID(0);
		
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			for (int i = 0; i < block.getStatements().size(); i++) {
				LIRStatement stmt = block.getStatements().get(i);
				
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
					if (qStmt.getDestination().isGlobal()) {
						StoreStmt sStmt = new StoreStmt(qStmt.getDestination());
						sStmt.setMyId();
						block.getStatements().add(i+1, sStmt);
					}
				}
			}
		}
		
		this.storeAnalyzer.analyze();
	}

	private void optimizeMethod(String methodName) {
		this.blockState.clear();

		while (true) {
			for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
				this.blockState.put(block, block.toString());
			}
			
			getStoreStmts(methodName);
			optimizeStores(methodName);

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
		}
	}

	private void optimizeStores(String methodName) {	
		for (StoreStmt sStmt: this.uniqueStores) {
			optimizeStore(methodName, sStmt);
		}
	}

	private void optimizeStore(String methodName, StoreStmt sStmt) {
		List<LIRStatement> prevStmts;
		List<LIRStatement> nextStmts;
		
		CFGBlock succ = getBlockForStmt(sStmt, methodName);
		int depth = sStmt.getDepth();
		boolean isOptimized = false;
		
		while (!isOptimized) {
			CFGBlock next = getSuccessorForEndBlock(succ, depth);
			prevStmts = copyStmts(succ);
			nextStmts = copyStmts(next);
			
			removeStore(succ, sStmt);
			insertStore(next, sStmt);
			
			this.mMap.get(methodName).regenerateStmts();
			
			storeAnalyzer.analyze();
			
			if (isGlobalDefStateConsistent(methodName)) {
				depth--;
				sStmt.setDepth(depth);
				succ = next;
			}
			else {
				succ.setStatements(prevStmts);
				next.setStatements(nextStmts);
				isOptimized = true;
			}
			
			if (depth == 0) break; // Can't optimize more
		}
		
		this.mMap.get(methodName).regenerateStmts();
	}

	private boolean isGlobalDefStateConsistent(String methodName) {
		for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
			if (!checkBlock(block)) { // Returns false if check failed
				return false;
			}
		}
		
		return true;
	}

	private boolean checkBlock(CFGBlock block) {
		this.storesInBlock.clear();
		this.seenCall = false;
		
		for (int i = block.getStatements().size()-1; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(StoreStmt.class)) {
				this.storesInBlock.add(((StoreStmt)stmt).getVariable());
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				if (qStmt.getDestination().isGlobal()) {
					checkName(qStmt.getDestination(), block);
				}
				
				killStores(qStmt, block);				
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				if (((CallStmt)stmt).getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				this.storesInBlock.clear();
				this.seenCall = true;
			}
		}

		return true;
	}
	
	private boolean checkName(Name name, CFGBlock block) {
		if (name.getClass().equals(VarName.class)) {
			VarName var = (VarName)name;
			if (var.isString()) return true;
		}

		if (this.storesInBlock.contains(name)) return true;
		
		if (this.seenCall) {
			return false; // must be in globalsInBlock map
		}

		List<Name> uniqueStores = this.storeAnalyzer.getStores().get(block.getMethodName());
		int i = uniqueStores.indexOf(name);
		
//		int succCount = 0;
//		for (CFGBlock b: block.getSuccessors()) {
//			BlockDataFlowState state = this.storeAnalyzer.getCfgBlocksState().get(b);
//			if (!state.getIn().get(i)) {
//				succCount++;
//			}
//		}
//
//		if (succCount != 0) {
//			return false;
//		}
		
		BlockDataFlowState state = this.storeAnalyzer.getCfgBlocksState().get(block);
		if (!state.getOut().get(i)) {
			return false;
		}

		return true;
	}
	
	private void killStores(QuadrupletStmt qStmt, CFGBlock block) {
		HashSet<Name> remove = new HashSet<Name>();
		for (Name name: this.storesInBlock) {
			if (name.equals(qStmt.getDestination())) continue; // One store can cover multiple assignments
			
			if (name.isArray()) {
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
					List<Name> stores = this.storeAnalyzer.getStores().get(block.getMethodName());
					int i = stores.indexOf(name);
					this.storeAnalyzer.getCfgBlocksState().get(block).getOut().set(i, false);
					remove.add(name);
				}
			}
		}
		
		this.storesInBlock.removeAll(remove);
	}

	private void insertStore(CFGBlock block, StoreStmt sStmt) {
		int index = -1;
		
		for (int i = block.getStatements().size()-1; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				if (lStmt.getLabelString().matches(ForEndLabelRegex)) {
					index = i;
					break;
				}
			}
		}
		
		block.getStatements().add(index+1, sStmt);	
	}

	private void removeStore(CFGBlock block, StoreStmt sStmt) {
		int index = -1;
		for (int i = 0; i < block.getStatements().size(); i++) {
			if (block.getStatements().get(i) == sStmt) {
				index = i;
				break;
			}
		}
		
		block.getStatements().remove(index);
	}

	private List<LIRStatement> copyStmts(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		newStmts.addAll(block.getStatements());
		return newStmts;
	}
	
	private CFGBlock getSuccessorForEndBlock(CFGBlock block, int depth) {
		CFGBlock successor = block;
		boolean found = false;
		
		while (!found) {
			successor = successor.getSuccessors().get(0);
			LIRStatement stmt = successor.getStatements().get(0);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				if (lStmt.getLabelString().matches(ForEndLabelRegex)) {
					found = true;
				}
			}
		}
		
		return successor;
	}

	private CFGBlock getBlockForStmt(StoreStmt sStmt, String methodName) {
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			for (LIRStatement stmt: block.getStatements()) {
				if (stmt == sStmt) return block;
			}
		}
		
		return null;
	}

	private void getStoreStmts(String methodName) {
		this.uniqueStores.clear();
		
		for (LIRStatement stmt: this.mMap.get(methodName).getStatements()) {
			if (stmt.getClass().equals(StoreStmt.class)) {
				StoreStmt sStmt = (StoreStmt) stmt;
				if (sStmt.getDepth() != 0) {
					this.uniqueStores.add(sStmt);
				}
			}
		}
	}
}
