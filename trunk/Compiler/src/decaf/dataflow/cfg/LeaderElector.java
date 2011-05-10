package decaf.dataflow.cfg;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LeaveStmt;
import decaf.codegen.flattener.ProgramFlattener;

public class LeaderElector {
	private ProgramFlattener pf;
	private List<String> labelsToMakeLeaders;
	private boolean mergeBoundChecks;
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
	private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
	
	public LeaderElector(ProgramFlattener pf) {
		this.pf = pf;
		this.labelsToMakeLeaders = new ArrayList<String>();
	}
	
	public void electLeaders() {
		for (String s: pf.getLirMap().keySet()) {
			processMethod(pf.getLirMap().get(s));
			labelsToMakeLeaders.clear(); // Clear list
		}
	}

	private void processMethod(List<LIRStatement> list) {
		boolean isFirst = true;
		boolean justSawJump = false;
		boolean inBoundCheck = false;
		boolean justSawReturn = false;
		
		for (LIRStatement stmt: list) {
			stmt.setIsLeader(false);

			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt label = (LabelStmt)stmt;
				if (label.getLabelString().matches(LeaderElector.ArrayBeginLabelRegex)) {
					inBoundCheck = true;
				}
				else if (label.getLabelString().matches(LeaderElector.ArrayPassLabelRegex)) {
					inBoundCheck = false;
				}
			}
			
			if (this.mergeBoundChecks && inBoundCheck) continue;
			
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
			
			if (justSawReturn) {
				stmt.setIsLeader(true);
				justSawReturn = false;
			}
			
			// Mark if statement is a jump statement
			if (stmt.getClass().equals(JumpStmt.class)) {
				justSawJump = true;
				
				JumpStmt jump = (JumpStmt) stmt;
				labelsToMakeLeaders.add(jump.getLabel().getLabelString());
			}
			
			if (stmt.getClass().equals(LeaveStmt.class)) {
				justSawReturn = true;
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
		for (String s: pf.getLirMap().keySet()) {
			out.println("Method " + s + ":");
			for (LIRStatement stmt: pf.getLirMap().get(s)) {
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
