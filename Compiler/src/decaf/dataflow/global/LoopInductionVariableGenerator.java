package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.MethodIR;

// Finds all the induction variables in the loops in the program
public class LoopInductionVariableGenerator {
	private HashMap<String, MethodIR> mMap;
	private HashSet<InductionVariable> inductionVariables;
	private LoopInvariantGenerator loopInvariantGen;
	
	public LoopInductionVariableGenerator(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.inductionVariables = new HashSet<InductionVariable>();
		this.loopInvariantGen = new LoopInvariantGenerator(mMap);
	}
	
	public boolean isInductionVariable(Name var, String loopId) {
		List<InductionVariable> inductionVarsInLoop = new ArrayList<InductionVariable>();
		for (InductionVariable iv : inductionVariables) {
			if (iv.getLqStmt().getLoopBodyBlockId().equals(loopId)) {
				inductionVarsInLoop.add(iv);
			}
		}
		return (getInductionVariable(var, inductionVarsInLoop) != null);
	}
	
	public void generateInductionVariables() {
		loopInvariantGen.getLoopBodyQuadrupletStmts();
		int numFound;
		HashMap<String, HashSet<LoopQuadrupletStmt>> loopQStmts = loopInvariantGen.getLoopBodyQuadrupletStmts();
		do {
			numFound = inductionVariables.size();
			for (String s : loopQStmts.keySet()) {
				for (LoopQuadrupletStmt lqStmt : loopQStmts.get(s)) {
					if (hasMoreThanOneDefinitionInLoopBody(lqStmt, loopInvariantGen.getQuadrupletStmtsInLoopBody(s))) {
						// More than one definition is not allowed for induction variables
						continue;
					}
					// Determine whether the statement is definition of basic induction variable
					if (addBasicInductionVariable(lqStmt, loopInvariantGen.getQuadrupletStmtsInLoopBody(s))) {
						continue;
					}
					// Determine whether the statement is defintion of derivied induction variable
					addDerivedInductionVariable(lqStmt, loopInvariantGen.getQuadrupletStmtsInLoopBody(s), s);
				}
			}
		} while (numFound != inductionVariables.size());
	}
	
	// Returns true if there is more than one definition for the dest in the given lqStmt
	// Return false otherwise
	private boolean hasMoreThanOneDefinitionInLoopBody(LoopQuadrupletStmt lqStmt, HashSet<QuadrupletStmt> loopQStmts) {
		QuadrupletStmt qStmt = lqStmt.getqStmt();
		Name dest = qStmt.getDestination();
		for (QuadrupletStmt qs : loopQStmts) {
			if (qs.getDestination().equals(dest)) {
				return true;
			}
		}
		return false;
	}
	
	// Returns true if it is basic induction variable: i = i + c where c is loop invariant
	// Return false otherwise
	private boolean addBasicInductionVariable(LoopQuadrupletStmt lqStmt, HashSet<QuadrupletStmt> loopQStmts) {
		if (lqStmt == null)
			return false;
		QuadrupletStmt qStmt = lqStmt.getqStmt();
		Name dest = qStmt.getDestination();
		Name arg1 = qStmt.getArg1();
		Name arg2 = qStmt.getArg2();
		QuadrupletOp qOp = qStmt.getOperator();	
		InductionVariable newIVar;
		if (arg1 != null) {
			if (arg1.equals(dest)) {
				if (arg2 != null) {
					if (loopInvariantGen.argSatisfiesLoopInvariant(arg2, 
							loopInvariantGen.getReachingDefForQStmts().get(qStmt), loopQStmts)) {
								if (qOp == QuadrupletOp.ADD || qOp == QuadrupletOp.MINUS) {
									newIVar = new InductionVariable(lqStmt);
									newIVar.setFamilyName(dest);
									inductionVariables.add(newIVar);
									return true;
								}
							}
				} else {
					if (qOp == null) {
						// i = i, should be deadcoded, but cover it just in case
						newIVar = new InductionVariable(lqStmt);
						newIVar.setFamilyName(dest);
						inductionVariables.add(newIVar);
						return true;
					}
				}
			} else if (arg2 != null) {
				if (arg2.equals(dest)) {
					// arg1 cannot be null
					if (loopInvariantGen.argSatisfiesLoopInvariant(arg1, 
							loopInvariantGen.getReachingDefForQStmts().get(qStmt), loopQStmts)) {
								if (qOp == QuadrupletOp.ADD || qOp == QuadrupletOp.MINUS) {
									newIVar = new InductionVariable(lqStmt);
									newIVar.setFamilyName(dest);
									inductionVariables.add(newIVar);
									return true;
								}
							}
				}
			}
		}
		return false;
	}
	
	private boolean addDerivedInductionVariable(LoopQuadrupletStmt lqStmt, HashSet<QuadrupletStmt> loopQStmts, String loopId) {
		if (lqStmt == null)
			return false;
		QuadrupletStmt qStmt = lqStmt.getqStmt();
		Name arg1 = qStmt.getArg1();
		Name arg2 = qStmt.getArg2();
		QuadrupletOp qOp = qStmt.getOperator();
		if (arg1 == null) {
			return false;
		}
		List<InductionVariable> inductionVarsInLoop = new ArrayList<InductionVariable>();
		for (InductionVariable iv : inductionVariables) {
			if (iv.getLqStmt().getLoopBodyBlockId().equals(loopId)) {
				inductionVarsInLoop.add(iv);
			}
		}
		InductionVariable newIVar = null;
		InductionVariable argIVar = getInductionVariable(arg1, inductionVarsInLoop);
		if (argIVar != null) {
			// arg1 is an induction variable
			if (arg2 != null) {
				if (loopInvariantGen.argSatisfiesLoopInvariant(arg2, 
						loopInvariantGen.getReachingDefForQStmts().get(qStmt), loopQStmts)) {
					if (qOp == QuadrupletOp.MUL || qOp == QuadrupletOp.ADD || qOp == QuadrupletOp.SUB) {
						newIVar = new InductionVariable(lqStmt);
						newIVar.setFamilyName(argIVar.getFamilyName());
					}
				}
			} else {
				if (qOp == QuadrupletOp.MINUS || qOp == null) {
					newIVar = new InductionVariable(lqStmt);
				}
			}
		} else {
			argIVar = getInductionVariable(arg2, inductionVarsInLoop);
			if (argIVar != null) {
				// arg2 is induction variable
				// arg1 cannot be null
				if (loopInvariantGen.argSatisfiesLoopInvariant(arg1, 
						loopInvariantGen.getReachingDefForQStmts().get(qStmt), loopQStmts)) {
					if (qOp == QuadrupletOp.MUL || qOp == QuadrupletOp.ADD || qOp == QuadrupletOp.SUB) {
						newIVar = new InductionVariable(lqStmt);
						newIVar.setFamilyName(argIVar.getFamilyName());
					}
				}
			}
		}
		if (newIVar == null) {
			return false;
		}
		
		return false;
	}
	
	// Returns InductionVariable for arg if arg is an induction variable in the given list of InductionVariables
	// Returns null otherwise
	private InductionVariable getInductionVariable(Name arg, List<InductionVariable> inductionVariables) {
		if (arg == null)
			return null;
		for (InductionVariable iVar : inductionVariables) {
			Name dest = iVar.getVariable();
			if (dest.equals(arg)) {
				return iVar;
			}
		}
		return null;
	}
	
	public void setmMap(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
	}
}
