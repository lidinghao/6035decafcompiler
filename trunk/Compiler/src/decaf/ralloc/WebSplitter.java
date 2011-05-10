package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.StoreStmt;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class WebSplitter {
	private HashMap<String, MethodIR> mMap;
	private String currentMethod;
	private List<Web> potentialWebs;
	private int programPointIndex;
	private ReachingDefinitions reachingDefinitions;
	private LivenessAnalysis livenessAnalysis;
	private HashMap<Web, Integer[]> splitPoints;
	
	public WebSplitter(HashMap<String, MethodIR> mMap, ReachingDefinitions reachingDefinitions, LivenessAnalysis livenessAnalysis) {
		this.mMap = mMap;
		this.reachingDefinitions = reachingDefinitions;
		this.livenessAnalysis = livenessAnalysis;
		this.splitPoints = new HashMap<Web, Integer[]>();
	}
	
	public void setState(String currentMethod, List<Web> potentialWebs) {
		this.currentMethod = currentMethod;
		this.potentialWebs = potentialWebs;
	}
	
	public void split() {
		System.out.println("SPLITTING...");
		// Identify program point with > N webs (in split list)
		if (!identifyProgramPoint()) {
			System.out.println("ERROR: PROGRAM POINT SEARCH FAILED!");
			
			return;
		}
		
		// Calculate maximal split points for each web
		generateSplitPoints();
		
		// Select web to split (from potentialWebs)
		Web splitWeb = this.selectWebToSplit();
		
		
		// Split at right point
		for (Web w: this.splitPoints.keySet()) {
			System.out.println("SPLIT PTS: " + w.getIdentifier() + " :: [" + this.splitPoints.get(w)[0] +", " + this.splitPoints.get(w)[1] + "]");
		}
		
		Name var = splitWeb.getVariable();
		StoreStmt store = new StoreStmt(var);
		LoadStmt load = new LoadStmt(var);
		
		// Insert store (in CFG)
		insertStore(store, this.splitPoints.get(splitWeb)[0]);
		
		// Insert load (in CFG)
		insertLoad(load, this.splitPoints.get(splitWeb)[1]);
		
		this.mMap.get(this.currentMethod).regenerateStmts();
		
		//System.out.println(mMap.get(this.currentMethod).getStatements());
		
		// Done
		System.out.println("SPLITTING COMPLETE!");
	}
	
	private void insertLoad(LoadStmt load, int i) {
		LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
		CFGBlock block = getBlockWithStmtIndex(i);
		
		int index = 0;
		for (; index < block.getStatements().size(); index++) {
			if (block.getStatements().get(index) == stmt) {
				break;
			}
		}
		
		block.getStatements().add(index, load);
	}
	
	private void insertStore(StoreStmt store, int i) {
		LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
		CFGBlock block = getBlockWithStmtIndex(i);
		
		int index = 0;
		for (; index < block.getStatements().size(); index++) {
			if (block.getStatements().get(index) == stmt) {
				break;
			}
		}
		
		block.getStatements().add(index+1, store);
	}

	private Web selectWebToSplit() {
		this.generateSplitPoints();
		
		Web splitWeb = null;
		float minHeuristic = 2147483647;
		
		for (Web w: this.splitPoints.keySet()) {
			Integer[] pt = this.splitPoints.get(w);
			int dist = pt[2] - pt[0];
			int depth = pt[1] + pt[3]; // Add depth?
			float heuristic = calculateHeuristic(dist, depth);
			if (heuristic < minHeuristic) {
				minHeuristic = heuristic;
				splitWeb = w;
			}
		}
		
		return splitWeb;
	}
	
	private float calculateHeuristic(int distance, int depth) {
		return (float) (0.5 * distance + 0.5 * depth);
	}

	private void generateSplitPoints() {
		this.splitPoints.clear();
		
		for (Web w: this.potentialWebs) {
			this.splitPoints.put(w, generateSplitPointForWeb(w));
		}
	}

	// Return index of the two uses (in pf)!
	private Integer[] generateSplitPointForWeb(Web w) {
		Integer[] indices = new Integer[4]; // first use, first use depth, last use, last use depth
		
		CFGBlock start = findProgramPointBlock();
		int previousDefUseIndex = findPreviousDefUse(w);
		CFGBlock previousUseBlock = getBlockWithStmtIndex(previousDefUseIndex);
		int nextUseIndex = findNextUse(w);
		CFGBlock nextUseBlock = getBlockWithStmtIndex(nextUseIndex);
		
		if (previousUseBlock.equals(start)) {
			indices[0] = previousDefUseIndex;
			indices[1] = start.getStatements().get(0).getDepth(); // Same depth as block
		}
		else {
			// Find block, index to insert store in
		}
		
		if (nextUseBlock.equals(start)) {
			indices[2] = nextUseIndex;
			indices[3] = start.getStatements().get(0).getDepth(); // Same depth as block
		}
		else {
			// Find block, index to insert load in
		}
		
		
		indices[0] = previousDefUseIndex;
		indices[1] = nextUseIndex;
		
		return indices;
	}

	private CFGBlock getBlockWithStmtIndex(int i) {
		LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
		
		for (CFGBlock block: this.mMap.get(this.currentMethod).getCfgBlocks()) {
			for (LIRStatement s: block.getStatements()) {
				if (s == stmt) return block; // == or .equals()?
			}
		}
		
		return null;
	}

	private int findNextUse(Web w) {
		int i = this.programPointIndex + 1;
		
		while (i < this.mMap.get(this.currentMethod).getStatements().size()) {
			LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
			
			for (LIRStatement use : w.getUses()) {
				if (stmt == use) { // == or .equals()?
					return i;
				}
			}
			
			i++;
		}
		
		return -1;
	}

	private int findPreviousDefUse(Web w) {
		int i = this.programPointIndex;
		
		List<LIRStatement> defUse = new ArrayList<LIRStatement>();
		defUse.addAll(w.getUses());
		defUse.addAll(w.getDefinitions());
		
		while (i >= 0) {
			LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
			
			for (LIRStatement du : defUse) {
				if (stmt == du) { // == or .equals()?
					return i;
				}
			}
			
			i--;
		}
		
		return 0; // 
	}

	private CFGBlock findProgramPointBlock() {
		LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(this.programPointIndex);
		
		for (CFGBlock block: this.mMap.get(this.currentMethod).getCfgBlocks()) {
			for (LIRStatement s: block.getStatements()) {
				if (s == stmt) return block;
			}
		}
		
		return null;
	}

	private boolean identifyProgramPoint() {
		System.out.println("FINDING PROGRAM POINT...");
		
		for (int i = 0; i < this.mMap.get(this.currentMethod).getStatements().size(); i++) {
			LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) continue; // Ignore labels as program points
			
			List<Web> potentialLiveWebs = findLiveWebs(stmt);
			
//			System.out.println("CHECKING: " + stmt + "  ;  " + stmt.getLiveInSet());
//			System.out.println("# webs: " + potentialLiveWebs.size());
			
			//System.out.println("FINDING PROGRAM POINT: POTENTIAL WEBS...");
			
			if (potentialLiveWebs.size() <= WebColorer.regCount) continue; // Not enough potential live webs
			
			//System.out.println("FINDING PROGRAM POINT: POTENTIAL DONE...");
			
			List<Web> liveWebs = doDefsReach(stmt, potentialLiveWebs);
			
			if (liveWebs.size() <= WebColorer.regCount) continue; // Not enough live webs
			
			// stmt is the required program point
			System.out.println("PROGRAM POINT FOUND: " + this.mMap.get(this.currentMethod).getStatements().get(i-1) + " at " + (i-1) + " with " + stmt.getLiveInSet());
			
			this.programPointIndex = i - 1;
			
			this.potentialWebs.clear();
			this.potentialWebs.addAll(liveWebs);
			
			return true;
		}
		
		return false;
	}
	
	private List<Web> doDefsReach(LIRStatement stmt, List<Web> potentialLiveWebs) {
		List<LIRStatement> defs = this.reachingDefinitions.getUniqueDefinitions().get(this.currentMethod);
		
		List<Web> temp = new ArrayList<Web>();

		for (Web w: potentialLiveWebs) {
			boolean defFound = false;
			
			for (LIRStatement def : w.getDefinitions()) {
				int index = defs.indexOf(def);
				if (stmt.getReachingDefInSet().get(index)) {
					defFound = true;
					break;
				}
			}
			
			if (defFound) {
				temp.add(w);
			}
		}
		
		return temp;
	}

	private List<Web> findLiveWebs(LIRStatement stmt) {
		List<Name> globalVars = this.livenessAnalysis.getUniqueVariables().get(this.currentMethod);
		
		List<Web> temp = new ArrayList<Web>();
		
		for (int i = 0; i < globalVars.size(); i++) {
			if (stmt.getLiveInSet().get(i)) {
				for (Web w: this.potentialWebs) {
					if (globalVars.get(i).equals(w.getVariable())) { // Potential web is of live var
						temp.add(w);
						break; // Only one live web per variable
					}
				}
			}
		}
		
		return temp;
	}
}
