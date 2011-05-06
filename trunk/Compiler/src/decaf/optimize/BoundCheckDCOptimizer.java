package decaf.optimize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

// The goal of this optimizer is to remove unneeded array bounds checks for array usages which
//	do with dataflow optimizations (not with array CSE which should be done after load, store adding)

public class BoundCheckDCOptimizer {
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
	
	private HashMap<String, MethodIR> mMap;
	private List<ArrayName> uncheckedArrayNames;
	
	public BoundCheckDCOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.uncheckedArrayNames = new ArrayList<ArrayName>();
	}
	
	public void performBoundCheckDC() {
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			performDC(this.mMap.get(s));
			this.mMap.get(s).regenerateStmts();
		}
	}
	
	private void performDC(MethodIR methodIR) {
		for (CFGBlock block: methodIR.getCfgBlocks()) {
			performDC(block);
		}
	}

	private void performDC(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		boolean add = true;

		for (int i = block.getStatements().size() - 1; i >=0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (isArrayPassStmt(stmt)) {
				ArrayName checkingForArrayName = getArrayNameFromLabel(block, i);
				if (checkingForArrayName == null) { // DC bound check
					add = false;
				}
				else {
					this.uncheckedArrayNames.remove(checkingForArrayName); // BC seen
				}
			}
			else if (isArrayBeginStmt(stmt)) {
				if (!add) { // If were removing BC, not start adding stmts again
					add = true;
					continue;
				}
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				markUnchecked(qStmt.getArg1());
				markUnchecked(qStmt.getArg2());
				markUnchecked(qStmt.getDestination());
				invalidateArrayNames(qStmt.getDestination());
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				markUnchecked(cStmt.getArg1());
				markUnchecked(cStmt.getArg2());
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;
				markUnchecked(pStmt.getName());
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;
				markUnchecked(pStmt.getName());
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				CallStmt cStmt = (CallStmt) stmt;
				if (!cStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) {
					this.uncheckedArrayNames.clear(); // Invalidate all bound checks
				}
			}
			
			if (add) {
				newStmts.add(0,stmt);
			}
		}
		
		block.setStatements(newStmts);
	}

	private void invalidateArrayNames(Name name) {
		List<ArrayName> toRemove = new ArrayList<ArrayName>();
		for (ArrayName arr: this.uncheckedArrayNames) {
			boolean removeName = false;
			Name index = arr.getIndex();
			
			do {
				if (index.equals(name)) { // Index being reassigned, KILL!
					removeName = true;
				}
				
				if (index.isArray()) {
					index = ((ArrayName)index).getIndex();
				}
				
			} while (index.isArray());
			
			if (removeName) {
				toRemove.add(arr);
			}
		}
		
		this.uncheckedArrayNames.removeAll(toRemove);
	}

	private void markUnchecked(Name name) {
		if (name == null || !name.isArray()) return;
		
		this.uncheckedArrayNames.add((ArrayName) name);
	}

	private ArrayName getArrayNameFromLabel(CFGBlock block, int i) {
		LIRStatement stmt = block.getStatements().get(i);
		String id = getArrayIDFromArrayLabelStmt((LabelStmt) stmt, "pass");
		
		while (!isArrayBeginStmt(stmt)) {
			i--;
			stmt = block.getStatements().get(i);
		}
		
		CmpStmt cmpStmt = (CmpStmt) block.getStatements().get(i+1);
		Name index = cmpStmt.getArg1();
		
		ArrayName rtn = null;		
		for (ArrayName name: this.uncheckedArrayNames) {
			if (!name.getId().equals(id)) continue;
			if (!name.getIndex().equals(index)) continue;
			
			rtn = name;
			break;
		}
		
		return rtn;
	}

	private boolean isArrayPassStmt(LIRStatement stmt) {
		if (stmt.getClass().equals(LabelStmt.class)) {
			String label = ((LabelStmt)stmt).getLabelString();
			return label.matches(ArrayPassLabelRegex);
		}
		return false;
	}
	
	private boolean isArrayBeginStmt(LIRStatement stmt) {
		if (stmt.getClass().equals(LabelStmt.class)) {
			String label = ((LabelStmt)stmt).getLabelString();
			return label.matches(ArrayBeginLabelRegex);
		}
		return false;
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
