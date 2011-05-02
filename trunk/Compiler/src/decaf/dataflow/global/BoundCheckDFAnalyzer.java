package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.StoreStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class BoundCheckDFAnalyzer {
	public class BoundCheckDef {
		private Name index;
		
		public Name getIndex() {
			return index;
		}

		private String arrayId;
		
		public BoundCheckDef(String arrayId, Name index) {
			this.arrayId = arrayId;
			this.index = index;
		}
		
		public BoundCheckDef(ArrayName name) {
			this.arrayId = name.getId();
			this.index = name.getIndex();
		}
		
		@Override
		public int hashCode() {
			return this.index.toString().hashCode() + 13 * this.index.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null) return false;
			if (!o.getClass().equals(BoundCheckDef.class)) return false;
			
			BoundCheckDef def = (BoundCheckDef)o;
			if (!this.arrayId.equals(def.arrayId)) return false;
			if (!this.index.equals(def.index)) return false;
			
			return true;			
		}
		
		@Override
		public String toString() {
			return this.arrayId + "[" + this.index + "]";
		}
	}
	
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState;
	private HashMap<String, MethodIR> mMap;
	private HashMap<String, List<BoundCheckDef>> indicesMap;
	
	public BoundCheckDFAnalyzer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.cfgBlocksToProcess = new HashSet<CFGBlock>();
		this.cfgBlocksState = new HashMap<CFGBlock, BlockDataFlowState>();
		this.indicesMap = new HashMap<String, List<BoundCheckDef>>();
	}
	
	public void analyze() {
		this.cfgBlocksState.clear();
		this.indicesMap.clear();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			initialize(methodName);
			runWorkList(methodName);
		}
	}

	private void runWorkList(String methodName) {
		int totalGlobals = this.indicesMap.get(methodName).size();
		
		CFGBlock entry = this.getBlockById(methodName, 0);
		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(totalGlobals); // OUT = GEN for entry block
		calculateGenKillSets(entry, entryBlockFlow);
		entryBlockFlow.setOut(entryBlockFlow.getGen());
		cfgBlocksToProcess.remove(entry);
		
		this.cfgBlocksState.put(entry, entryBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockDataFlowState bFlow = generateDFState(block);
			this.cfgBlocksState.put(block, bFlow);
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

	private void initialize(String methodName) {
		this.cfgBlocksToProcess.clear();
		HashSet<BoundCheckDef> temp = new HashSet<BoundCheckDef>();
		
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			List<LIRStatement> blockStmts = block.getStatements();
			for (int i = 0; i < blockStmts.size(); i++) {
				LIRStatement stmt = blockStmts.get(i);
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					
					if (qStmt.getDestination().isArray()) {
						ArrayName arrName = (ArrayName) qStmt.getDestination();
						temp.add(new BoundCheckDef(arrName));
					}
					if (qStmt.getArg1().isArray()) {
						ArrayName arrName = (ArrayName) qStmt.getArg1();
						temp.add(new BoundCheckDef(arrName));
					}
					if (qStmt.getArg2() != null && qStmt.getArg2().isArray()) {
						ArrayName arrName = (ArrayName) qStmt.getArg2();
						temp.add(new BoundCheckDef(arrName));
					}
				}
				else if (stmt.getClass().equals(CmpStmt.class)) {
					CmpStmt cStmt = (CmpStmt) stmt;
					if (cStmt.getArg1().isArray()) {
						ArrayName arrName = (ArrayName) cStmt.getArg1();
						temp.add(new BoundCheckDef(arrName));
					}
					if (cStmt.getArg2().isArray()) {
						ArrayName arrName = (ArrayName) cStmt.getArg2();
						temp.add(new BoundCheckDef(arrName));
					}
				}
				else if (stmt.getClass().equals(PushStmt.class)) {
					PushStmt pStmt = (PushStmt) stmt;
					
					if (pStmt.getName().isArray()) {
						ArrayName arrName = (ArrayName) pStmt.getName();
						temp.add(new BoundCheckDef(arrName));
					}
				}
				else if (stmt.getClass().equals(PopStmt.class)) {
					PopStmt pStmt = (PopStmt) stmt;
					
					if (pStmt.getName().isArray()) {
						ArrayName arrName = (ArrayName) pStmt.getName();
						temp.add(new BoundCheckDef(arrName));
					}
				}
				else if (stmt.getClass().equals(LoadStmt.class)) {
					LoadStmt lStmt = (LoadStmt) stmt;
					
					if (lStmt.getVariable().isArray()) {
						ArrayName arrName = (ArrayName) lStmt.getVariable();
						temp.add(new BoundCheckDef(arrName));
					}
				}
				else if (stmt.getClass().equals(StoreStmt.class)) {
					StoreStmt sStmt = (StoreStmt) stmt;
					
					if (sStmt.getVariable().isArray()) {
						ArrayName arrName = (ArrayName) sStmt.getVariable();
						temp.add(new BoundCheckDef(arrName));
					}
				}
			}
			
			this.cfgBlocksToProcess.add(block);
		}
		
		this.indicesMap.put(methodName, new ArrayList<BoundCheckDef>());
		this.indicesMap.get(methodName).addAll(temp);
	}
	
	private BlockDataFlowState generateDFState(CFGBlock block) {
		int totalGlobals = this.indicesMap.get(block.getMethodName()).size();
		// Get the original out BitSet for this block
		BitSet origOut;
		if (this.cfgBlocksState.containsKey(block)) {
			origOut = this.cfgBlocksState.get(block).getOut();
		} else {
			origOut = new BitSet(totalGlobals);
			origOut.set(0, totalGlobals); // Confluence operator is AND
		}
		
		// Calculate the in BitSet by taking intersection of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(totalGlobals);
		
		// If there exists at least one predecessor, set In to all True
		if (block.getPredecessors().size() > 0) {
			bFlow.getIn().set(0, totalGlobals);
		}
		
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (this.cfgBlocksState.containsKey(pred)) {
				in.and(this.cfgBlocksState.get(pred).getOut());
			}
		}
		
		calculateGenKillSets(block, bFlow);
		
		// Calculate Out
		BitSet out = bFlow.getOut(); // OUT = (IN - KILL) U GEN
		out.or(in);
		out.xor(bFlow.getKill());
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
	
	private void calculateGenKillSets(CFGBlock block, BlockDataFlowState bFlow) {
		List<LIRStatement> blockStmts = block.getStatements();
		QuadrupletStmt qStmt;
		
		boolean bcAdded = false;
		String currentArrayBC = null;
		
		for (LIRStatement stmt : blockStmts) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				if (lStmt.getLabelString().matches(BoundCheckDFAnalyzer.ArrayBeginLabelRegex)) {
					currentArrayBC = getArrayIDFromArrayLabelStmt(lStmt, "begin");
					bcAdded = false;
				}
				else if (lStmt.getLabelString().matches(BoundCheckDFAnalyzer.ArrayPassLabelRegex)) {
					currentArrayBC = null;
				}
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				if (currentArrayBC != null && !bcAdded) { // in bc block and bc not added to map
					BoundCheckDef bcDef = new BoundCheckDef(currentArrayBC, cStmt.getArg1());
					int index = this.indicesMap.get(block.getMethodName()).indexOf(bcDef);
					if (index >= 0) { // In case only BC left, will DC it
						bFlow.getGen().set(index);
						bcAdded = true;
					}
				}
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				if (!qStmt.getDestination().getClass().equals(RegisterName.class)) {
					updateKillGenSet(block.getMethodName(), qStmt.getDestination(), bFlow, false);
				}
			}
			else if (stmt.getClass().equals(LoadStmt.class)) {
				updateKillGenSet(block.getMethodName(), ((LoadStmt)stmt).getVariable(), bFlow, true);
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				updateKillGenSet(block.getMethodName(), (CallStmt)stmt, bFlow);
			}
		}
	}
	
	private void updateKillGenSet(String methodName, CallStmt stmt, BlockDataFlowState bFlow) {
		if (stmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) return;
		
		// Kill bc for globals
		for (int i = 0; i < this.indicesMap.get(methodName).size(); i++) {
			BoundCheckDef bcDef = this.indicesMap.get(methodName).get(i);
			if (bcDef.getIndex().isGlobal()) {
				if (bFlow.getIn().get(i)) {
					bFlow.getKill().set(i);
				}
				
				bFlow.getGen().clear(i);
			}
		}
	}

	private void updateKillGenSet(String methodName, Name var, BlockDataFlowState bFlow, boolean isLoad) {		
		// Kill bc for all *dependent* newly defined var
		for (int i = 0; i < this.indicesMap.get(methodName).size(); i++) {
			Name index = this.indicesMap.get(methodName).get(i).getIndex();
			
			boolean resetName = false;
			
			do {
				if (index.equals(var)) { // Index being reassigned, KILL!
					resetName = true;
				}
				
				if (index.isArray()) {
					index = ((ArrayName)index).getIndex();
				}
				
			} while (index.isArray());
			
			if (!isLoad) { // QuadrupletStmt definition
				if (var.isArray() && index.isArray()) {
					ArrayName dest = (ArrayName) var;
					ArrayName myIndex = (ArrayName) index;
					if (myIndex.getId().equals(dest.getId())) {
							resetName = true;
					}
				}
			}

			if (resetName) {
				if (bFlow.getIn().get(i)) {
					bFlow.getKill().set(i);
				}
				
				bFlow.getGen().clear(i);
			}
		}
	}

	public HashMap<CFGBlock, BlockDataFlowState> getCfgBlocksState() {
		return cfgBlocksState;
	}

	public void setCfgBlocksState(
			HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState) {
		this.cfgBlocksState = cfgBlocksState;
	}

	public HashMap<String, List<BoundCheckDef>> getUniqueIndices() {
		return indicesMap;
	}
	
	private String getArrayIDFromArrayLabelStmt(LabelStmt stmt, String end) {
		String name = stmt.getLabelString();
      int i = name.indexOf(".array.");
      name = name.substring(i + 7);
      
      name = name.substring(0, name.length() - end.length() - 1);
      i = name.indexOf(".");
      name = name.substring(0, i);
      
      return name;  
	}
}
