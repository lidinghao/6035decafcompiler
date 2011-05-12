package decaf.parallel;

import java.util.ArrayList;
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
import decaf.codegen.flatir.VarName;
import decaf.dataflow.cfg.MethodIR;
import decaf.ralloc.ReachingDefinitions;

// Resolves array indices to Integer[] which follow the template 
// needed for parallelization distance vector analysis
public class ArrayIndexResolver {
	HashMap<String, MethodIR> mMap;
	ReachingDefinitions rd;
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	
	public ArrayIndexResolver(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.rd = new ReachingDefinitions(mMap);
		rd.analyze();
	}
	
	// Resolve index in the stmt
	// Return null if index cannot be deterministically resolved
	public Integer[] resolveIndex(LIRStatement stmt, Name index, List<Name> loopVars, String methodName) {
		// If index is Array, return null
		if (index.getClass().equals(ArrayName.class)) {
			return null;
		}
		System.out.println("Resolving index... " + stmt);
		
		BitSet reachingDefs = stmt.getReachingDefInSet();
		List<Name> newLoopVars = null;
		if (loopVars == null) {
			loopVars = loopVariablesForStmt(stmt);
		} else {
			// If new Loop vars is empty, return null
			newLoopVars = loopVariablesForStmt(stmt);
			if (newLoopVars.size() == 0) {
				System.out.println("Null - loop vars is empty");
				return null;
			}
			// Ensure that the loop variables we are looking at are the same
			if (newLoopVars.size() == loopVars.size()) {
				for (int i = 0; i < newLoopVars.size(); i++) {
					VarName newLoopVar = (VarName)newLoopVars.get(i);
					VarName origLoopVar = (VarName)loopVars.get(i);
					if (!newLoopVar.getId().equals(origLoopVar.getId())) {
						System.out.println("NEW LOOP VARS: " + newLoopVar.hashString());
						System.out.println("ORIG LOOP VARS: " + origLoopVar.hashString());
						System.out.println("Null - loop vars are different");
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
		if (index.getClass().equals(VarName.class)) {
			for (Name lVar : loopVars) {
				VarName varName = (VarName)lVar;
				if (varName.getId().equals(((VarName)index).getId())) {
					int indexOfLoopVar = loopVars.indexOf(lVar);
					coeffs[indexOfLoopVar] = 1;
					return coeffs;
				}
			}
		}
		
		List<QuadrupletStmt> defsForIndex = new ArrayList<QuadrupletStmt>();
		for (LIRStatement def : rd.getUniqueDefinitions().get(methodName)) {
			if (!(def.equals(stmt))) {
				if (((QuadrupletStmt)def).getDestination().equals(index)) {
					defsForIndex.add((QuadrupletStmt)def);
				}
			}
		}
		System.out.println("Definitions for index: " + defsForIndex);
		if (defsForIndex.size() == 0) {
			// No definitions for index... probably a global
			System.out.println("Null - No definitions for index...");
			return null;
		}
		QuadrupletStmt reachingDef = null;
		// If more than one reaching def for index, return null
		int numReachingDefs = 0;
		for (QuadrupletStmt def : defsForIndex) {
			if (reachingDefs.get(def.getMyId())) {
				reachingDef = def;
				System.out.println("Reaching def: " + def);
				numReachingDefs++;
			}
			if (numReachingDefs > 1) {
				System.out.println("Null - more than one reaching def");
				return null;
			}
		}
		
		// If the QuadrupletOp is not one of the following, return null
		if (reachingDef.getOperator() != QuadrupletOp.ADD && 
				reachingDef.getOperator() != QuadrupletOp.MUL && 
				reachingDef.getOperator() != QuadrupletOp.SUB &&
				reachingDef.getOperator() != QuadrupletOp.MINUS &&
				reachingDef.getOperator() != QuadrupletOp.MOVE) {
			System.out.println("Null - unsupported quadruplet operator");
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
					Integer[] arg1Coeffs = resolveIndex(reachingDef, arg1, loopVars, methodName);
					if (arg1Coeffs == null) {
						System.out.println("Null - arg1coeffs null");
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
				Integer[] arg2Coeffs = resolveIndex(reachingDef, arg2, loopVars, methodName);
				if (arg2Coeffs == null) {
					System.out.println("Null - arg2coeffs null");
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
		Integer[] arg1Coeffs = resolveIndex(reachingDef, arg1, loopVars, methodName);
		if (arg1Coeffs == null) {
			System.out.println("Null - arg1coeffs null");
			return null;
		}
		if (arg2 != null) {
			Integer[] arg2Coeffs = resolveIndex(reachingDef, arg2, loopVars, methodName);
			if (arg2Coeffs == null) {
				System.out.println("Null - arg2coeffs null");
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
						// Check next statements till something assigns to VarName which indicates
						// it is a loop variable
						int initIndex = i+1;
						QuadrupletStmt forInit = (QuadrupletStmt)methodStmts.get(initIndex);
						Name dest = forInit.getDestination();
						while (!dest.getClass().equals(VarName.class)) {
							initIndex++;
							forInit = (QuadrupletStmt)methodStmts.get(initIndex);
							dest = forInit.getDestination();
						}
						loopVars.add(dest);
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
