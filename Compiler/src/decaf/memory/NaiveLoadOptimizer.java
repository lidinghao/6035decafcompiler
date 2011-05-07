package decaf.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class NaiveLoadOptimizer {
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
	private static String ForTestLabelRegex = "[a-zA-z_]\\w*.for\\d+.test";
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private HashMap<String, MethodIR> mMap;
	private ReachingGlobalDefinitions df;
	private HashSet<LoadStmt> loadGlobals;
	private HashSet<LoadStmt> loadArrays;
	private HashSet<Name> globalsInBlock;
	private HashMap<CFGBlock, String> blockState;
	private boolean seenCall;
	
	public NaiveLoadOptimizer(HashMap<String, MethodIR> mMap) {
		this.df = new ReachingGlobalDefinitions(mMap);
		this.mMap = mMap;
		this.loadGlobals = new HashSet<LoadStmt>();
		this.loadArrays = new HashSet<LoadStmt>();
		this.globalsInBlock = new HashSet<Name>();
		this.seenCall = false;
		this.blockState = new HashMap<CFGBlock, String>();
	}
	
	public ReachingGlobalDefinitions getDf() {
		return df;
	}

	public void setDf(ReachingGlobalDefinitions df) {
		this.df = df;
	}

	public void optimizeLoads() {
		this.df.analyze();
		
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
			
			getLoadStmts(methodName);
			optimizeLoads(methodName);

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
		
		addBoundChecksToLoads(methodName);
	}

	private void addBoundChecksToLoads(String methodName) {
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			for (int i = 0; i < block.getStatements().size(); i++) {
				LIRStatement stmt = block.getStatements().get(i);
				
				if (stmt.getClass().equals(LoadStmt.class)) { // Found load
					LoadStmt load = (LoadStmt) stmt;
					if (!load.getVariable().isArray()) continue;
					
					boolean addBC = true;
					LIRStatement prev = block.getStatements().get(i - 1);
					if (prev.getClass().equals(LabelStmt.class)) {
						LabelStmt lStmt = (LabelStmt) prev;
						if (lStmt.getLabelString().matches(NaiveLoadOptimizer.ArrayPassLabelRegex)) {
							addBC = false;
						}
					}
					
					if (addBC) {
						block.getStatements().addAll(i, load.getBoundCheck());
					}
				}
			}
		}
	}

	private void optimizeLoads(String methodName) {	
		for (LoadStmt lStmt: this.loadGlobals) {
			optimizeLoad(methodName, lStmt);
		}
		
		for (LoadStmt lStmt: this.loadArrays) {
			optimizeLoad(methodName, lStmt);
		}
	}

	private void optimizeLoad(String methodName, LoadStmt lStmt) {
		List<LIRStatement> prevStmts;
		List<LIRStatement> nextStmts;
		
		CFGBlock prev = getBlockForStmt(lStmt, methodName);
		int depth = lStmt.getDepth();
		boolean isOptimized = false;
		
		while (!isOptimized) {
			CFGBlock next = getParentForInitBlock(prev, depth);
			prevStmts = copyStmts(prev);
			nextStmts = copyStmts(next);
			
			removeLoad(prev, lStmt);
			insertLoad(next, lStmt);
			
			this.mMap.get(methodName).regenerateStmts();
			
			df.analyze();
			
			if (isGlobalDefStateConsistent(methodName)) {
				depth = next.getStatements().get(0).getDepth();
				lStmt.setDepth(depth);
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
		
		for (LIRStatement stmt: block.getStatements()) {			
			if (stmt.getClass().equals(LoadStmt.class)) {
				this.globalsInBlock.add(((LoadStmt)stmt).getVariable());
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;

				if (qStmt.getArg1().isGlobal()) {
					if (!checkName(qStmt.getArg1(), block)) {
						return false;
					}
				}
				if (qStmt.getArg2() != null && qStmt.getArg2().isGlobal()) {
					if (!checkName(qStmt.getArg2(), block)) {
						return false;
					}
				}
				
				killLocalGlobals(qStmt, block);
				
				if (qStmt.getDestination().isGlobal()) {
					this.globalsInBlock.add(qStmt.getDestination());
				}				
			} 
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				if (cStmt.getArg1().isGlobal()) {
					if (!checkName(cStmt.getArg1(), block))
						return false;
				}
				if (cStmt.getArg2().isGlobal()) {
					if (!checkName(cStmt.getArg2(), block))
						return false;
				}
			} 
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;

				if (pStmt.getName().isGlobal()) {
					if (!checkName(pStmt.getName(), block))
						return false;
				}
			} 
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;

				if (pStmt.getName().isGlobal()) {
					if (!checkName(pStmt.getName(), block))
						return false;
				}
			}
			else if (stmt.getClass().equals(LoadStmt.class)) {
				this.globalsInBlock.add(((LoadStmt)stmt).getVariable());
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

		List<Name> uniqueGlobals = this.df.getUniqueGlobals().get(block.getMethodName());
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
		
		BlockDataFlowState state = this.df.getCfgBlocksState().get(block);
		if (!state.getIn().get(i)) {
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
						if (!arrName.getIndex().getClass().equals(ConstantName.class)) {
							if (arrName.getId().equals(dest.getId())) {
								resetName = true;
							}
						}
					}
				}
				
				if (resetName) {
					List<Name> uniqueGlobals = this.df.getUniqueGlobals().get(block.getMethodName());
					int i = uniqueGlobals.indexOf(name);
					this.df.getCfgBlocksState().get(block).getIn().set(i, false);
					remove.add(name);
				}
			}
		}
		
		this.globalsInBlock.removeAll(remove);
	}

	private void insertLoad(CFGBlock block, LoadStmt loadStmt) {
		int index = -1;
		
		for (int i = block.getStatements().size()-1; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				if (lStmt.getLabelString().matches(NaiveLoadOptimizer.ForInitLabelRegex)) {
					index = i;
					break;
				}
			}
		}
		
		block.getStatements().add(index, loadStmt);	
	}

	private void removeLoad(CFGBlock prev, LoadStmt lStmt) {
		int index = -1;
		for (int i = 0; i < prev.getStatements().size(); i++) {
			if (prev.getStatements().get(i) == lStmt) {
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
	
	private CFGBlock getParentForInitBlock(CFGBlock block, int depth) {
		CFGBlock parent = block;
		boolean found = false;
		
		while (!found) {
			parent = parent.getPredecessors().get(0);
			LIRStatement stmt = parent.getStatements().get(0);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				if (lStmt.getLabelString().matches(NaiveLoadOptimizer.ForTestLabelRegex)) {
					parent = parent.getPredecessors().get(0);
					found = true;
				}
			}
		}
		
		return parent;
	}

	private CFGBlock getBlockForStmt(LoadStmt lStmt, String methodName) {
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			for (LIRStatement stmt: block.getStatements()) {
				if (stmt == lStmt) return block;
			}
		}
		
		return null;
	}

	private void getLoadStmts(String methodName) {
		this.loadArrays.clear();
		this.loadGlobals.clear();
		
		for (LIRStatement stmt: this.mMap.get(methodName).getStatements()) {
			if (stmt.getClass().equals(LoadStmt.class)) {
				LoadStmt lStmt = (LoadStmt) stmt;
				if (lStmt.getDepth() != 0) {
					if (lStmt.getVariable().isArray()) {
						this.loadArrays.add(lStmt);
					}
					else {
						this.loadGlobals.add(lStmt);
					}
				}
			}
		}
	}
}
