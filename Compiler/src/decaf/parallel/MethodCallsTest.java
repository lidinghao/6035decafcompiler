package decaf.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.MethodIR;

// Finds loops which don't make method calls (but callout is okay)
public class MethodCallsTest {
	HashMap<String, MethodIR> mMap;
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	private static String ForIncrLabelRegex = "[a-zA-z_]\\w*.for\\d+.incr";
	
	public MethodCallsTest(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;	
	}
	
	// Returns string of loop ids whose loops don't make method calls (callouts okay)
	public List<String> getLoopIDsWhichPass() {
		List<String> uniqueLoopIds = getAllLoopIds();
		List<String> parallelizableLoops = new ArrayList<String>();
		List<String> unParallelizableLoops = new ArrayList<String>();
		for (String loopId : uniqueLoopIds) {
			if (passesMethodCallsTest(loopId)) {
				parallelizableLoops.add(loopId);
			} else {
				unParallelizableLoops.add(loopId);
			}
		}
		System.out.println("LOOPS WHICH PASS METHOD CALLS TEST: " + parallelizableLoops);
		System.out.println("LOOPS WHICH FAIL METHOD CALLS TEST: " + unParallelizableLoops);
		return parallelizableLoops;
	}
	
	public boolean passesMethodCallsTest(String loopId) {
		int forBodyLabelIndex = getForLabelStmtIndexInMethod(loopId, ForBodyLabelRegex);
		int forIncrLabelIndex = getForLabelStmtIndexInMethod(loopId, ForIncrLabelRegex);
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		
		for (int i = forBodyLabelIndex + 1; i < forIncrLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(CallStmt.class)) {
				String methodLabel = ((CallStmt)stmt).getMethodLabel();
				if (methodLabel.equals(ProgramFlattener.exceptionHandlerLabel)) {
					continue;
				}
				// If methodLabel does not start with '"', it is method call
				if (!methodLabel.startsWith("\"")) {
					return false;
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
