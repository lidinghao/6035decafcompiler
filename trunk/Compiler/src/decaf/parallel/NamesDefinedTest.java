package decaf.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;
import decaf.dataflow.cfg.MethodIR;

// Finds loops which only assign to variables which are local or in a more nested loop
public class NamesDefinedTest {
	HashMap<String, MethodIR> mMap;
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	
	public NamesDefinedTest(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;	
	}
	
	// Returns string of loop ids whose loops don't assign to variables 
	// which are local to the loop or nested loop
	public List<String> getLoopIDsWhichPass() {
		List<String> uniqueLoopIds = getAllLoopIds();
		List<String> parallelizableLoops = new ArrayList<String>();
		List<String> unParallelizableLoops = new ArrayList<String>();
		for (String loopId : uniqueLoopIds) {
			if (passesNamesDefinedTest(loopId)) {
				parallelizableLoops.add(loopId);
			} else {
				unParallelizableLoops.add(loopId);
			}
		}
		System.out.println("LOOPS WHICH PASS NAMES DEF TEST: " + parallelizableLoops);
		System.out.println("LOOPS WHICH FAIL NAMES DEF TEST: " + unParallelizableLoops);
		return parallelizableLoops;
	}
	
	public boolean passesNamesDefinedTest(String loopId) {
		int forBodyLabelIndex = getForLabelStmtIndexInMethod(loopId, ForBodyLabelRegex);
		int forEndLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		
		//System.out.println("NAME DEF TEST: " + loopId);
		
		boolean ignore = false;
		String forLabel;
		for (int i = forBodyLabelIndex + 1; i < forEndLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(LabelStmt.class)) {
				forLabel = ((LabelStmt)stmt).getLabelString();
				if (forLabel.matches(ForInitLabelRegex)) {
					// Ignore nested for init and test blocks, only care about statements from body to end
					// We will globalize the non-loop-local variables used in init and test later
					ignore = true;
				} else if (forLabel.matches(ForBodyLabelRegex)) {
					ignore = false;
				}
				continue;
			}
			if (ignore) {
				continue;
			}
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				if (validStmt(stmt, loopId).size() != 0) {
					return false;
				}
			}
		}
		return true;
	}
	
	// Return empty list if we assign or use a Name whose blockId is allowed by the loop,
	// Returns the list of faulty Names otherwise
	public List<Name> validStmt(LIRStatement stmt, String loopId) {
		List<Integer> loopIdBlockIds = blockIdsForLoop(loopId);
		List<Name> invalid = new ArrayList<Name>();
		Name dest = null, arg1 = null, arg2 = null;
		if (stmt.getClass().equals(QuadrupletStmt.class)) {
			dest = ((QuadrupletStmt)stmt).getDestination();
			arg1 = ((QuadrupletStmt)stmt).getArg1();
			arg2 = ((QuadrupletStmt)stmt).getArg2();
		} else if (stmt.getClass().equals(CmpStmt.class)) {
			arg1 = ((CmpStmt)stmt).getArg1();
			arg2 = ((CmpStmt)stmt).getArg2();
		} else if (stmt.getClass().equals(PopStmt.class)) {
			arg1 = ((PopStmt)stmt).getName();
		} else if (stmt.getClass().equals(PushStmt.class)) {
			arg1 = ((PushStmt)stmt).getName();
		}
		if (dest != null) {
			if (!validArg(dest, loopIdBlockIds)) {
				invalid.add(dest);
			}
		}
		if (arg1 != null) {
			if (!validArg(arg1, loopIdBlockIds)) {
				invalid.add(arg1);
			}
		}
		if (arg2 != null) {
			if (!validArg(arg2, loopIdBlockIds)) {
				invalid.add(arg2);
			}
		}
		return invalid;
	}
	
	public boolean validArg(Name arg, List<Integer> loopIdBlockIds) {
		if (arg.getClass().equals(VarName.class)) {
			int blockId = ((VarName)arg).getBlockId();
			if (!loopIdBlockIds.contains(blockId) && blockId != -1) {
				return false;
			}
		}
		return true;
	}
	
	// Returns the list of block ids for which we are allowed to define variables in the loop
	// to maintain parallelization opportunity
	public List<Integer> blockIdsForLoop(String loopId) {
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		int forInitLabelIndex = getForLabelStmtIndexInMethod(loopId, ForInitLabelRegex);
		int forEndLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		List<Integer> loopBlockIds = new ArrayList<Integer>();
		String forLabel;
		
		for (int i = forInitLabelIndex; i < forEndLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(LabelStmt.class)) {
				forLabel = ((LabelStmt)stmt).getLabelString();
				if (forLabel.matches(ForInitLabelRegex)) {
					// Get next statement, which defines the loop variable
					Name dest = ((QuadrupletStmt)methodStmts.get(i+1)).getDestination();
					while (!dest.getClass().equals(VarName.class)) {
						i++;
						dest = ((QuadrupletStmt)methodStmts.get(i+1)).getDestination();
					}
					// Add block id of loop variable
					int blockId = ((VarName)dest).getBlockId();
					loopBlockIds.add(blockId);
				}
			}
		}
		return loopBlockIds;
	}
	
	private List<String> getAllLoopIds() {
		List<String> uniqueLoopIds = new ArrayList<String>();
		for (String s : mMap.keySet()) {
			for (LIRStatement stmt : mMap.get(s).getStatements()) {
				if (stmt.getClass().equals(LabelStmt.class)) {
					LabelStmt lStmt = (LabelStmt)stmt;
					String labelStr = lStmt.getLabelString();
					if (labelStr.matches(ForBodyLabelRegex)) {
						uniqueLoopIds.add(getIdFromForLabel(labelStr));
					}
				}
			}
		}
		return uniqueLoopIds;
	}
	
	private int getForLabelStmtIndexInMethod(String loopId, String label) {
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		for (int i = 0; i < methodStmts.size(); i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt)stmt;
				String labelStr = lStmt.getLabelString();
				if (labelStr.matches(label)) {
					// Label points to this for loop's id
					if (getIdFromForLabel(labelStr).equals(loopId)) {
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	private String getIdFromForLabel(String label) {
		String[] forInfo = label.split("\\.");
		return forInfo[0] + "." + forInfo[1];
	}
}
