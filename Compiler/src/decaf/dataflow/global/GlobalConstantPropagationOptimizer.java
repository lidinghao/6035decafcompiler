package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class GlobalConstantPropagationOptimizer {
	private HashMap<String, MethodIR> mMap;
	private HashMap<QuadrupletStmt, BitSet> reachingDefForQStmts;
	private BlockReachingDefinitionGenerator reachingDefGenerator;
	
	public GlobalConstantPropagationOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.reachingDefForQStmts = new HashMap<QuadrupletStmt, BitSet>();
		this.reachingDefGenerator = new BlockReachingDefinitionGenerator(mMap, ConfluenceOperator.OR);
		this.reachingDefGenerator.generate();
	}
	
	public void performGlobalConstantProp() {
		if (reachingDefGenerator.getTotalDefinitions() == 0)
			return;
		
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			// Optimize blocks
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				optimize(block);
			}
		}
	}
	
	private void optimize(CFGBlock block) {
		BlockDataFlowState bFlow = reachingDefGenerator.getBlockReachingDefs().get(block);
		PopStmt popStmt;
		PushStmt pushStmt;
		CmpStmt cStmt;
		QuadrupletStmt qStmt;
		ConstantName arg1, arg2, arrIndex;
		Name qDest;
		
		for (LIRStatement stmt: block.getStatements()) {
			// Reset kill set
			bFlow.getKill().clear();
			if (stmt.getClass().equals(CallStmt.class)) {
				CallStmt callStmt = (CallStmt) stmt;
				if (callStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				
				// Update BlockDataFlowState kill set
				reachingDefGenerator.invalidateFunctionCall(bFlow);
				// Update BlockDataFlowState in set by using updated kill set
				bFlow.getIn().xor(bFlow.getKill());
			} else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				// For each use of a Name, see all the reaching definitions for that Name
				// If all reaching definitions assign the Name to the same constant, replace Name with that constant
				arg1 = reachingDefsHaveSameConstant(qStmt.getArg1(), bFlow);
				arg2 = reachingDefsHaveSameConstant(qStmt.getArg2(), bFlow);
				qDest = qStmt.getDestination();
				// If destination is ArrayName, try to optimize the index
				if (qDest.getClass().equals(ArrayName.class)) {
					arrIndex = reachingDefsHaveSameConstant(((ArrayName)qDest).getIndex(), bFlow);
					if (arrIndex != null)
						((ArrayName)qDest).setIndex(arrIndex);
				}
				if (arg1 != null) {
					// Set arg1 to Constant
					qStmt.setArg1(arg1);
				}
				if (arg2 != null) {
					// Set arg2 to Constant
					qStmt.setArg2(arg2);
				}
				
				System.out.println("reaching defs for qstmt: " + qStmt + " -> " + bFlow.getIn());

				// Update the map for QuadrupletStmt -> reaching defs for stmt (the current IN BitSet)
				reachingDefForQStmts.put(qStmt, (BitSet)bFlow.getIn().clone());
				// Update BlockDataFlowState kill set
				reachingDefGenerator.updateKillSet(qStmt.getDestination(), bFlow);
				// Update BlockDataFlowState in set by using updated kill set
				bFlow.getIn().xor(bFlow.getKill());
				// Add current stmt id to in set
				bFlow.getIn().set(qStmt.getMyId(), true);
				
			// Optimize PopStmt
			} else if (stmt.getClass().equals(PopStmt.class)) {
				popStmt = (PopStmt)stmt;
				arg1 = reachingDefsHaveSameConstant(popStmt.getName(), bFlow);
				if (arg1 != null) {
					popStmt.setName(arg1);
				}
				
			// Optimize PushStmt
			} else if (stmt.getClass().equals(PushStmt.class)) {
				pushStmt = (PushStmt)stmt;
				arg1 = reachingDefsHaveSameConstant(pushStmt.getName(), bFlow);
				if (arg1 != null) {
					pushStmt.setName(arg1);
				}
			
			// Optimize CmpStmt
			} else if (stmt.getClass().equals(CmpStmt.class)) {
				cStmt = (CmpStmt)stmt;
				arg1 = reachingDefsHaveSameConstant(cStmt.getArg1(), bFlow);
				if (arg1 != null) {
					cStmt.setArg1(arg1);
				}
				arg2 = reachingDefsHaveSameConstant(cStmt.getArg2(), bFlow);
				if (arg2 != null) {
					cStmt.setArg2(arg2);
				}
			}
		}		
	}
	
	// Returns a ConstantName if all reaching definitions assign arg to the same constant, null otherwise
	private ConstantName reachingDefsHaveSameConstant(Name arg, BlockDataFlowState bFlow) {
		if (arg == null)
			return null;
		// Don't constant propagate register values
		if (arg.getClass().equals(RegisterName.class))
			return null;
		
		// If the Name is ArrayName, we try to optimize its index as well
		Name arrIndex;
		List<QuadrupletStmt> additionalReachingDefs = null;
		if (arg.getClass().equals(ArrayName.class)) {
			// Try optimizing index if ArrayName
			arrIndex = reachingDefsHaveSameConstant(((ArrayName)arg).getIndex(), bFlow);
			if (arrIndex != null) {
				((ArrayName)arg).setIndex(arrIndex);
			}
			additionalReachingDefs = getAdditionalDefinitionsForArrayName((ArrayName)arg);
		}
		
		HashMap<Name, ArrayList<QuadrupletStmt>> nameToStmts = reachingDefGenerator.getNameToQStmts();
		ArrayList<QuadrupletStmt> stmtsForName = nameToStmts.get(arg);
		if (stmtsForName == null) {
			return null;
		}
		
		ConstantName cName = null;
		// First see if any of the original QuadrupletStmts reach and agree on the same ConstantName
		for (QuadrupletStmt qStmt : stmtsForName) {
			if (bFlow.getIn().get(qStmt.getMyId())) {
				// Check if statement is of type : arg = constant
				Name arg1 = qStmt.getArg1();
				if (arg1.getClass().equals(ConstantName.class) && qStmt.getArg2() == null) {
					if (cName == null) {
						cName = (ConstantName)arg1;
					} else {
						if (!((ConstantName)arg1).equals(cName)) {
							cName = null; break;
						}
					}
				} else {
					cName = null; break;
				}
			}
		}
		if (additionalReachingDefs != null) {
			if (cName != null) {
				// If the original QuadrupletStmts agree on a ConstantName, only then look at the 
				// additional reaching definitions
				// First check if any of them reach, and if they don't, we don't care about them
				boolean additionalReach = false;
				for (QuadrupletStmt qStmt : additionalReachingDefs) {
					if (bFlow.getIn().get(qStmt.getMyId())) {
						additionalReach = true;
						break;
					}
				}
				if (additionalReach) {
					// Get the ConstantName from these additional reaching defs
					ConstantName additionalCName = null;
					for (QuadrupletStmt qStmt : additionalReachingDefs) {
						if (bFlow.getIn().get(qStmt.getMyId())) {
							// Check if statement is of type : arg = constant
							Name arg1 = qStmt.getArg1();
							if (arg1.getClass().equals(ConstantName.class) && qStmt.getArg2() == null) {
								if (additionalCName == null) {
									additionalCName = (ConstantName)arg1;
								} else {
									if (!((ConstantName)arg1).equals(additionalCName)) {
										return null;
									}
								}
							} else {
								return null;
							}
						}
					}
					// Additional ConstantName and original ConstantName should be equal, otherwise return null
					if (!cName.equals(additionalCName)) {
						return null;
					}
				}
			}
		}
		return cName;
	}

	// Returns a list of QuadrupletStmts which define the given Name
	public List<QuadrupletStmt> getDefinitionsForName(Name arg) {
		HashMap<Name, ArrayList<QuadrupletStmt>> nameToStmts = reachingDefGenerator.getNameToQStmts();
		ArrayList<QuadrupletStmt> stmtsForName = nameToStmts.get(arg);
		// If the Name is an Array, add additional QuadrupletStmts
		if (arg.getClass().equals(ArrayName.class)) {
			if (stmtsForName != null) {
				stmtsForName.addAll(getAdditionalDefinitionsForArrayName((ArrayName)arg));
			} else {
				return getAdditionalDefinitionsForArrayName((ArrayName)arg); 
			}
		}
		return stmtsForName;
	}
	
	private List<QuadrupletStmt> getAdditionalDefinitionsForArrayName(ArrayName arg) {
		List<QuadrupletStmt> additionalReachingDefs = new ArrayList<QuadrupletStmt>();
		// Get the index, and if it is constant, get all qstmts which assign to 
		// array id with variable index
		Name arrIndex = ((ArrayName)arg).getIndex();
		String id = ((ArrayName)arg).getId();
		if (arrIndex.getClass().equals(ConstantName.class)) {
			for (QuadrupletStmt qStmt : reachingDefGenerator.getUniqueQStmts()) {
				if (reachingDefGenerator.isArrayNameWithIdAndVariableIndex(qStmt.getDestination(), id)) {
					additionalReachingDefs.add(qStmt);
				}
			}
		} else {
			for (QuadrupletStmt qStmt : reachingDefGenerator.getUniqueQStmts()) {
				// If index is not constant, get all qstmts which assign to array id	
				if (reachingDefGenerator.isArrayNameWithId(qStmt.getDestination(), id)) {
					additionalReachingDefs.add(qStmt);
				}
			}
		}
		return additionalReachingDefs;
	}
	
	public BlockReachingDefinitionGenerator getReachingDefGenerator() {
		return reachingDefGenerator;
	}
	
	public HashMap<QuadrupletStmt, BitSet> getReachingDefForQStmts() {
		return reachingDefForQStmts;
	}

	public void setReachingDefForQStmts(
			HashMap<QuadrupletStmt, BitSet> reachingDefForQStmts) {
		this.reachingDefForQStmts = reachingDefForQStmts;
	}
}
