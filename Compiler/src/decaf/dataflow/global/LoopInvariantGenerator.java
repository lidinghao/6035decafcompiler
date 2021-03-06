package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.dataflow.cfg.MethodIR;

// Finds loop invariant statements in loops
// A loop invariant statement (t = a1 op a2) satifies one of the following conditions:
// 1. ai is a constant
// 2. all definitions of ai that reach the statement are outside the loop

// Uses LoopQuadrupletStmt, a container class which stores QuadrupletStmt and CFGBlock id 
// of the loop body block which the QuadrupletStmt is in

public class LoopInvariantGenerator {
	private HashMap<String, MethodIR> mMap;
	// Map from QuadrupletStmt id to LoopQuadrupletStmt
	private HashSet<LoopQuadrupletStmt> loopInvariantStmts;
	// Map from QuadrupletStmt to BitSet representing all the QuadrupletStmt IDs which reach that point
	private HashMap<LIRStatement, BitSet> reachingDefForStmts;
	// Map from loop body id to a HashSet of all the LoopQuadrupletStmts in that loop body
	private HashMap<String, HashSet<LoopQuadrupletStmt>> allLoopBodyQStmts;
	private HashMap<String, List<LoopQuadrupletStmt>> loopBodyQStmtsList;
	// Map from loop id to block id
	private HashMap<String, Integer> loopIdToBlockId;

	// This optimizer isn't related to LoopInvariant optimizations, but it updates the Reaching definitions
	// which we need for loop optimizations
	private GlobalConstantPropagationOptimizer gcp;
	private static String ForIncrLabelRegex = "[a-zA-z_]\\w*.for\\d+.incr";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	
	public LoopInvariantGenerator(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.loopInvariantStmts = new HashSet<LoopQuadrupletStmt>();
		this.loopBodyQStmtsList = new HashMap<String, List<LoopQuadrupletStmt>>();
		this.allLoopBodyQStmts = new HashMap<String, HashSet<LoopQuadrupletStmt>>();
		this.loopIdToBlockId = new HashMap<String, Integer>();
		this.gcp = new GlobalConstantPropagationOptimizer(mMap);
	}
	
	public void generateLoopInvariants() {
		// Generates the Map of QStmt -> BitSet representing all the QStmts IDs which reach at that point
		gcp.generateReachingDefsForQStmts();
		reachingDefForStmts = gcp.getReachingDefForStmts();
		generateLoopBodyQuadrupletStmts();
		// Keep loop until no more loop invariants are added
		int numLoopInvariants;
		do {
			numLoopInvariants = loopInvariantStmts.size();
			for (String loopBodyId : allLoopBodyQStmts.keySet()) {
				for (LoopQuadrupletStmt lQStmt : allLoopBodyQStmts.get(loopBodyId)) {
					if (isLoopInvariant(lQStmt)) {
						loopInvariantStmts.add(lQStmt);
					}
				}
			}
		} while (numLoopInvariants != loopInvariantStmts.size());
		
		System.out.println("LOOP INVARIANT STMTS: " + loopInvariantStmts);
	}
	
	// Returns a map which maps a loop CFGBlock to all the QuadrupletStmts in that block
	private void generateLoopBodyQuadrupletStmts() {
		String forLabel;
		HashSet<LoopQuadrupletStmt> loopStmts;
		List<LoopQuadrupletStmt> loopStmtsList;
		for (String s : mMap.keySet()) {
			boolean inFor = false;
			List<String> forIdList = new ArrayList<String>();
			List<LIRStatement> stmts = mMap.get(s).getStatements();
			for (int i = 0; i < stmts.size(); i++) {
				LIRStatement stmt = stmts.get(i);
				if (stmt.getClass().equals(LabelStmt.class)) {
					forLabel = ((LabelStmt)stmt).getLabelString();
					if (forLabel.matches(ForIncrLabelRegex)) {
						// Set block id from the next statement which initialized loop var
						QuadrupletStmt initStmt = (QuadrupletStmt)stmts.get(i+1);
						int blockId = ((VarName)initStmt.getDestination()).getBlockId();
						loopIdToBlockId.put(getIdFromForLabel(forLabel), blockId);
					} else if (forLabel.matches(ForEndLabelRegex)) {
						forIdList.remove(forIdList.size()-1);
					} else  if (forLabel.matches(ForBodyLabelRegex)) {
						// Update map of body label to its stmt index
						forIdList.add(getIdFromForLabel(forLabel));
					}
					inFor = !forIdList.isEmpty();
					continue;
				} else if (inFor) { 
					if (stmt.getClass().equals(QuadrupletStmt.class)) {
						// Add the QuadrupletStmt to all the loops in the list
						for (String forId : forIdList) {
							if (!allLoopBodyQStmts.containsKey(forId)) {
								loopStmts = new HashSet<LoopQuadrupletStmt>();
								loopStmtsList = new ArrayList<LoopQuadrupletStmt>();
								loopBodyQStmtsList.put(forId, loopStmtsList);
								allLoopBodyQStmts.put(forId, loopStmts);
							} else {
								loopStmtsList = loopBodyQStmtsList.get(forId);
								loopStmts = allLoopBodyQStmts.get(forId);
							}
							LoopQuadrupletStmt lqs = new LoopQuadrupletStmt((QuadrupletStmt)stmt, forId, i);
							loopStmts.add(lqs);
							loopStmtsList.add(lqs);
						}
					}
				}
			}
		}
	}
	
	// Returns true if the LoopQuadrupletStmt is a loop invariant stmt, False otherwise
	private boolean isLoopInvariant(LoopQuadrupletStmt loopQStmt) {
		// Ignore statements which assign or use registers
		if (usesRegisters(loopQStmt.getqStmt()))
			return false;
		System.out.println("processing " + loopQStmt.getqStmt());
		QuadrupletStmt qStmt = loopQStmt.getqStmt();
		HashSet<QuadrupletStmt> loopQStmts = getQuadrupletStmtsInLoopBody(loopQStmt.getLoopBodyBlockId());
		BitSet reachingDefForQStmt = reachingDefForStmts.get(qStmt);
		Name arg1 = qStmt.getArg1();
		Name arg2 = qStmt.getArg2();
		// If the dest is array name, check its index argument
		if (qStmt.getDestination().getClass().equals(ArrayName.class)) {
			Name index = ((ArrayName)qStmt.getDestination()).getIndex();
			if (!argSatisfiesLoopInvariant(index, reachingDefForQStmt, loopQStmts)) {
				return false;
			}
		}
		if (arg1 != null) {
			if (argSatisfiesLoopInvariant(arg1, reachingDefForQStmt, loopQStmts)) {
				if (arg2 != null) {
					if (argSatisfiesLoopInvariant(arg2, reachingDefForQStmt, loopQStmts)) {
						// Both args satisify loop invariant properties
						return true;
					}
				} else {
					// arg1 satisifies loop invariant properties, arg2 doesn't exist
					return true;
				}
			}
		} else {
			// Neither arg1 nor arg2 exists
			// Assumes that if arg1 doesn't exist, arg2 cannot exist
			return true;
		}
		return false;
	}
	
	public boolean argSatisfiesLoopInvariant(Name arg, BitSet reachingDefForQStmt, 
			HashSet<QuadrupletStmt> loopQStmts) {
		boolean argSatisfiesLoopInvariant = false;
		System.out.println("FOR ARG: " + arg);
		// Check if arg is a constant
		if (arg.getClass().equals(ConstantName.class)) {
			argSatisfiesLoopInvariant = true;
		} else {
			// Get all possible definitions for the arg
			List<QuadrupletStmt> defsForArg = gcp.getDefinitionsForName(arg);
			if (defsForArg != null) {
				List<QuadrupletStmt> reachingDefsForArg = new ArrayList<QuadrupletStmt>();
				// Use BitSet to generate list of reaching definitions of the arg
				for (QuadrupletStmt qs : defsForArg) {
					if (reachingDefForQStmt.get(qs.getMyId())) {
						reachingDefsForArg.add(qs);
					}
				}
				// Check if all defs which are reaching are outside the loop body
				argSatisfiesLoopInvariant = true;
				for (QuadrupletStmt reachingQStmt : reachingDefsForArg) {
					if (loopQStmts.contains(reachingQStmt)) {
						// A reaching definition is in the loop
						argSatisfiesLoopInvariant = false;
					}
				}
			} else {
				// No definitions for arg - can be a global arg which is never assigned
				argSatisfiesLoopInvariant = true;
			}
		}
		// If arg is ArrayName, ensure that the index also satisfies loop invariant properties
		if (arg.getClass().equals(ArrayName.class)) {
			boolean indexSatisifed = argSatisfiesLoopInvariant(((ArrayName)arg).getIndex(), 
					reachingDefForQStmt, loopQStmts);
			return indexSatisifed && argSatisfiesLoopInvariant;
		}
		return argSatisfiesLoopInvariant;
	}
	
	public HashSet<QuadrupletStmt> getQuadrupletStmtsInLoopBody(String forId) {
		HashSet<QuadrupletStmt> loopQStmts = new HashSet<QuadrupletStmt>();
		HashSet<LoopQuadrupletStmt> blockLoopQStmts = allLoopBodyQStmts.get(forId);
		for (LoopQuadrupletStmt lqs : blockLoopQStmts) {
			loopQStmts.add(lqs.getqStmt());
		}
		return loopQStmts;
	}
	
	private boolean usesRegisters(QuadrupletStmt qStmt) {
		Name dest = qStmt.getDestination();
		Name arg1 = qStmt.getArg1();
		Name arg2 = qStmt.getArg2();
		if (dest != null) {
			if (dest.getClass().equals(RegisterName.class)) {
				return true;
			}
		}
		if (arg1 != null) {
			if (arg1.getClass().equals(RegisterName.class)) {
				return true;
			}
		}
		if (arg2 != null) {
			if (arg2.getClass().equals(RegisterName.class)) {
				return true;
			}
		}
		return false;
	}
	
	private String getIdFromForLabel(String label) {
		String[] forInfo = label.split("\\.");
		return forInfo[0] + "." + forInfo[1];
	}
	
	public HashSet<LoopQuadrupletStmt> getLoopInvariantStmts() {
		return loopInvariantStmts;
	}

	public void setLoopInvariantStmts(
			HashSet<LoopQuadrupletStmt> loopInvariantStmts) {
		this.loopInvariantStmts = loopInvariantStmts;
	}
	
	public HashMap<String, HashSet<LoopQuadrupletStmt>> getAllLoopBodyQStmts() {
		return allLoopBodyQStmts;
	}

	public void setAllLoopBodyQStmts(
			HashMap<String, HashSet<LoopQuadrupletStmt>> allLoopBodyQStmts) {
		this.allLoopBodyQStmts = allLoopBodyQStmts;
	}
	
	public HashMap<LIRStatement, BitSet> getReachingDefForStmts() {
		return reachingDefForStmts;
	}

	public void setReachingDefForStmts(
			HashMap<LIRStatement, BitSet> reachingDefForQStmts) {
		this.reachingDefForStmts = reachingDefForQStmts;
	}

	public GlobalConstantPropagationOptimizer getGcp() {
		return gcp;
	}

	public void setGcp(GlobalConstantPropagationOptimizer gcp) {
		this.gcp = gcp;
	}

	public HashMap<String, List<LoopQuadrupletStmt>> getLoopBodyQStmtsList() {
		return loopBodyQStmtsList;
	}

	public void setLoopBodyQStmtsList(
			HashMap<String, List<LoopQuadrupletStmt>> loopBodyQStmtsList) {
		this.loopBodyQStmtsList = loopBodyQStmtsList;
	}

	public HashMap<String, Integer> getLoopIdToBlockId() {
		return loopIdToBlockId;
	}

	public void setLoopIdToBlockId(HashMap<String, Integer> loopIdToBlockId) {
		this.loopIdToBlockId = loopIdToBlockId;
	}
}
