package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.dataflow.cfg.MethodIR;

public class LoopStrengthReductionOptimizer {
	private HashMap<String, MethodIR> mMap;
	private LoopInductionVariableGenerator loopInductionVarGen;
	private LoopInvariantGenerator loopInvarGen;
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	private static String ForTestLabelRegex = "[a-zA-z_]\\w*.for\\d+.test";
	
	public LoopStrengthReductionOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.loopInductionVarGen = new LoopInductionVariableGenerator(mMap);
		this.loopInvarGen = this.loopInductionVarGen.getLoopInvariantGen();
	}
	
	public void performStrengthReductionOptimization() {
		loopInductionVarGen.generateInductionVariables();
		for (String loopId : loopInvarGen.getAllLoopBodyQStmts().keySet()) {
			String[] loopInfo = loopId.split("\\.");
			List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
			List<InductionVariable> loopIVars = 
				loopInductionVarGen.getInductionVariablesForLoop(loopId);
			// Find the index for the for test label since we will add 
			// the initialization statements above this point
			int testLabelIndex = getForLabelStmtIndexInMethod(loopId, ForTestLabelRegex);
			// Note: If a jmp to body statement exists about the test label, 
			// we must move it to after the initialization statements
			LIRStatement aboveTestLabel = methodStmts.get(testLabelIndex-1);
			LIRStatement initBodyJmpStmt = null;
			if (aboveTestLabel.getClass().equals(JumpStmt.class)) {
				JumpStmt jStmt = (JumpStmt)aboveTestLabel;
				String label = jStmt.getLabel().getLabelString();
				if (label.matches(ForBodyLabelRegex)) {
					// Label points to this for loop's id
					if (getIdFromForLabel(label).equals(loopId)) {
						initBodyJmpStmt = methodStmts.remove(testLabelIndex-1);
					}
				}
			}
			// Find the index of the for body label since we will be adding 
			// the i <- i' statements right afterwards
			int bodyLabelIndex = getForLabelStmtIndexInMethod(loopId, ForBodyLabelRegex);
			// Find the basic induction variables
			List<InductionVariable> basicLoopIVars = new ArrayList<InductionVariable>();
			for (InductionVariable i : loopIVars) {
				if (!i.isDerived()) {
					basicLoopIVars.add(i);
				}
			}
			// Create the basic induction variable to derived induction variable map
			HashMap<InductionVariable, List<InductionVariable>> basicToDerived = 
				new HashMap<InductionVariable, List<InductionVariable>>();
			List<InductionVariable> derivedList;
			for (InductionVariable basicIVar : basicLoopIVars) {
				if (!basicToDerived.containsKey(basicIVar)) {
					basicToDerived.put(basicIVar, new ArrayList<InductionVariable>());
				}
				derivedList = basicToDerived.get(basicIVar);
				for (InductionVariable otherIVar : loopIVars) {
					if (otherIVar.isDerived() && otherIVar.
							getFamilyName().equals(basicIVar.getVariable())) {
						derivedList.add(otherIVar);
					}
				}
			}
			// Add the derived induction variable statements one by one after the
			// definition of the associated basic induction variable
			for (InductionVariable basicIVar : basicLoopIVars) {
				int basicIVarStmt = getStmtIndexInMethod(basicIVar.getLqStmt().
						getLoopBodyBlockId(), basicIVar.getLqStmt().getqStmt());
				List<InductionVariable> canAdd = new ArrayList<InductionVariable>();
				for (InductionVariable derivedIVar : basicToDerived.get(basicIVar)) {
					if (derivedIVar.getDerivedFrom() == basicIVar) {
						canAdd.add(derivedIVar);
					}
					//while (derivedIVar)
				}
			}
		}
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
	
	private int getStmtIndexInMethod(String loopId, LIRStatement methodStmt) {
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		for (int i = 0; i < methodStmts.size(); i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt == methodStmt) {
				return i;
			}
		}
		System.out.println("STMT NOT FOUND IN METHOD!!");
		return -1;
	}
	
	private String getIdFromForLabel(String label) {
		String[] forInfo = label.split("\\.");
		return forInfo[0] + "." + forInfo[1];
	}
}
