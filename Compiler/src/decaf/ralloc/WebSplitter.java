package decaf.ralloc;

import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class WebSplitter {
	private HashMap<String, MethodIR> mMap;
	private String currentMethod;
	private List<Web> potentialWebs;
	private CFGBlock programPointBlock;
	private int programPointIndex;
	private ReachingDefinitions reachingDefinitions;
	private LivenessAnalysis livenessAnalysis;
	
	public WebSplitter(HashMap<String, MethodIR> mMap, ReachingDefinitions reachingDefinitions, LivenessAnalysis livenessAnalysis) {
		this.mMap = mMap;
		this.reachingDefinitions = reachingDefinitions;
		this.livenessAnalysis = livenessAnalysis;
	}
	
	public void setState(String currentMethod, List<Web> potentialWebs) {
		this.currentMethod = currentMethod;
		this.potentialWebs = potentialWebs;
	}
	
	public void split() {
		System.out.println("SPLITTING...");
		// Identify program point with > N webs (in split list)
		identifyProgramPoint();
		
		// Select web to split
		
		// Split at right point
		System.out.println("SPLITTING COMPLETE!");
	}

	private void identifyProgramPoint() {
		for (CFGBlock block: this.mMap.get(this.currentMethod).getCfgBlocks()) {
			for (int i = 0; i < block.getStatements().size(); i++) {
				LIRStatement stmt = block.getStatements().get(i);
				
				if (!areVarsLive(stmt)) continue;
				if (!doDefsReach(stmt)) continue;
				
				// stmt is the required program point
				System.out.println("PROGRAM POINT FOUND: " + stmt + " in  block: " + block.getIndex());
				
				this.programPointBlock = block;
				this.programPointIndex = i;
			}
		}
		
		System.out.println("PROGRAM POINT SEARCH FAILED!");
	}
	
	private boolean doDefsReach(LIRStatement stmt) {
		List<LIRStatement> globalDefs = this.reachingDefinitions.getUniqueDefinitions().get(this.currentMethod);
		
		for (Web w: this.potentialWebs) {
			boolean defFound = false;
			
			for (LIRStatement def: w.getDefinitions()) {				
				int index;
				
				if (def.getClass().equals(QuadrupletStmt.class)) {
					index = ((QuadrupletStmt)stmt).getMyId();
				}
				else {
					index = ((LoadStmt)stmt).getMyId();
				}
				
				if (stmt.getReachingDefInSet().get(index)) { // At least one def should reach
					defFound = true;
				}
			}
			
			if (!defFound) return false;
		}
		
		return true;
	}

	private boolean areVarsLive(LIRStatement stmt) {
		List<Name> uniqueVars = this.livenessAnalysis.getUniqueVariables().get(this.currentMethod);
		
		for (Web w: this.potentialWebs) {
			Name name = w.getVariable();
			int index = uniqueVars.indexOf(name);
			
			if (!stmt.getLiveInSet().get(index)) return false;
		}
		
		return true;
	}
}
