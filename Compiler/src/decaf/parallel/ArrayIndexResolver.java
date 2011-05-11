package decaf.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.GlobalConstantPropagationOptimizer;

// Resolves array indices to Integer[] which follow the template 
// needed for parallelization distance vector analysis
public class ArrayIndexResolver {
	HashMap<String, MethodIR> mMap;
	// Map from QuadrupletStmt to BitSet representing all the 
	// QuadrupletStmt IDs which reach that point
	private HashMap<LIRStatement, BitSet> reachingDefForStmts;
	// Map from Name to QuadrupletStmt which assign to that Name
	private HashMap<Name, ArrayList<QuadrupletStmt>> nameToQStmts;
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	
	public ArrayIndexResolver(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		GlobalConstantPropagationOptimizer gcp = new GlobalConstantPropagationOptimizer(mMap);
		// Generates the Map of QStmt -> BitSet representing 
		// all the QStmts IDs which reach at that point
		gcp.generateReachingDefsForQStmts();
		this.reachingDefForStmts = gcp.getReachingDefForStmts();
		this.nameToQStmts = gcp.getReachingDefGenerator().getNameToQStmts();
	}
	
	// Resolve index in the stmt
	// Return null if index cannot be deterministically resolved
	public Integer[] resolveIndex(LIRStatement stmt, Name index, List<Name> loopVars) {
		// If index is Array, return null
		if (index.getClass().equals(ArrayName.class)) {
			return null;
		}
		
		BitSet reachingDefs = reachingDefForStmts.get(stmt);
		List<Name> newLoopVars = null;
		if (loopVars == null) {
			loopVars = loopVariablesForStmt(stmt);
		} else {
			// If new Loop vars is empty, return null
			newLoopVars = loopVariablesForStmt(stmt);
			if (newLoopVars.size() == 0) {
				return null;
			}
			// Ensure that the loop variables we are looking at are the same
			if (newLoopVars.size() == loopVars.size()) {
				for (int i = 0; i < newLoopVars.size(); i++) {
					if (newLoopVars.get(i) != loopVars.get(i)) {
						return null;
					}
				}
			}
		}
		
		// Initialize coeffs
		Integer[] coeffs = new Integer[loopVars.size()+1];
		for (int i = 0; i < coeffs.length; i++) {
			coeffs[i] = 0;
		}
		// If the index is a constant, trivial case
		if (index.getClass().equals(ConstantName.class)) {
			coeffs[coeffs.length-1] = Integer.parseInt(((ConstantName)index).getValue());
			return coeffs;
		}

		// If the index is a loop var, trivial case
		if (loopVars.contains(index)) {
			int indexOfLoopVar;
			if (newLoopVars != null) {
				indexOfLoopVar = newLoopVars.indexOf(index);
			} else {
				indexOfLoopVar = loopVars.indexOf(index);
			}
			coeffs[indexOfLoopVar] = 1;
			return coeffs;
		}
		
		List<QuadrupletStmt> defsForIndex = nameToQStmts.get(index);
		if (defsForIndex == null) {
			// No definitions for index... probably a global
			return null;
		}
		QuadrupletStmt reachingDef = null;
		// If more than one reaching def for index, return null
		int numReachingDefs = 0;
		for (QuadrupletStmt def : defsForIndex) {
			if (reachingDefs.get(def.getMyId())) {
				reachingDef = def;
				numReachingDefs++;
			}
			if (numReachingDefs > 1) {
				return null;
			}
		}
		
		// If the QuadrupletOp is not one of the following, return null
		if (reachingDef.getOperator() != QuadrupletOp.ADD && 
				reachingDef.getOperator() != QuadrupletOp.MUL && 
				reachingDef.getOperator() != QuadrupletOp.SUB &&
				reachingDef.getOperator() != QuadrupletOp.MINUS &&
				reachingDef.getOperator() != QuadrupletOp.MOVE) {
			return null;
		}
		
		// Recursively resolve the arguments of the reaching def
		// Trivial cases:
		// 1. One or two constant arguments
		Name arg1 = reachingDef.getArg1();
		Name arg2 = reachingDef.getArg2();
		if (arg1.getClass().equals(ConstantName.class)) {
			int arg1Val = Integer.parseInt(((ConstantName)arg1).getValue());
			if (arg2 != null) {
				if (arg2.getClass().equals(ConstantName.class)) {
					int arg2Val = Integer.parseInt(((ConstantName)arg2).getValue());
					
					if (reachingDef.getOperator() == QuadrupletOp.ADD) {
						coeffs[loopVars.size()] = arg1Val + arg2Val;
						return coeffs;
					} else if (reachingDef.getOperator() == QuadrupletOp.SUB) {
						coeffs[loopVars.size()] = arg1Val - arg2Val;
						return coeffs;
					} else if (reachingDef.getOperator() == QuadrupletOp.MUL) {
						coeffs[loopVars.size()] = arg1Val * arg2Val;
						return coeffs;
					}
				}
			} else {
				if (reachingDef.getOperator() != QuadrupletOp.MINUS) {
					coeffs[loopVars.size()] = arg1Val;
					return coeffs;
				} else {
					coeffs[loopVars.size()] = -arg1Val;
					return coeffs;
				}
			}
		}
		// 2. Constant with some non-constant
		if (!arg1.getClass().equals(ConstantName.class)) {
			if (arg2 != null) {
				if (arg2.getClass().equals(ConstantName.class)) {
					int arg2Val = Integer.parseInt(((ConstantName)arg2).getValue());
					Integer[] arg1Coeffs = resolveIndex(reachingDef, arg1, loopVars);
					if (arg1Coeffs == null) {
						return null;
					}
					if (reachingDef.getOperator() == QuadrupletOp.ADD) {
						arg1Coeffs[loopVars.size()] += arg2Val;
						return arg1Coeffs;
						
					} else if (reachingDef.getOperator() == QuadrupletOp.SUB) {
						arg1Coeffs[loopVars.size()] -= arg2Val;
						return arg1Coeffs;
						
					} else if (reachingDef.getOperator() == QuadrupletOp.MUL) {
						for (int i = 0; i < loopVars.size()+1; i++) {
							arg1Coeffs[i] *= arg2Val;
						}
						return arg1Coeffs;
					}
				}
			}
		} else {
			int arg1Val = Integer.parseInt(((ConstantName)arg1).getValue());
			if (arg2 != null) {
				// arg2 must be non-constant
				Integer[] arg2Coeffs = resolveIndex(reachingDef, arg2, loopVars);
				if (arg2Coeffs == null) {
					return null;
				}

				if (reachingDef.getOperator() == QuadrupletOp.ADD) {
					arg2Coeffs[loopVars.size()] += arg1Val;
					return arg2Coeffs;
					
				} else if (reachingDef.getOperator() == QuadrupletOp.SUB) {
					arg2Coeffs[loopVars.size()] -= arg1Val;
					return arg2Coeffs;
					
				} else if (reachingDef.getOperator() == QuadrupletOp.MUL) {
					for (int i = 0; i < loopVars.size()+1; i++) {
						arg2Coeffs[i] *= arg1Val;
					}
					return arg2Coeffs;
				}
			}
		}
		// 3. One or two non-constants
		Integer[] arg1Coeffs = resolveIndex(reachingDef, arg1, loopVars);
		if (arg1Coeffs == null) {
			return null;
		}
		if (arg2 != null) {
			Integer[] arg2Coeffs = resolveIndex(reachingDef, arg2, loopVars);
			if (arg2Coeffs == null) {
				return null;
			}
			if (reachingDef.getOperator() == QuadrupletOp.ADD) {
				for (int i = 0; i < loopVars.size()+1; i++) {
					arg1Coeffs[i] += arg2Coeffs[i];
				}
				return arg1Coeffs;
				
			} else if (reachingDef.getOperator() == QuadrupletOp.SUB) {
				for (int i = 0; i < loopVars.size()+1; i++) {
					arg1Coeffs[i] -= arg2Coeffs[i];
				}
				return arg1Coeffs;
				
			} else if (reachingDef.getOperator() == QuadrupletOp.MUL) {
				for (int i = 0; i < loopVars.size()+1; i++) {
					arg1Coeffs[i] *= arg2Coeffs[i];
				}
				return arg1Coeffs;
			}
		} else {
			return arg1Coeffs;
		}
		
		return null;
	}
	
	// Returns the loop variables at play at the level of the stmt
	public List<Name> loopVariablesForStmt(LIRStatement targetStmt) {
		String forLabel;
		List<Name> loopVars = new ArrayList<Name>();
		for (String s : mMap.keySet()) {
			List<LIRStatement> methodStmts = mMap.get(s).getStatements();
			for (int i = 0; i < methodStmts.size(); i++) {
				LIRStatement stmt = methodStmts.get(i);
				if (stmt.getClass().equals(LabelStmt.class)) {
					forLabel = ((LabelStmt)stmt).getLabelString();
					if (forLabel.matches(ForInitLabelRegex)) {
						// Get next statement, which defines the loop variable
						loopVars.add(((QuadrupletStmt)methodStmts.get(i+1)).getDestination());
					} else if (forLabel.matches(ForEndLabelRegex)) {
						// Remove last loop variable in list
						loopVars.remove(loopVars.size()-1);
					}
					continue;
				}
				if (stmt == targetStmt) {
					return loopVars;
				}
			}
		}
		// Should be empty...
		return loopVars;
	}
}
