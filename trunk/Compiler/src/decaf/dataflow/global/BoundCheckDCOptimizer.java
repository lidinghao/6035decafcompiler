package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

// The goal of this optimizer is to remove unneeded array bounds checks for array usages which
//	have been removed by the dead code elimination.

public class BoundCheckDCOptimizer {
	private HashMap<String, MethodIR> mMap;
	// Map from a Name to all the ArrayName IDs (ArrayName -> Variable -> ID) that use that Name as an index
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*_array_[a-zA-z_]\\w*_\\d+_pass";
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*_array_[a-zA-z_]\\w*_\\d+_begin";
	private static String MethodCallLabelRegex = "\\w*_mcall_\\w*_\\d+";
	
	public BoundCheckDCOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
	}
	
	public void performDC() {
		for (String s: this.mMap.keySet()) {
			System.out.println("FOR METHOD " + s);
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			// Optimize method
			MethodIR method = this.mMap.get(s);
			removeArrayDeadCodeForMethod(method);
		}
	}

	private void removeArrayDeadCodeForMethod(MethodIR method) {
		List<ArrayList<LIRStatement>> tempBuffer = new ArrayList<ArrayList<LIRStatement>>();
		List<ArrayName> tempArrNameIndexBuffer = new ArrayList<ArrayName>();
		boolean inArrBoundCheck = false;
		boolean checkNextStmt = false;
		ArrayList<LIRStatement> curTempBuffer = null;
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (CFGBlock block : method.getCfgBlocks()) {
			int stmtIndex = 0;
			List<LIRStatement> stmtList = block.getStatements();
			for (LIRStatement stmt : stmtList) {
				if (inArrBoundCheck) {
					curTempBuffer.add(stmt);
				}
				if (isArrayPassStmt(stmt)) {
					inArrBoundCheck = false;
					checkNextStmt = true;
					// Add current buffer to list of temp buffers
					tempBuffer.add(curTempBuffer);
					stmtIndex++;
					continue;
				}
				if (isArrayBeginStmt(stmt)) {
					inArrBoundCheck = true;
					curTempBuffer = new ArrayList<LIRStatement>();
					curTempBuffer.add(stmt);
					ArrayName arrName = getArrInfoStringFromLabelAndCmpStmt((LabelStmt)stmt,
							(CmpStmt)(stmtList.get(stmtIndex+1)));
					tempArrNameIndexBuffer.add(arrName);
					checkNextStmt = false;
					stmtIndex++;
					continue;
					
				} else if (checkNextStmt) {
					List<ArrayName> arrayRefs = getArrayReferences(block, stmtIndex, stmt);
					if (arrayRefs != null) {
						// Process stored buffers, keep the ones that matter, ignore the ones that don't
						HashSet<Integer> buffersProcessed = new HashSet<Integer>();
						for (ArrayName arrName : arrayRefs) {
							// Based on the arrName observed, compare to temp Array IDs stored and if there is a match,
							// remove the Array ID from the buffer and write its statement buffer to the new statement list
							int bufferIndex = 0;
							for (ArrayName aName : tempArrNameIndexBuffer) {
								if (aName.equals(arrName)) {
									// There is a match between this buffer and the found array reference
									if (!buffersProcessed.contains(bufferIndex)) {
										newStmts.addAll(tempBuffer.get(bufferIndex));
										buffersProcessed.add(bufferIndex);
									}
								}
								bufferIndex++;
							}
						}
					}
					curTempBuffer = null;
					tempBuffer.clear();
					checkNextStmt = false;
				}
				if (!inArrBoundCheck) 
					newStmts.add(stmt);
				stmtIndex++;
			}
		}
	}
	
	// Returns any ArrayName references to arrays in the given statement in the block
	// If the stmt is a method call, traverse the next stmts till the 'call'
	private List<ArrayName> getArrayReferences(CFGBlock block, int stmtIndex, LIRStatement stmt) {
		List<ArrayName> arrayRefs = new ArrayList<ArrayName>();
		if (isMethodCallStmt(stmt)) {
			// Method call
			LIRStatement methodStmt;
			List<LIRStatement> stmts = block.getStatements();
			for (int i = stmtIndex+1; i < stmts.size(); i++) {
				methodStmt = stmts.get(i);
				if (methodStmt.getClass().equals(CallStmt.class))
					break;
				arrayRefs.addAll(getArrayReferences(methodStmt));
			}
		} else {
			// Non-method call statement
			arrayRefs.addAll(getArrayReferences(stmt));
		}
		return arrayRefs;
	}
	
	private List<ArrayName> getArrayReferences(LIRStatement stmt) {
		List<ArrayName> arrayRefs = new ArrayList<ArrayName>();
		
		Name arg1 = null, arg2 = null, arg3 = null;
		// Check for Push, Pop, CmpStmt, QuadrupletStmt, Load
		if (stmt.getClass().equals(PushStmt.class)) {
			PushStmt pStmt = (PushStmt)stmt;
			arg1 = pStmt.getName();
			
		} else if (stmt.getClass().equals(PopStmt.class)) {
			PopStmt pStmt = (PopStmt)stmt;
			arg1 = pStmt.getName();
			
		} else if (stmt.getClass().equals(CmpStmt.class)) {
			CmpStmt cStmt = (CmpStmt)stmt;
			arg1 = cStmt.getArg1();
			arg2 = cStmt.getArg2();
			
		} else if (stmt.getClass().equals(QuadrupletStmt.class)) {
			QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
			arg1 = qStmt.getArg1();
			arg2 = qStmt.getArg2();
			arg3 = qStmt.getDestination();
			
		} else if (stmt.getClass().equals(LoadStmt.class)) {
			LoadStmt lStmt = (LoadStmt)stmt;
			arg1 = lStmt.getVariable();
		}
		if (arg1 != null) {
			if (arg1.getClass().equals(ArrayName.class))
				arrayRefs.add((ArrayName)arg1);
		}
		if (arg2 != null) {
			if (arg2.getClass().equals(ArrayName.class))
				arrayRefs.add((ArrayName)arg2);
		}
		if (arg3 != null) {
			if (arg3.getClass().equals(ArrayName.class))
				arrayRefs.add((ArrayName)arg3);
		}
		
		return arrayRefs;
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
	
	private boolean isMethodCallStmt(LIRStatement stmt) {
		if (stmt.getClass().equals(LabelStmt.class)) {
			String label = ((LabelStmt)stmt).getLabelString();
			return label.matches(MethodCallLabelRegex);
		}
		return false;
	}
	
	// Returns ArrayName from the array label and the next compare stmt
	private ArrayName getArrInfoStringFromLabelAndCmpStmt(LabelStmt lStmt, CmpStmt cStmt) {
		String arrString = lStmt.getLabelString();
		String[] parts = arrString.split(".");
		// Second index is array id
		String arrId = parts[2];
		Name index = cStmt.getArg1();
		return new ArrayName(arrId, index);
	}
}
