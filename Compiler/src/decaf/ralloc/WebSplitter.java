package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.StoreStmt;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class WebSplitter {
   private static String IfEndRegex = "[a-zA-z_]\\w*.if\\d+.end";
   private static String IfTestRegex = "[a-zA-z_]\\w*.if\\d+.test";
   private static String ForInitRegex = "[a-zA-z_]\\w*.for\\d+.init";
   private static String ForEndRegex = "[a-zA-z_]\\w*.for\\d+.end";
   private static String ForTestRegex = "[a-zA-z_]\\w*.for\\d+.test";
   
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
			System.out.println("SPLIT PTS: " + w.getIdentifier() + " :: [" + this.splitPoints.get(w)[0] +", " + this.splitPoints.get(w)[2] + "]");
		}
		
		System.out.println("\nSPLITTING: " + splitWeb.getIdentifier() + " at [" + this.splitPoints.get(splitWeb)[0] + ", " + this.splitPoints.get(splitWeb)[2] + "]");
		
		Name var = splitWeb.getVariable();
		StoreStmt store = new StoreStmt(var);
		LoadStmt load = new LoadStmt(var);
		
		// Insert store (in CFG)
		insertStore(store, this.splitPoints.get(splitWeb)[0]);
		
		// Insert load (in CFG)
		insertLoad(load, this.splitPoints.get(splitWeb)[2]);
		
		this.mMap.get(this.currentMethod).regenerateStmts();
		
		// Done
		System.out.println("SPLITTING COMPLETE!");
		
		System.out.println(mMap.get(this.currentMethod).getStatements());
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
			int dist = -1 * (pt[2] - pt[0]); // Dist (want max)
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
		
		
		CFGBlock oldPrevious = null;
		CFGBlock currentPrevious = start;
		
		while (!currentPrevious.equals(oldPrevious)) {
			oldPrevious = currentPrevious;
			
			// Find parent common node
			currentPrevious = getPreviousCommonNode(currentPrevious);

			// Parent went before use
			if (currentPrevious.getIndex() < previousUseBlock.getIndex()) {
				currentPrevious = oldPrevious;
			}
		}
		
		if (currentPrevious.getIndex() == previousUseBlock.getIndex()) {
			// Reached use block
			indices[0] = previousDefUseIndex;
			indices[1] = previousUseBlock.getStatements().get(0).getDepth();
		}
		else {
			// Find depth and index to insert
			indices[0] = getPrevInsertIndex(currentPrevious);
			indices[1] = currentPrevious.getStatements().get(0).getDepth();
		}
		
		CFGBlock oldNext = null;
		CFGBlock currentNext = start;
		
		while (!currentNext.equals(oldNext)) {
			oldNext = currentNext;
			
			// Find parent common node
			currentNext = getNextCommonNode(currentNext);
			
			// Child went after use
			if (currentNext.getIndex() > nextUseBlock.getIndex()) {
				currentNext = oldNext;
			}
		}
		
		if (currentNext.getIndex() == nextUseBlock.getIndex()) {
			// Reached use block
			indices[2] = nextUseIndex;
			indices[3] = nextUseBlock.getStatements().get(0).getDepth();
		}
		else {
			// Find depth and index to insert
			indices[2] = getNextInsertIndex(currentNext);
			indices[3] = currentNext.getStatements().get(0).getDepth();
		}
		
		return indices;
	}
	
	private Integer getNextInsertIndex(CFGBlock block) {
		for (int i = block.getStatements().size()-1; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				continue;
			}
			
			return i;
		}
		
		return block.getStatements().size();
	}

	private int getPrevInsertIndex(CFGBlock block) {
		for (int i = block.getStatements().size()-1; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class) ||
					stmt.getClass().equals(JumpStmt.class)) {
				continue;
			}
			
			return i;
		}
		
		return 0;
	}

	private CFGBlock getPreviousCommonNode(CFGBlock currentPrevious) {
		//System.out.println("FINDING FOR: " + currentPrevious);
		
		if (isForTestBlock(currentPrevious)) {
			return currentPrevious.getPredecessors().get(0);
		}
		
		if (isForEndNode(currentPrevious)) {
			return getForInitBlock(currentPrevious);
		}
		
		if (isIfEndNode(currentPrevious)) {
			return getIfTestBlock(currentPrevious);
		}
		
		if (currentPrevious.getPredecessors().isEmpty()) {
			return currentPrevious;
		}
		if (currentPrevious.getPredecessors().size() == 1) {
			return currentPrevious.getPredecessors().get(0);
		}
		
		return null;
	}

	private CFGBlock getNextCommonNode(CFGBlock currentNext) {
		if (isForInitNode(currentNext)) {
			return getForEndBlock(currentNext);
		}
		
		if (isIfTestNode(currentNext)) {
			return getIfEndBlock(currentNext);
		}
		
		if (currentNext.getSuccessors().isEmpty()) {
			return currentNext;
		}
		if (currentNext.getSuccessors().size() == 1) {
			return currentNext.getPredecessors().get(0);
		}
		
		return null;
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
	
	private CFGBlock getForInitBlock(CFGBlock block) {
		LabelStmt label = (LabelStmt) block.getStatements().get(0);
		
		String index = getIndexOfFor(label.getLabelString());
		
		int found = -1;
		
		for (int i = 0; i < this.mMap.get(this.currentMethod).getStatements().size(); i++) {
			LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(ForInitRegex)) {
					if (l.getLabelString().contains(index)) {
						found = i;
						break;
					}
				}
			}
		}
		
		return getBlockWithStmtIndex(found);
	}
	
	private String getIndexOfFor(String name) {
		int i = name.indexOf(".for");
      name = name.substring(i + 4);
      
      i = name.indexOf(".");
      name = name.substring(0, i);
      
      return name; 
	}
	
	private boolean isForEndNode(CFGBlock block) {
		if (!block.getStatements().isEmpty()) {
			LIRStatement stmt = block.getStatements().get(0);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt label = (LabelStmt) stmt;
				return (label.getLabelString().matches(ForEndRegex));
			}
		}
		
		return false;
	}
	
	private boolean isForTestBlock(CFGBlock block) {
		if (!block.getStatements().isEmpty()) {
			LIRStatement stmt = block.getStatements().get(0);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt label = (LabelStmt) stmt;
				return (label.getLabelString().matches(ForTestRegex));
			}
		}
		
		return false;
	}
	
	private boolean isForInitNode(CFGBlock block) {
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(ForInitRegex)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private CFGBlock getForEndBlock(CFGBlock block) {
		LabelStmt label = null;
		
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(ForInitRegex)) {
					label = l;
				}
			}
		}
		
		String index = getIndexOfFor(label.getLabelString());
		
		int found = -1;
		
		for (int i = 0; i < this.mMap.get(this.currentMethod).getStatements().size(); i++) {
			LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(ForEndRegex)) {
					if (l.getLabelString().contains(index)) {
						found = i;
						break;
					}
				}
			}
		}
		
		return getBlockWithStmtIndex(found);
	}

	// IF STUFF
	private CFGBlock getIfTestBlock(CFGBlock block) {
		LabelStmt label = (LabelStmt) block.getStatements().get(0); // block is if end block
		
		String index = getIndexOfIf(label.getLabelString());
		
		int found = -1;
		
		for (int i = 0; i < this.mMap.get(this.currentMethod).getStatements().size(); i++) {
			LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(IfTestRegex)) {
					if (l.getLabelString().contains(index)) {
						found = i;
						break;
					}
				}
			}
		}
		
		return getBlockWithStmtIndex(found);
	}
	
	private String getIndexOfIf(String name) {
		int i = name.indexOf(".if");
      name = name.substring(i + 3);
      
      i = name.indexOf(".");
      name = name.substring(0, i);
      
      return name; 
	}
	
	private boolean isIfEndNode(CFGBlock block) {
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(IfEndRegex)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean isIfTestNode(CFGBlock block) {
		if (!block.getStatements().isEmpty()) {
			LIRStatement stmt = block.getStatements().get(0);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt label = (LabelStmt) stmt;
				return (label.getLabelString().matches(IfTestRegex));
			}
		}
		
		return false;
	}
	
	private CFGBlock getIfEndBlock(CFGBlock block) {
		LabelStmt label = null;
		
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(IfTestRegex)) {
					label = l;
				}
			}
		}
		
		String index = getIndexOfFor(label.getLabelString());
		
		int found = -1;
		
		for (int i = 0; i < this.mMap.get(this.currentMethod).getStatements().size(); i++) {
			LIRStatement stmt = this.mMap.get(this.currentMethod).getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(IfEndRegex)) {
					if (l.getLabelString().contains(index)) {
						found = i;
						break;
					}
				}
			}
		}
		
		return getBlockWithStmtIndex(found);
	}
}
