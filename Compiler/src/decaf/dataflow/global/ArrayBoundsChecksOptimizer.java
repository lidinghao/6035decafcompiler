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

// This optimizer has two goals:
// 1. To do only one array bounds check per unique index instead 
//		of multiple array bounds checks for the same index if the 
//		index does not change in between.
// 2. To remove unneeded array bounds checks for array usages which
//		have been removed by the dead code elimination.
// NOTE: We need to run this multiple times for statements that need
// multiple array bounds checks before being executed (e.g. a[1] = a[2] + a[3]
// will need this optimization to run at least 3 times).

public class ArrayBoundsChecksOptimizer {
	private HashMap<String, MethodIR> mMap;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<CFGBlock, BlockDataFlowState> blockArrIndexDefs;
	private List<String> arrLabelsToRemove;
	private String nextArrPassLabelStmt;
	private List<CFGBlock> tempBlocksToCheck;
	// One Variable per Name
	private HashMap<ArrayName, Variable> arrNameToVar;
	// Map from a Name to all the ArrayName IDs (ArrayName -> Variable -> ID) that use that Name as an index
	private HashMap<Name, HashSet<Integer>> nameToArrNameIDs;
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*_array_[a-zA-z_]\\w*_\\d+_pass";
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*_array_[a-zA-z_]\\w*_\\d+_begin";
	private static String MethodCallLabelRegex = "\\w*_mcall_\\w*_\\d+";
	private int totalArrIndices;
	
	public ArrayBoundsChecksOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		blockArrIndexDefs = new HashMap<CFGBlock, BlockDataFlowState>();
		arrNameToVar = new HashMap<ArrayName, Variable>();
		nameToArrNameIDs = new HashMap<Name, HashSet<Integer>>();
		arrLabelsToRemove = new ArrayList<String>();
		tempBlocksToCheck = new ArrayList<CFGBlock>();
		nextArrPassLabelStmt = null;
		totalArrIndices = 0;
	}
	
	private void initialize() {
		// LiveVar IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		Variable.setID(0);
		ArrayName arrArg;
		QuadrupletStmt qStmt;
		Name arg1, arg2, dest;
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.getClass().equals(QuadrupletStmt.class)) {
						qStmt = (QuadrupletStmt)stmt;
						dest = qStmt.getDestination();
						if (dest != null) {
							if (dest.getClass().equals(ArrayName.class)) {
								arrArg = (ArrayName)dest;
								updateNameToVar(arrArg);
							}
						}
						arg1 = qStmt.getArg1();
						if (arg1 != null) {
							if (qStmt.getArg1().getClass().equals(ArrayName.class)) {
								arrArg = (ArrayName)arg1;
								updateNameToVar(arrArg);
							}
						}
						arg2 = qStmt.getArg2();
						if (arg2 != null) {
							if (qStmt.getArg2().getClass().equals(ArrayName.class)) {
								arrArg = (ArrayName)arg2;
								updateNameToVar(arrArg);
							}
						}
					}
				}
				cfgBlocksToProcess.add(block);
			}
		}
		totalArrIndices = arrNameToVar.size();
	}
	
	private void updateNameToVar(ArrayName arrArg) {
		// We are using something of the form A[i]
		if (!arrNameToVar.containsKey(arrArg)) {
			// Update A[i] -> Variable map
			Variable var = new Variable(arrArg);
			arrNameToVar.put(arrArg, var);
			// Update i -> A[i] map
			Name arrIndex = arrArg.getIndex();
			if (!(nameToArrNameIDs.containsKey(arrIndex))) {
				nameToArrNameIDs.put(arrIndex, new HashSet<Integer>());
			}
			nameToArrNameIDs.get(arrIndex).add(var.getMyId());
		}
	}
	
	public void performArrayOptimization() {
		// First remove the array bounds checks dead code
		removeArrayBoundsCheckDeadCode();
		
//		// Then optimize on re-using indices
//		initialize();
//		if (totalArrIndices == 0) {
//			return;
//		}
//		
//		while (cfgBlocksToProcess.size() != 0) {
//			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
//			BlockDataFlowState bFlow = generateForBlock(block);
//			blockArrIndexDefs.put(block, bFlow);
//		}
//		
//		for (String s: this.mMap.keySet()) {
//			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
//			
//			// Add blocks to process list
//			MethodIR mir =  this.mMap.get(s);
//			for (CFGBlock block: mir.getCfgBlocks()) {
//				if (isArrayPassBlock(block)) {
//					cfgBlocksToProcess.add(block);
//				}
//			}
//			
//			int i = 0;
//			while (cfgBlocksToProcess.size() != 0) {
//				CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[i];
//				if (nextArrPassLabelStmt != null) {
//					// Check if current block matches the label
//					LabelStmt arrPassLabel = (LabelStmt)block.getStatements().get(0);
//					if (getArrayIDFromArrayLabelStmt(arrPassLabel).equals(nextArrPassLabelStmt)) {
//						optimize(block);
//						i = 0;
//					} else {
//						// See next block
//						i++;
//					}
//				} else {
//					// Process current block
//					optimize(block);
//					i = 0;
//				}
//			}
//
//			// Remove the code based on the arrLabelsToRemove for the current method
//			removeArrayDeadCodeWithLabelsForMethod(arrLabelsToRemove, mir);
//			arrLabelsToRemove.clear();
//		}
	}
	
	private BlockDataFlowState generateForBlock(CFGBlock block) {
		BitSet origOut;
		if (blockArrIndexDefs.containsKey(block)) {
			origOut = blockArrIndexDefs.get(block).getOut();
		} else {
			origOut = new BitSet(totalArrIndices);
			// Confluence operator is AND, so initialize out set to all 1s
			origOut.set(0, totalArrIndices, true);
		}
		BlockDataFlowState bFlow = new BlockDataFlowState(totalArrIndices);
		// If there exists at least one predecessor, set In to all True
		if (block.getPredecessors().size() > 0) {
			bFlow.getIn().set(0, totalArrIndices);
		}
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (blockArrIndexDefs.containsKey(pred)) {
				in.and(blockArrIndexDefs.get(pred).getOut());
			}
		}
		calculateGenKillSets(block, bFlow);
		// Calculate Out
		BitSet out = bFlow.getOut();
		out.or(in);
		out.xor(bFlow.getKill()); // Invariant: kill is a subset of in
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
	
	// Gen corresponds to when some A[i] has been used
	// Kill corresponds to when a some i which is used in A[i] is re-defined
	private void calculateGenKillSets(CFGBlock block, BlockDataFlowState bFlow) {
		BitSet gen = bFlow.getGen();
		List<LIRStatement> blockStmts = block.getStatements();
		QuadrupletStmt qStmt;
		Name arg1, arg2, dest;
		
		for (LIRStatement stmt : blockStmts) {
			if (stmt.getClass().equals(CallStmt.class)) {
				// Invalidate arg registers
				for (int i = 0; i < Register.argumentRegs.length; i++) {
					updateKillSet(new RegisterName(Register.argumentRegs[i]), bFlow);
				}
				
				// Reset symbolic value for %RAX
				updateKillSet(new RegisterName(Register.RAX), bFlow);
				
				// Invalidate global vars;
				for (Name name: this.arrNameToVar.keySet()) {
					if (name.getClass().equals(VarName.class)) {
						VarName var = (VarName) name;
						if (var.getBlockId() == -1) { // Global
							updateKillSet(name, bFlow);
						}
					}
					if (name.getClass().equals(ArrayName.class)) {
						updateKillSet(name, bFlow);
					}
				}
			}
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				arg1 = qStmt.getArg1();
				arg2 = qStmt.getArg2();
				dest = qStmt.getDestination();
				if (arg1 != null) {
					if (arg1.getClass().equals(ArrayName.class)) {
						if (arrNameToVar.containsKey((ArrayName)arg1)) {
							gen.set(arrNameToVar.get((ArrayName)arg1).getMyId(), true);
						}
					}
				}
				if (arg2 != null) {
					if (arg2.getClass().equals(ArrayName.class)) {
						if (arrNameToVar.containsKey((ArrayName)arg2)) {
							gen.set(arrNameToVar.get((ArrayName)arg2).getMyId(), true);
						}
					}
				}
				if (dest != null) {
					if (dest.getClass().equals(ArrayName.class)) {
						if (arrNameToVar.containsKey((ArrayName)dest)) {
							gen.set(arrNameToVar.get((ArrayName)dest).getMyId(), true);
						}
					}
				}
				// Update kill set with destination
				updateKillSet(dest, bFlow);
			}
		}
	}
	
	private void updateKillSet(Name dest, BlockDataFlowState bFlow) {
		BitSet kill = bFlow.getKill();
		BitSet in = bFlow.getIn();
		HashSet<Integer> stmtIdsForDest = nameToArrNameIDs.get(dest);
		if (stmtIdsForDest != null) {
			// Kill if it is part of In
			Iterator<Integer> it = stmtIdsForDest.iterator();
			while (it.hasNext()) {
				int index = it.next();
				if (in.get(index)) {
					kill.set(index, true); // Ensures Kill is always a subset of In
				}
				// Remove from Gen if it exists
				bFlow.getGen().set(index, false);
			}
		}
	}
	
	// Assumes the block is an array pass block
	private void optimize(CFGBlock block) {
		// If the second statement is not a another array begin label, then we try to optimize
		LIRStatement stmt = block.getStatements().get(1);
		if (stmt != null) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				// Get block's label
				tempBlocksToCheck.add(block);
				// The next block to look for
				nextArrPassLabelStmt = getArrayIDFromArrayLabelStmt((LabelStmt)stmt);
				cfgBlocksToProcess.remove(block);
			}
		} else {
			// Remove this check, there is nothing after it
			LabelStmt arrPassLabel = (LabelStmt)(block.getStatements().get(0));
			arrLabelsToRemove.add(getArrayIDFromArrayLabelStmt(arrPassLabel));
			cfgBlocksToProcess.remove(block);
			tempBlocksToCheck.clear();
			nextArrPassLabelStmt = null;
		}
		// Optimize push, pop, cmp, quadruplet statements
		// If the statement contains multiple array bounds checks (e.g a[i] = a[j] + a[k]),
		// 	we will see this statement after the a[k] array bounds check
		// tempArrLabelsToRemove list will contain [ID label corresponding to 
		// 	a[i] checks, ID label corresponding to a[j] checks]
		// tempBlocksToCheck list will contain [array pass block for a[i] check, 
		// 	array pass block for a[j] check]
		
		// Optimize
		Name dest, arg1, arg2;
		int numOptimizationsPossible = 0;
		if (stmt.getClass().equals(PushStmt.class)) {
			arg1 = ((PushStmt)stmt).getName();
			// arg1 has to be ArrayName, so single optimization is possible
			numOptimizationsPossible = 1;
			tryToRemoveArrayCheck(block, arg1);
		} else if (stmt.getClass().equals(PopStmt.class)) {
			arg1 = ((PopStmt)stmt).getName();
			// arg1 has to be ArrayName, so single optimization is possible
			numOptimizationsPossible = 1;
			tryToRemoveArrayCheck(block, arg1);
		} else if (stmt.getClass().equals(CmpStmt.class)) {
			arg1 = ((CmpStmt)stmt).getArg1();
			arg2 = ((CmpStmt)stmt).getArg2();
			if (arg1.getClass().equals(ArrayName.class)) {
				if (arg2.getClass().equals(ArrayName.class)) {
					// Both arg1 and arg2 are ArrayName, so double optimization possible
					numOptimizationsPossible = 2;
					tryToRemoveArrayCheck(tempBlocksToCheck.get(0), arg1);
					tryToRemoveArrayCheck(block, arg2);
				} else {
					// Only arg1 is ArrayName, so single optimization is possible
					numOptimizationsPossible = 1;
					tryToRemoveArrayCheck(block, arg1);
				}
			} else {
				// Only arg2 is ArrayName, so single optimization is possible
				tryToRemoveArrayCheck(block, arg2);
			}
		} else if (stmt.getClass().equals(QuadrupletStmt.class)) {
			dest = ((QuadrupletStmt)stmt).getDestination();
			arg1 = ((QuadrupletStmt)stmt).getArg1();
			arg2 = ((QuadrupletStmt)stmt).getArg2();
			if (dest.getClass().equals(ArrayName.class)) {
				if (arg1.getClass().equals(ArrayName.class)) {
					if (arg2.getClass().equals(ArrayName.class)) {
						// All dest, arg1, and arg2 are ArrayName, so triple optimization possible
						numOptimizationsPossible = 3;
						tryToRemoveArrayCheck(tempBlocksToCheck.get(0), dest);
						tryToRemoveArrayCheck(tempBlocksToCheck.get(1), arg1);
						tryToRemoveArrayCheck(block, arg2);
					} else {
						// Only dest, arg1 are ArrayName, so double optimization possible
						numOptimizationsPossible = 2;
						tryToRemoveArrayCheck(tempBlocksToCheck.get(0), dest);
						tryToRemoveArrayCheck(block, arg1);
					}
				} else {
					if (arg2.getClass().equals(ArrayName.class)) {
						// Only dest, arg2 are ArrayName, so double optimization possible
						numOptimizationsPossible = 2;
						tryToRemoveArrayCheck(tempBlocksToCheck.get(0), dest);
						tryToRemoveArrayCheck(block, arg2);
					} else {
						// Only dest is ArrayName, so single optimization  is possible
						numOptimizationsPossible = 1;
						tryToRemoveArrayCheck(block, dest);
					}
				}
			} else {
				if (arg1.getClass().equals(ArrayName.class)) {
					if (arg2.getClass().equals(ArrayName.class)) {
						// arg1, and arg2 are ArrayName, so double optimization possible
						numOptimizationsPossible = 2;
						tryToRemoveArrayCheck(tempBlocksToCheck.get(0), arg1);
						tryToRemoveArrayCheck(block, arg2);
					} else {
						// Only arg1 is ArrayName, so single optimization is possible
						numOptimizationsPossible = 1;
						tryToRemoveArrayCheck(block, arg1);
					}
				} else {
					// Only arg2 is ArrayName, so single optimization is possible
					numOptimizationsPossible = 1;
					tryToRemoveArrayCheck(block, arg2);
				}
			}
		}
		// If there are more tempBlocksToCheck than optimizations possible, then there
		//		are extraneous blocks that can be removed starting from the earliest block
		if (tempBlocksToCheck.size() > numOptimizationsPossible-1) {
			for (int i = 0; i < tempBlocksToCheck.size() - numOptimizationsPossible + 1; i++) {
				LabelStmt arrPassLabel = (LabelStmt)(tempBlocksToCheck.get(i).getStatements().get(0));
				arrLabelsToRemove.add(getArrayIDFromArrayLabelStmt(arrPassLabel));
			}
		}
		// Remove current block
		cfgBlocksToProcess.remove(block);
		tempBlocksToCheck.clear();
		nextArrPassLabelStmt = null;
	}
	
	private void tryToRemoveArrayCheck(CFGBlock block, Name arg) {
		BlockDataFlowState bFlow = blockArrIndexDefs.get(block);
		int bitId = arrNameToVar.get((ArrayName)arg).getMyId();
		if (bFlow.getIn().get(bitId)) {
			// Array bounds have been checked before
			LabelStmt arrPassLabel = (LabelStmt)block.getStatements().get(0);
			arrLabelsToRemove.add(getArrayIDFromArrayLabelStmt(arrPassLabel));
		}
	}
	
	// Returns true if this block's first statement is an array pass label
	// Returns false otherwise
	private boolean isArrayPassBlock(CFGBlock block) {
		LIRStatement stmt = block.getStatements().get(0);
		if (stmt != null) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				if (((LabelStmt)stmt).getLabelString().matches(ArrayPassLabelRegex)) {
					return true;
				}
			}
		}
		return false;
	}
	
	// ARRAY BOUNDS CHECKS DEAD CODE REMOVAL
	
	private void removeArrayBoundsCheckDeadCode() {
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
		List<String> tempArrNameIndexBuffer = new ArrayList<String>();
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
					String arrNameIndex = getArrInfoStringFromLabelAndCmpStmt((LabelStmt)stmt, 
							(CmpStmt)(stmtList.get(stmtIndex+1)));
					tempArrNameIndexBuffer.add(arrNameIndex);
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
							for (String arrNameIndex : tempArrNameIndexBuffer) {
								String[] arrInfo = arrNameIndex.split("|");
								if (arrInfo[0].equals(arrName.getId()) && arrInfo[1].equals(arrName.getIndex().toString())) {
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

	private String getArrayIDFromArrayLabelStmt(LabelStmt stmt) {
		int id = stmt.getLabelString().lastIndexOf(".");
		return stmt.getLabelString().substring(0, id);
	}
	
	// Returns array id and index string in the form "id|index"
	private String getArrInfoStringFromLabelAndCmpStmt(LabelStmt lStmt, CmpStmt cStmt) {
		String arrString = lStmt.getLabelString();
		String[] parts = arrString.split(".");
		// Second index is array id
		String arrId = parts[2];
		Name index = cStmt.getArg1();
		return (arrId + "|" + index);
	}
}
