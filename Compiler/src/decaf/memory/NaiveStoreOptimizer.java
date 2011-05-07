package decaf.memory;

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

public class NaiveStoreOptimizer {
	private static String ForTestLabelRegex = "[a-zA-z_]\\w*.for\\d+.test";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	private HashMap<String, MethodIR> mMap;
	private LiveGlobalStores lgs;
	private HashSet<StoreStmt> storeGlobals;
	private HashSet<StoreStmt> storeArrays;
	private HashSet<Name> globalsInBlock;
	private HashMap<CFGBlock, String> blockState;
	private boolean seenCall;
	
	public NaiveStoreOptimizer(HashMap<String, MethodIR> mMap) {
		this.lgs = new LiveGlobalStores(mMap);
		this.mMap = mMap;
		this.storeGlobals = new HashSet<StoreStmt>();
		this.storeArrays = new HashSet<StoreStmt>();
		this.globalsInBlock = new HashSet<Name>();
		this.seenCall = false;
		this.blockState = new HashMap<CFGBlock, String>();
	}
	
	public LiveGlobalStores getLGS() {
		return lgs;
	}

	public void setLGS(LiveGlobalStores df) {
		this.lgs = df;
	}

	public void optimizeStores() {
		this.lgs.analyze();
		
		for (String methodName : this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			optimizeMethod(methodName);
			
			this.mMap.get(methodName).regenerateStmts();
		}
	}

	private void optimizeMethod(String methodName) {
		this.blockState.clear();

		while (true) {
			for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
				this.blockState.put(block, block.toString());
			}
			
			getStoreStmts(methodName);
			optimizeStore(methodName);

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
		
		// Dead code loads
		LoadsDC dc = new LoadsDC(this.mMap);
		dc.removeDeadLoads();
	}

	private void optimizeStore(String methodName) {	
		for (StoreStmt sStmt: this.storeGlobals) {
			optimizeStore(methodName, sStmt);
		}
		
		for (StoreStmt lStmt: this.storeArrays) {
			optimizeStore(methodName, lStmt);
		}
	}

	private void optimizeStore(String methodName, StoreStmt sStmt) {
		List<LIRStatement> prevStmts;
		List<LIRStatement> nextStmts;
		
		CFGBlock prev = getBlockForStmt(sStmt, methodName);
		int depth = sStmt.getDepth();
		boolean isOptimized = false;
		
		while (!isOptimized) {
			CFGBlock next = getChildForEndBlock(prev, depth);
			prevStmts = copyStmts(prev);
			nextStmts = copyStmts(next);
			
			removeStore(prev, sStmt);
			insertStore(next, sStmt);
			
			this.mMap.get(methodName).regenerateStmts();
			
			lgs.analyze();
			
//			System.out.println("\n\nTRY: " + sStmt);
//			for (LIRStatement stmt: this.mMap.get(methodName).getStatements()) {
//				System.out.println(stmt);
//			}
//			System.out.println("END TRY: " + sStmt + "\n\n");
			
			if (isGlobalDefStateConsistent(methodName)) {
				depth = next.getStatements().get(0).getDepth();
				sStmt.setDepth(depth);
				prev = next;
			}
			else {
				prev.setStatements(prevStmts);
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
		this.globalsInBlock.clear();
		this.seenCall = false;
		
		for (int i = block.getStatements().size()-1; i >= 0; i--) {	
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(StoreStmt.class)) {
				this.globalsInBlock.add(((StoreStmt)stmt).getVariable());
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				
				if (qStmt.getDestination().isGlobal()) {
					if (!checkName(qStmt.getDestination(), block)) {
						return false;
					}
					
					this.globalsInBlock.add(qStmt.getDestination());
				}
				
				killLocalGlobals(qStmt, block);
			} 
			else if (stmt.getClass().equals(CallStmt.class)) {
				if (((CallStmt)stmt).getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				this.globalsInBlock.clear();
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

		if (this.globalsInBlock.contains(name)) return true;
		
		if (this.seenCall) {
			return false; // must be in globalsInBlock map
		}

		List<Name> uniqueGlobals = this.lgs.getUniqueGlobals().get(block.getMethodName());
		int i = uniqueGlobals.indexOf(name);
//		
//		int predCount = 0;
//		for (CFGBlock b: block.getPredecessors()) {
//			BlockDataFlowState state = this.df.getCfgBlocksState().get(b);
//			if (!state.getOut().get(i)) {
//				predCount++;
//			}
//		}
//
//		if (predCount != 0) {
//			return false;
//		}
		
		BlockDataFlowState state = this.lgs.getCfgBlocksState().get(block);
		if (!state.getOut().get(i)) {
			return false;
		}

		return true;
	}
	
	private void killLocalGlobals(QuadrupletStmt qStmt, CFGBlock block) {
		HashSet<Name> remove = new HashSet<Name>();
		for (Name name: this.globalsInBlock) {
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
						if (dest.getIndex().getClass().equals(ConstantName.class) &&
								!arrName.getIndex().getClass().equals(ConstantName.class)) {
							if (arrName.getId().equals(dest.getId())) {
								resetName = true;
							}
						}
					}
				}
				
				if (resetName) {
					List<Name> uniqueGlobals = this.lgs.getUniqueGlobals().get(block.getMethodName());
					int i = uniqueGlobals.indexOf(name);
					this.lgs.getCfgBlocksState().get(block).getOut().set(i, false);
					remove.add(name);
				}
			}
		}
		
		this.globalsInBlock.removeAll(remove);
	}

	private void insertStore(CFGBlock block, StoreStmt sStmt) {
		int index = -1;
		
		for (int i = block.getStatements().size()-1; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				if (lStmt.getLabelString().matches(ForEndLabelRegex)) {
					index = i+1;
					break;
				}
			}
		}
		
		block.getStatements().add(index, sStmt);	
	}

	private void removeStore(CFGBlock prev, StoreStmt sStmt) {
		int index = -1;
		for (int i = 0; i < prev.getStatements().size(); i++) {
			if (prev.getStatements().get(i) == sStmt) {
				index = i;
				break;
			}
		}
		
		prev.getStatements().remove(index);
	}

	private List<LIRStatement> copyStmts(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		newStmts.addAll(block.getStatements());
		return newStmts;
	}
	
	private CFGBlock getChildForEndBlock(CFGBlock block, int depth) {
		CFGBlock succ = block;
		boolean found = false;
		
		while (!found) {
			succ = succ.getPredecessors().get(0);
			LIRStatement stmt = succ.getStatements().get(0);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				if (lStmt.getLabelString().matches(ForTestLabelRegex)) {
					succ = succ.getSuccessors().get(1);
					found = true;
				}
			}
		}
		
		return succ;
	}

	private CFGBlock getBlockForStmt(StoreStmt lStmt, String methodName) {
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			for (LIRStatement stmt: block.getStatements()) {
				if (stmt == lStmt) return block;
			}
		}
		
		return null;
	}

	private void getStoreStmts(String methodName) {
		this.storeArrays.clear();
		this.storeGlobals.clear();
		
		for (LIRStatement stmt: this.mMap.get(methodName).getStatements()) {
			if (stmt.getClass().equals(StoreStmt.class)) {
				StoreStmt sStmt = (StoreStmt) stmt;
				if (sStmt.getDepth() != 0) {
					if (sStmt.getVariable().isArray()) {
						this.storeArrays.add(sStmt);
					}
					else {
						this.storeGlobals.add(sStmt);
					}
				}
			}
		}
	}
}
