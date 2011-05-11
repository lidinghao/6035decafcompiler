package decaf.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;
import decaf.dataflow.cfg.MethodIR;

// Finds loops in the program which don't assign global variables
public class GlobalsDefinedTest {
	HashMap<String, MethodIR> mMap;
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	
	public GlobalsDefinedTest(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;	
	}
	
	// Returns string of loop ids whose loops don't assign global variables
	public List<String> getLoopIDsWhichPass() {
		List<String> uniqueLoopIds = getAllLoopIds();
		List<String> parallelizableLoops = new ArrayList<String>();
		for (String loopId : uniqueLoopIds) {
			if (passesGlobalsDefinedTest(loopId)) {
				parallelizableLoops.add(loopId);
			}
		}
		return parallelizableLoops;
	}
	
	public boolean passesGlobalsDefinedTest(String loopId) {
		int forBodyLabelIndex = getForLabelStmtIndexInMethod(loopId, ForBodyLabelRegex);
		int forEndLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		
		for (int i = forBodyLabelIndex + 1; i < forEndLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				Name dest = ((QuadrupletStmt)stmt).getDestination();
				if (dest.getClass().equals(VarName.class)) {
					int blockId = ((VarName)dest).getBlockId();
					if (blockId == -1) {
						return false;
					}
				}
			}
		}
		return true;
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
