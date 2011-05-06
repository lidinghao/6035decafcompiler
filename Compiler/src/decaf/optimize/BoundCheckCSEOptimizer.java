package decaf.optimize;

import java.util.ArrayList;
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
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;
import decaf.optimize.BoundCheckDFAnalyzer.BoundCheckDef;

public class BoundCheckCSEOptimizer {
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
	private BoundCheckDFAnalyzer bc;
	private HashSet<BoundCheckDef> bcInBlock;
	private HashMap<String, MethodIR> mMap;
	
	public BoundCheckCSEOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.bc = new BoundCheckDFAnalyzer(mMap);
		this.bcInBlock = new HashSet<BoundCheckDef>();
	}
	
	public void performBoundCheckCSE() {
		this.bc.analyze();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			optimize(methodName);
			
			this.mMap.get(methodName).regenerateStmts();
		}
	}

	public BoundCheckDFAnalyzer getBCAnalyzer() {
		return bc;
	}

	public void setBCAnalyzer(BoundCheckDFAnalyzer bc) {
		this.bc = bc;
	}

	private void optimize(String methodName) {
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			optimize(block);
		}
	}

	private void optimize(CFGBlock block) {
		this.bcInBlock.clear();
		boolean addStmt = true;
		
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (int i = 0; i < block.getStatements().size(); i++) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				if (lStmt.getLabelString().matches(ArrayBeginLabelRegex)) {
					// Peek to check if already cmp done
					CmpStmt cStmt = (CmpStmt) block.getStatements().get(i + 1);
					BoundCheckDef bcDef = this.bc.new BoundCheckDef(getArrayIDFromArrayLabelStmt(lStmt, "begin"), cStmt.getArg1());
					int index = this.bc.getUniqueIndices().get(block.getMethodName()).indexOf(bcDef);
					
					boolean inInSet = false;
					if (index >= 0) { // In case haven't DC bound checks
						inInSet = this.bc.getCfgBlocksState().get(block).getIn().get(index);
					}
					
					if (inInSet ||	this.bcInBlock.contains(bcDef)) {
						addStmt = false;
					}
					else {
						this.bcInBlock.add(bcDef);
					}
				}
				else if (lStmt.getLabelString().matches(ArrayPassLabelRegex)) {
					if (!addStmt) {
						addStmt = true;
						continue;
					}
					
					addStmt = true;
				}
			}
			
			if (!addStmt) continue;
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				invalidateName(block, ((QuadrupletStmt)stmt).getDestination(), false);
			}
			else if (stmt.getClass().equals(LoadStmt.class)) {
				invalidateName(block, ((LoadStmt)stmt).getVariable(), true);
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				invalidateFunctionCall(block, (CallStmt)stmt);
			}
			
			newStmts.add(stmt);
		}
		
		block.setStatements(newStmts);
	}
	
	private void invalidateFunctionCall(CFGBlock block, CallStmt stmt) {
		if (stmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) return;
		
		BlockDataFlowState bFlow = this.bc.getCfgBlocksState().get(block);
		String methodName = block.getMethodName();
		
		// Kill bc for globals
		for (int i = 0; i < this.bc.getUniqueIndices().get(methodName).size(); i++) {
			BoundCheckDef bcDef = this.bc.getUniqueIndices().get(methodName).get(i);
			if (bcDef.getIndex().isGlobal()) {
				bFlow.getIn().set(i, false);
				this.bcInBlock.remove(bcDef);
			}
		}
	}
	
	private void invalidateName(CFGBlock block, Name var, boolean isLoad) {
		for (int i = 0; i < this.bc.getUniqueIndices().get(block.getMethodName()).size(); i++) {
			BoundCheckDef bc = this.bc.getUniqueIndices().get(block.getMethodName()).get(i);
			Name index = bc.getIndex();
			
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
				this.bc.getCfgBlocksState().get(block).getIn().set(i, false);
				this.bcInBlock.remove(var);
			}
		}
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
