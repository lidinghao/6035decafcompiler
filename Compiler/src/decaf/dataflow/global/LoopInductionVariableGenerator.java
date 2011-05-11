package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.MethodIR;

// Finds all the induction variables in the loops in the program
public class LoopInductionVariableGenerator {
	private HashSet<InductionVariable> inductionVariables;
	private LoopInvariantGenerator loopInvariantGen;

	public LoopInductionVariableGenerator(HashMap<String, MethodIR> mMap) {
		this.inductionVariables = new HashSet<InductionVariable>();
		this.loopInvariantGen = new LoopInvariantGenerator(mMap);
	}
	
	public boolean isInductionVariable(Name var, String loopId) {
		List<InductionVariable> inductionVarsInLoop = getInductionVariablesForLoop(loopId);
		return (getInductionVariable(var, inductionVarsInLoop) != null);
	}
	
	public void generateInductionVariables() {
		loopInvariantGen.generateLoopInvariants();
		int numFound;
		HashMap<String, HashSet<LoopQuadrupletStmt>> loopQStmts = loopInvariantGen.getAllLoopBodyQStmts();
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
					// Determine whether the statement is defintion of derived induction variable
					addDerivedInductionVariable(lqStmt, loopInvariantGen.getQuadrupletStmtsInLoopBody(s), s);
				}
			}
		} while (numFound != inductionVariables.size());
		
		System.out.println("INDUCTION STMTS: " + inductionVariables);
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
	// Adds the induction variable to the overall list if one is found
	private boolean addBasicInductionVariable(LoopQuadrupletStmt lqStmt, HashSet<QuadrupletStmt> loopQStmts) {
		if (lqStmt == null)
			return false;
		QuadrupletStmt qStmt = lqStmt.getqStmt();
		Name dest = qStmt.getDestination();
		Name arg1 = qStmt.getArg1();
		Name arg2 = qStmt.getArg2();
		QuadrupletOp qOp = qStmt.getOperator();	
		String loopId = lqStmt.getLoopBodyBlockId();
		InductionVariable newIVar;
		if (arg1 != null) {
			if (arg1.equals(dest)) {
				if (arg2 != null) {
					if (loopInvariantGen.argSatisfiesLoopInvariant(arg2, 
							loopInvariantGen.getReachingDefForQStmts().get(qStmt), loopQStmts)) {
								if (qOp == QuadrupletOp.ADD || qOp == QuadrupletOp.MINUS) {
									newIVar = new InductionVariable(lqStmt);
									newIVar.setFamilyName(dest);
									newIVar.setDerived(false);
									newIVar.setAdder(arg2);
									newIVar.setMultiplier(new ConstantName(1));
									newIVar.setBlockId(loopInvariantGen.getLoopIdToBlockId().get(loopId));
									inductionVariables.add(newIVar);
									return true;
								}
							}
				} else {
					if (qOp == null) {
						// i = i, should be deadcoded, but cover it just in case
						newIVar = new InductionVariable(lqStmt);
						newIVar.setFamilyName(dest);
						newIVar.setDerived(false);
						newIVar.setAdder(new ConstantName(0));
						newIVar.setMultiplier(new ConstantName(1));
						newIVar.setBlockId(loopInvariantGen.getLoopIdToBlockId().get(loopId));
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
									newIVar.setDerived(false);
									newIVar.setAdder(arg1);
									newIVar.setMultiplier(new ConstantName(1));
									newIVar.setBlockId(loopInvariantGen.getLoopIdToBlockId().get(loopId));
									inductionVariables.add(newIVar);
									return true;
								}
							}
				}
			}
		}
		return false;
	}
	
	// Returns true if a derived induction variable is found
	// False otherwise
	// Adds a derived induction variable to the list of induction variables
	private boolean addDerivedInductionVariable(LoopQuadrupletStmt lqStmt, 
			HashSet<QuadrupletStmt> loopQStmts, String loopId) {
		if (lqStmt == null)
			return false;
		QuadrupletStmt qStmt = lqStmt.getqStmt();
		Name arg1 = qStmt.getArg1();
		Name arg2 = qStmt.getArg2();
		QuadrupletOp qOp = qStmt.getOperator();
		if (arg1 == null) {
			return false;
		}
		List<InductionVariable> inductionVarsInLoop = getInductionVariablesForLoop(loopId);
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
						newIVar.setDerived(true);
						newIVar.setDerivedFrom(argIVar);
						newIVar.setBlockId(loopInvariantGen.getLoopIdToBlockId().get(loopId));
						if (qOp == QuadrupletOp.MUL) {
							newIVar.setAdderByMult(arg2);
							newIVar.setMultiplierByMult(arg2);
						} else {
							newIVar.setAdderByAdd(arg2, qOp);
							newIVar.setMultiplier(argIVar.getMultiplier());
						}
					}
				}
			} else {
				if (qOp == QuadrupletOp.MINUS || qOp == null) {
					newIVar = new InductionVariable(lqStmt);
					newIVar.setFamilyName(argIVar.getFamilyName());
					newIVar.setDerived(true);
					newIVar.setDerivedFrom(argIVar);
					newIVar.setBlockId(loopInvariantGen.getLoopIdToBlockId().get(loopId));
					if (qOp == QuadrupletOp.MINUS) {
						newIVar.setAdderByMult(new ConstantName(-1));
						newIVar.setMultiplierByMult(new ConstantName(-1));
					} else {
						newIVar.setAdder(argIVar.getAdder());
						newIVar.setMultiplier(argIVar.getMultiplier());
					}
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
						newIVar.setDerived(true);
						newIVar.setDerivedFrom(argIVar);
						newIVar.setBlockId(loopInvariantGen.getLoopIdToBlockId().get(loopId));
						if (qOp == QuadrupletOp.MUL) {
							newIVar.setAdderByMult(arg1);
							newIVar.setMultiplierByMult(arg1);
						} else {
							newIVar.setAdderByAdd(arg1, qOp);
							newIVar.setMultiplier(argIVar.getMultiplier());
						}
					}
				}
			}
		}
		if (newIVar != null) {
			// A new induction variable was set, so now perform more checks
			if (newIVar.getDerivedFrom().isDerived()) {
				// If the induction variable was derived from a derived induction variable
				// The only definition of the derived induction variable that reaches this definition
				// is the one in the loop
				Name derivedFrom = newIVar.getDerivedFrom().getVariable();
				// Reaching definition for current QuadrupletStmt
				BitSet reachingDefForQStmt = loopInvariantGen.getReachingDefForQStmts().get(qStmt);
				// Get all possible definitions for the derivedFrom
				List<QuadrupletStmt> defsForArg = loopInvariantGen.getGcp().getDefinitionsForName(derivedFrom);
				if (defsForArg != null) {
					List<QuadrupletStmt> reachingDefsForArg = new ArrayList<QuadrupletStmt>();
					// Use BitSet to generate list of reaching definitions of the arg
					for (QuadrupletStmt qs : defsForArg) {
						if (reachingDefForQStmt.get(qStmt.getMyId())) {
							reachingDefsForArg.add(qs);
						}
					}
					if (reachingDefsForArg.size() == 1) {
						// One reaching def, ensure it is in the loop
						if (loopQStmts.contains(reachingDefsForArg.get(0))) {
							// Now ensure there is no definition of the familyName on any path between the
							// reaching def and the current statement
							Name familyName = newIVar.getFamilyName();
							if (!defForNameExistsBetweenStmtsInLoop(familyName, reachingDefsForArg.get(0), qStmt, loopId)) {
								inductionVariables.add(newIVar);
								return true;
							}
						}
					}
				}
			} else {
				// No additional checks needed...
				inductionVariables.add(newIVar);
				return true;
			}
		}
		return false;
	}
	
	// Returns true if a definition of name exists in a statement between the first and last statement in the loop body
	// determined by the given loopId
	// Returns false otherwise
	private boolean defForNameExistsBetweenStmtsInLoop(Name name, QuadrupletStmt first, QuadrupletStmt last, String loopId) {
		List<LoopQuadrupletStmt> stmtsInLoopBody = loopInvariantGen.getLoopBodyQStmtsList().get(loopId);
		boolean inRegion = false;
		for (LoopQuadrupletStmt lqs : stmtsInLoopBody) {
			if (lqs.getqStmt() == first) {
				inRegion = true;
			}
			if (inRegion) {
				Name dest = lqs.getqStmt().getDestination();
				if (dest.equals(name)) {
					return true;
				}
				if (name.getClass().equals(ArrayName.class)) {
					if (dest.equals(ArrayName.class)) {
						String destId = ((ArrayName)dest).getId();
						Name destIndex = ((ArrayName)dest).getIndex();
						String nameId = ((ArrayName)name).getId();
						Name nameIndex = ((ArrayName)name).getIndex();
						if (destId.equals(nameId)) {
							// If both indices are constants, return true only if the constants are the same
							if (destIndex.getClass().equals(ConstantName.class) && nameIndex.getClass().equals(ConstantName.class)) {
								 if (destIndex.equals(nameIndex)) {
									 return true;
								 }
							} else {
								// Both indices are not constants, any combination can result in a given execution
								return true;
							}
						}
					}
					// Ensure that dest does not play a role in the index of name
					Name nameIndex = name;
					do {
						nameIndex = ((ArrayName)nameIndex).getIndex();
						if (nameIndex.equals(dest)) {
							return true;
						}
					} while (nameIndex.getClass().equals(ArrayName.class));
				}
			}
			if (lqs.getqStmt() == last) {
				return false;
			}
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
	
	public List<InductionVariable> getInductionVariablesForLoop(String loopId) {
		List<InductionVariable> inductionVarsInLoop = new ArrayList<InductionVariable>();
		for (InductionVariable iv : inductionVariables) {
			if (iv.getLqStmt().getLoopBodyBlockId().equals(loopId)) {
				inductionVarsInLoop.add(iv);
			}
		}
		return inductionVarsInLoop;
	}
	
	public LoopInvariantGenerator getLoopInvariantGen() {
		return loopInvariantGen;
	}

	public void setLoopInvariantGen(LoopInvariantGenerator loopInvariantGen) {
		this.loopInvariantGen = loopInvariantGen;
	}
}
