package decaf.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.MethodIR;

public class ParallelizationOptimizer {
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	private HashMap<String, MethodIR> mMap;
	private ProgramFlattener pf;
	
	public ParallelizationOptimizer(HashMap<String, MethodIR> mMap, ProgramFlattener pf) {
		this.mMap = mMap;
		this.pf = pf;
	}
	
	public void performParallelization() {
		ArrayIndexResolverTest arrResTest = new ArrayIndexResolverTest(mMap);
		GlobalsDefinedTest globalTest = new GlobalsDefinedTest(mMap);
		NamesDefinedTest namesTest = new NamesDefinedTest(mMap);
		MethodCallsTest methodTest = new MethodCallsTest(mMap);
		List<String> arrResPass = arrResTest.getLoopIDsWhichPass();
		List<String> globalPass = globalTest.getLoopIDsWhichPass();
		List<String> namesPass = namesTest.getLoopIDsWhichPass();
		List<String> methodPass = methodTest.getLoopIDsWhichPass();
		List<String> parallelizable = new ArrayList<String>();
		for (String loopId : getAllLoopIds()) {
			if (arrResPass.contains(loopId) && globalPass.contains(loopId) 
					&& namesPass.contains(loopId) && methodPass.contains(loopId)) {
				parallelizable.add(loopId);
			}
		}
		System.out.println("PARALLELIZABLE LOOPS: " + parallelizable);
		if (parallelizable.size() > 0) {
			// The loopIds which we actually parallelize
			List<String> parallelize = new ArrayList<String>();
			parallelize.addAll(parallelizable);
			// Only parallelize the outermost loops if multiple nested loops are parallelizable
			for (String loopId : parallelizable) {
				List<String> childLoopIds = getChildLoopIds(loopId);
				parallelize.removeAll(childLoopIds);
			}
			System.out.println("PARALELLIZED LOOPS: " + parallelize);
			LoopParallelizer lp = new LoopParallelizer(mMap, parallelize, pf);
			lp.parallelize();
		}
	}
	
	// Returns the list of child loop ids for the given loop id
	public List<String> getChildLoopIds(String loopId) {
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		int forInitLabelIndex = getForLabelStmtIndexInMethod(loopId, ForInitLabelRegex);
		int forEndLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		List<String> childLoopIds = new ArrayList<String>();
		String forLabel;
		
		for (int i = forInitLabelIndex + 1; i < forEndLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(LabelStmt.class)) {
				forLabel = ((LabelStmt)stmt).getLabelString();
				if (forLabel.matches(ForInitLabelRegex)) {
					childLoopIds.add(getIdFromForLabel(forLabel));
				}
			}
		}
		return childLoopIds;
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
	
	private String getIdFromForLabel(String label) {
		String[] forInfo = label.split("\\.");
		return forInfo[0] + "." + forInfo[1];
	}
}
