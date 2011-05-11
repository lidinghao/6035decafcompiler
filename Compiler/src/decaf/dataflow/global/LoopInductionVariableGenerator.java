package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ExpressionFlattenerVisitor;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

// Finds all the induction variables in the loops in the program
public class LoopInductionVariableGenerator {
	private HashSet<InductionVariable> inductionVariables;
	private LoopInvariantGenerator loopInvariantGen;
	private HashMap<String, MethodIR> mMap;
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
   private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
   private static String ArrayFailLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.fail";
	
   public LoopInductionVariableGenerator(HashMap<String, MethodIR> mMap) {
   	this.mMap = mMap;
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
					if (isInductionVariable(lqStmt.getqStmt().getDestination(), s)) {
						continue;
					}
					System.out.println("IV checking " + lqStmt);
					if (hasMoreThanOneDefinitionInLoopBody(lqStmt, loopInvariantGen.getQuadrupletStmtsInLoopBody(s))) {
						// More than one definition is not allowed for induction variables
						continue;
					}
					System.out.println("has one definition... continuing");
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
			if (qs.getDestination().equals(dest) && qs != qStmt) {
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
		System.out.println("processing possible basic IV" + lqStmt);
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
							loopInvariantGen.getReachingDefForStmts().get(qStmt), loopQStmts)) {
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
							loopInvariantGen.getReachingDefForStmts().get(qStmt), loopQStmts)) {
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
		System.out.println("processing possible derived IV " + lqStmt);
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
		List<LIRStatement> boundCheckForArg;
		List<LIRStatement> boundCheckForDest = getBoundCheckStmts(qStmt, qStmt.getDestination(), loopId);
		InductionVariable argIVar = getInductionVariable(arg1, inductionVarsInLoop);
		if (argIVar != null) {
			// arg1 is an induction variable
			if (arg2 != null) {
				if (loopInvariantGen.argSatisfiesLoopInvariant(arg2, 
						loopInvariantGen.getReachingDefForStmts().get(qStmt), loopQStmts)) {
					if (qOp == QuadrupletOp.MUL || qOp == QuadrupletOp.ADD || qOp == QuadrupletOp.SUB) {
						newIVar = new InductionVariable(lqStmt);
						newIVar.setFamilyName(argIVar.getFamilyName());
						newIVar.setDerived(true);
						newIVar.setDerivedFrom(argIVar);
						newIVar.setBlockId(loopInvariantGen.getLoopIdToBlockId().get(loopId));
						boundCheckForArg = getBoundCheckStmts(qStmt, arg2, loopId);
						if (qOp == QuadrupletOp.MUL) {
							newIVar.setAdderByMult(arg2, boundCheckForArg);
							newIVar.setMultiplierByMult(arg2, boundCheckForArg);
						} else {
							newIVar.setAdderByAdd(arg2, qOp, boundCheckForArg);
							newIVar.setMultiplier(argIVar.getMultiplier());
						}
						newIVar.setBoundCheckStmtForDest(boundCheckForDest);
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
						newIVar.setAdderByMult(new ConstantName(-1), null);
						newIVar.setMultiplierByMult(new ConstantName(-1), null);
					} else {
						newIVar.setAdder(argIVar.getAdder());
						newIVar.setMultiplier(argIVar.getMultiplier());
					}
					newIVar.setBoundCheckStmtForDest(boundCheckForDest);
				}
			}
		} else {
			argIVar = getInductionVariable(arg2, inductionVarsInLoop);
			if (argIVar != null) {
				// arg2 is induction variable
				// arg1 cannot be null
				if (loopInvariantGen.argSatisfiesLoopInvariant(arg1, 
						loopInvariantGen.getReachingDefForStmts().get(qStmt), loopQStmts)) {
					if (qOp == QuadrupletOp.MUL || qOp == QuadrupletOp.ADD || qOp == QuadrupletOp.SUB) {
						newIVar = new InductionVariable(lqStmt);
						newIVar.setFamilyName(argIVar.getFamilyName());
						newIVar.setDerived(true);
						newIVar.setDerivedFrom(argIVar);
						newIVar.setBlockId(loopInvariantGen.getLoopIdToBlockId().get(loopId));
						boundCheckForArg = getBoundCheckStmts(qStmt, arg2, loopId);
						if (qOp == QuadrupletOp.MUL) {
							newIVar.setAdderByMult(arg1, boundCheckForArg);
							newIVar.setMultiplierByMult(arg1, boundCheckForArg);
						} else {
							newIVar.setAdderByAdd(arg1, qOp, boundCheckForArg);
							newIVar.setMultiplier(argIVar.getMultiplier());
						}
						newIVar.setBoundCheckStmtForDest(boundCheckForDest);
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
				BitSet reachingDefForQStmt = loopInvariantGen.getReachingDefForStmts().get(qStmt);
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
	
	private List<LIRStatement> getBoundCheckStmts(QuadrupletStmt qStmt, Name arg, String loopId) {
		CFGBlock blockWithQStmt = null;
		int indexOfQStmtInBlock = -1;
		pf:
		for (String s : mMap.keySet()) {
			for (CFGBlock block : mMap.get(s).getCfgBlocks()) {
				List<LIRStatement> blockStmts =  block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt == qStmt) {
						blockWithQStmt = block;
						indexOfQStmtInBlock = i;
						break pf;
					}
				}
			}
		}
		return getBoundCheck(arg, blockWithQStmt, indexOfQStmtInBlock);
	}
	
	// Following methods are copied from LoopInvariantOptimizer.java
	
	private List<LIRStatement> getBoundCheck(Name name, CFGBlock block, int stmtIndex) {
		if (name == null) return null;
		if (!name.isArray()) return null;
				
		ArrayName arrName = (ArrayName) name;
		Name index = arrName.getIndex();
		
		boolean inBoundCheck = false;
		boolean inRequiredBC = false;
		int startIndex = -1;
		
		for (int i = stmtIndex; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt)stmt;
				if (lStmt.getLabelString().matches(LoopInductionVariableGenerator.ArrayPassLabelRegex) &&
						getArrayIDFromArrayLabelStmt(lStmt, "pass").equals(arrName.getId())) {
					inBoundCheck = true; // Bound check for right array
				}
			}
			
			if (!inBoundCheck) continue;
			
			if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				if (cStmt.getArg1().equals(index) && !inRequiredBC) {
					inRequiredBC = true;
				}
			}
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt)stmt;
				if (lStmt.getLabelString().matches(LoopInductionVariableGenerator.ArrayBeginLabelRegex)) {
					if (inRequiredBC) {
						startIndex = i;
						break;
					}
					
					inBoundCheck = false;
				}
			}
		}
		if (startIndex == -1) {
			// Nothing was found
			return null;
		}
		List<LIRStatement> stmts = new ArrayList<LIRStatement>();	
		for (int i = startIndex; i < block.getStatements().size(); i++) {
			LIRStatement stmt = block.getStatements().get(i);			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				
				stmts.add(getAlternateLabel(lStmt, block.getMethodName()));
				
				if (lStmt.getLabelString().matches(LoopInductionVariableGenerator.ArrayPassLabelRegex)) {
					break;
				}
				
				continue;
			}
			else if (stmt.getClass().equals(JumpStmt.class)) {
				JumpStmt jStmt = (JumpStmt) stmt;
				JumpStmt newJStmt = new JumpStmt(jStmt.getCondition(), getAlternateLabel(jStmt.getLabel(), block.getMethodName()));
				stmts.add(newJStmt);
				continue;
			}
			
			stmts.add(block.getStatements().get(i));
		}
		
		ExpressionFlattenerVisitor.MAXBOUNDCHECKS++;
		
		return stmts;
	}
	
	private LabelStmt getAlternateLabel(LabelStmt lStmt, String methodName) {
		if (lStmt.getLabelString().matches(LoopInductionVariableGenerator.ArrayPassLabelRegex)) {
			return new LabelStmt(getArrayBoundPass(getArrayIDFromArrayLabelStmt(lStmt, "pass"), methodName));
		}
		else if (lStmt.getLabelString().matches(LoopInductionVariableGenerator.ArrayBeginLabelRegex)) {
			return new LabelStmt(getArrayBoundBegin(getArrayIDFromArrayLabelStmt(lStmt, "begin"), methodName));
		}
		else if (lStmt.getLabelString().matches(LoopInductionVariableGenerator.ArrayFailLabelRegex)) {
			return new LabelStmt(getArrayBoundFail(getArrayIDFromArrayLabelStmt(lStmt, "fail"), methodName));
		}
		
		return lStmt;
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
	
	private String getArrayBoundBegin(String name, String methodName) {
		return methodName + ".array." + name + "." + ExpressionFlattenerVisitor.MAXBOUNDCHECKS + ".begin";
	}
	
	private String getArrayBoundFail(String name, String methodName) {
		return methodName + ".array." + name + "." + ExpressionFlattenerVisitor.MAXBOUNDCHECKS + ".fail";
	}
	
	private String getArrayBoundPass(String name, String methodName) {
		return methodName + ".array." + name + "." + ExpressionFlattenerVisitor.MAXBOUNDCHECKS + ".pass";
	}
	
	// End copy
	
	public LoopInvariantGenerator getLoopInvariantGen() {
		return loopInvariantGen;
	}

	public void setLoopInvariantGen(LoopInvariantGenerator loopInvariantGen) {
		this.loopInvariantGen = loopInvariantGen;
	}
}
