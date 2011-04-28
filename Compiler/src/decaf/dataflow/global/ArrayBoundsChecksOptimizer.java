package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.MethodIR;

// This optimizer has two goals:
// 1. To do only one array bounds check per unique index instead 
//		of multiple array bounds checks for the same index if the 
//		index does not change in between.
// 2. To remove unneeded array bounds checks for array usages which
//		have been removed by the dead code elimination.

public class ArrayBoundsChecksOptimizer {
	private HashMap<String, MethodIR> mMap;
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*_array_[a-zA-z_]\\w*_\\d+_pass";
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*_array_[a-zA-z_]\\w*_\\d+_begin";
	
	public ArrayBoundsChecksOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		
	}
	
	private void initialize() {
		
	}
	
	public void performArrayOptimization() {
		// First remove the dead code
		removeArrayBoundsCheckDeadCode();
		
		// Then optimize on re-using indices
		// Generate CFG
		
	}
	
	private void removeArrayBoundsCheckDeadCode() {
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			// Optimize method
			MethodIR method = this.mMap.get(s);
			removeArrayDeadCodeForMethod(method);
		}
	}
	
	private void removeArrayDeadCodeForMethod(MethodIR method) {
		boolean checkNextStmtForArrayRef = false;
		List<String> arrIDsToDelete = new ArrayList<String>();
		
		for (LIRStatement stmt : method.getStatements()) {
			if (isArrayPassStmt(stmt)) {
				checkNextStmtForArrayRef = true;
				arrIDsToDelete.add(getArrayIDFromArrayPassLabelStmt((LabelStmt)stmt));
				continue;
			}
			if (checkNextStmtForArrayRef) {
				if (containsArrayReferences(stmt)) {
					arrIDsToDelete.clear();
				checkNextStmtForArrayRef = false;
			}
		}
		if (!arrIDsToDelete.isEmpty()) {
			List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
			boolean inArrDeleteZone = false;
			for (LIRStatement stmt : method.getStatements()) {
				if (inArrDeleteZone) {
					if (isArrayPassStmt(stmt)) {
						inArrDeleteZone = false;
						continue;
					}
					continue;
				}
				if (isArrayBeginStmt(stmt)) {
					if (shouldDeleteArrBlock(((LabelStmt)stmt).getLabelString(), arrIDsToDelete)) {
						inArrDeleteZone = true;
						continue;
					}
				}
				newStmts.add(stmt);
			}
			method.setStatements(newStmts);
		}
	}
	
	private boolean shouldDeleteArrBlock(String arrBeginLabel, List<String> arrIDsToDelete) {
		for (String id : arrIDsToDelete) {
			if (arrBeginLabel.contains(id)) {
				return true;
			}
		}
		return false;
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
			System.out.println("is arr begin label? " + label + " --> " + label.matches(ArrayBeginLabelRegex));
			return label.matches(ArrayBeginLabelRegex);
		}
		return false;
	}
	
	private boolean containsArrayReferences(LIRStatement stmt) {
		if (stmt.getClass().equals(QuadrupletStmt.class)) {
			QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
			Name arg1 = qStmt.getArg1();
			Name arg2 = qStmt.getArg2();
			Name dest = qStmt.getDestination();
			if (arg1 != null) {
				if (arg1.getClass().equals(ArrayName.class))
					return true;
			}
			if (arg2 != null) {
				if (arg2.getClass().equals(ArrayName.class))
					return true;
			}
			if (dest != null) {
				if (dest.getClass().equals(ArrayName.class))
					return true;
			}
		}
		return false;
	}
	
	private String getArrayIDFromArrayPassLabelStmt(LabelStmt stmt) {
		int id = stmt.getLabelString().lastIndexOf("_");
		return stmt.getLabelString().substring(0, id);
	}
}
