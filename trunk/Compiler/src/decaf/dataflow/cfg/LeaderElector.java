package decaf.dataflow.cfg;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;

public class LeaderElector {
	private HashMap<String, List<LIRStatement>> lirMap;
	private List<String> labelsToMakeLeaders;
	private boolean mergeBoundChecks;
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
	
	public LeaderElector(HashMap<String, List<LIRStatement>> lirMap) {
		this.lirMap = lirMap;
		this.labelsToMakeLeaders = new ArrayList<String>();
	}
	
	public void electLeaders() {
		for (String s: lirMap.keySet()) {
			processMethod(lirMap.get(s));
			labelsToMakeLeaders.clear(); // Clear list
		}
	}

	private void processMethod(List<LIRStatement> list) {
		boolean isFirst = true;
		boolean justSawJump = false;
		boolean inBoundCheck = false;
		for (LIRStatement stmt: list) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt label = (LabelStmt)stmt;
				if (label.getLabelString().matches(LeaderElector.ArrayBeginLabelRegex)) {
					inBoundCheck = true;
				}
				else if (label.getLabelString().matches(LeaderElector.ArrayPassLabelRegex)) {
					inBoundCheck = false;
				}
			}
			
			if (inBoundCheck) continue;
			
			// First statement in method (entry statement)
			if (isFirst) {
				stmt.setIsLeader(true);
				isFirst = false;
			}
			
			// Previous statement was jump
			if (justSawJump) {
				stmt.setIsLeader(true);
				justSawJump = false;
			}
			
			// Mark if statement is a jump statement
			if (stmt.getClass().equals(JumpStmt.class)) {
				justSawJump = true;
				
				JumpStmt jump = (JumpStmt) stmt;
				labelsToMakeLeaders.add(jump.getLabel().getLabelString());
			}
		}
		
		if (!labelsToMakeLeaders.isEmpty()) {
			markLeaderLabel(list);
		}
	}
	
	private void markLeaderLabel(List<LIRStatement> list) {
		for (LIRStatement stmt: list) {
			// If label is target of some jump
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt label = (LabelStmt) stmt;
				if (labelsToMakeLeaders.contains(label.getLabelString())) {
					stmt.setIsLeader(true);
					labelsToMakeLeaders.remove(label.getLabelString());
				}
			}
		}
	}

	public void printLeaders(PrintStream out) {
		for (String s: lirMap.keySet()) {
			out.println("Method " + s + ":");
			for (LIRStatement stmt: lirMap.get(s)) {
				if (stmt.isLeader()) {
					out.println(stmt);
				}
			}
			
			out.println();
		}
	}

	public void setMergeBoundChecks(boolean mergeBoundChecks) {
		this.mergeBoundChecks = mergeBoundChecks;
	}

	public boolean isMergeBoundChecks() {
		return mergeBoundChecks;
	}
}
