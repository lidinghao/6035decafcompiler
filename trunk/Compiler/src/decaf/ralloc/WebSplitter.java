package decaf.ralloc;

import java.util.ArrayList;
import java.util.BitSet;
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
   private static String ForBodyRegex = "[a-zA-z_]\\w*.for\\d+.body";
   
	private HashMap<String, MethodIR> mMap;
	private String currentMethod;
	private List<Web> potentialWebs;
	private int programPointIndex;
	private ReachingDefinitions reachingDefinitions;
	private LivenessAnalysis livenessAnalysis;
	private HashMap<Web, Integer[]> splitPoints;
	private Web currentWeb;
	private CFGBlock ppBlock;
	
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
			System.exit(-1);
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
		
		insertStatements(splitWeb);
		
//		if (var.toString().contains("c")) {
//			System.out.println("***********");
//			System.out.println(store + " ==> " + this.splitPoints.get(splitWeb)[0]);
//			System.out.println(load + " ==> " + this.splitPoints.get(splitWeb)[2]);
//			System.out.println("-----------");
//			for (LIRStatement stmt : this.mMap.get(this.currentMethod).getStatements()) {
//				System.out.println(stmt);
//			}
//			System.exit(-1);
//		}
		
		// Done
		System.out.println("SPLITTING COMPLETE!");
		
		for (LIRStatement stmt: mMap.get(this.currentMethod).getStatements()){
			System.out.println(stmt);
		}
		
	}
	
	private void insertStatements(Web splitWeb) {
		Name var = splitWeb.getVariable();
		StoreStmt store = new StoreStmt(var);
		LoadStmt load = new LoadStmt(var);
		LIRStatement ppStmt = this.mMap.get(this.currentMethod).getStatements().get(this.programPointIndex);
		
		// Insert store (in CFG)		
		if (this.ppBlock.getIndex() != this.splitPoints.get(splitWeb)[0]) {
			// Different block than PP
			insertStore(store, this.splitPoints.get(splitWeb)[0], 0);
		}
		else {
			// In same block as PP
			insertStoreInPPBlock(store, ppStmt);
		}
		
		// Insert load (in CFG)
		if (this.ppBlock.getIndex() != this.splitPoints.get(splitWeb)[2]) {
			// Different block than PP
			insertLoad(load, this.splitPoints.get(splitWeb)[2], 0);
		}
		else {
			// In same block as PP
			insertLoadInPPBlock(load, ppStmt);
		}
		
		this.mMap.get(this.currentMethod).regenerateStmts();
	}

	private void insertLoadInPPBlock(LoadStmt load, LIRStatement ppStmt) {
		// Get block
		CFGBlock block = null;
		
		for (CFGBlock b: this.mMap.get(this.currentMethod).getCfgBlocks()) {
			if (b.getIndex() == this.ppBlock.getIndex()) {
				block = b;
				break;
			}
		}
		
		// Find pp stmt index
		int ppIndex = -1;
		for (int i = 0; i < block.getStatements().size(); i++) {
			if (block.getStatements().get(i) == ppStmt) {
				ppIndex = i;
				break;
			}
		}
		
		this.insertLoad(load, this.ppBlock.getIndex(), ppIndex+1);
	}

	private void insertStoreInPPBlock(StoreStmt store, LIRStatement ppStmt) {
		CFGBlock block = null;
		
		for (CFGBlock b: this.mMap.get(this.currentMethod).getCfgBlocks()) {
			if (b.getIndex() == this.ppBlock.getIndex()) {
				block = b;
				break;
			}
		}
		
		// Find pp stmt index
		int ppIndex = -1;
		for (int i = 0; i < block.getStatements().size(); i++) {
			if (block.getStatements().get(i) == ppStmt) {
				ppIndex = i;
				break;
			}
		}
		
		this.insertStore(store, this.ppBlock.getIndex(), ppIndex-1);
	}

	private void insertLoad(LoadStmt load, int blockId, int start) {
		List<LIRStatement> uses = this.currentWeb.getUses();
		
		// Get block
		CFGBlock block = null;
		
		for (CFGBlock b: this.mMap.get(this.currentMethod).getCfgBlocks()) {
			if (b.getIndex() == blockId) {
				block = b;
				break;
			}
		}
		
		int index = -1;
		int insertableIndex = 0;
		for (int i = start; i < block.getStatements().size(); i++) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class) ||
					stmt.getClass().equals(JumpStmt.class)) {
				continue;
			}
			
			if (insertableIndex == 0) {
				insertableIndex = i;
			}
			
			if (uses.contains(stmt)) {
				index = i;
				break;
			}
		}
		
		if (index == -1) {
			block.getStatements().add(insertableIndex, load);
		}
		else {
			block.getStatements().add(index, load); // Will add before it
		}
	}
	
	private void insertStore(StoreStmt store, int blockId, int end) {
		List<LIRStatement> useDefs = new ArrayList<LIRStatement>();
		useDefs.addAll(this.currentWeb.getUses());
		useDefs.addAll(this.currentWeb.getDefinitions());
		
		// Get block
		CFGBlock block = null;
		
		for (CFGBlock b: this.mMap.get(this.currentMethod).getCfgBlocks()) {
			if (b.getIndex() == blockId) {
				block = b;
				break;
			}
		}
		
		int index = -1;
		int insertableIndex = 0;
		for (int i = block.getStatements().size()-1; i >= end ; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class) ||
					stmt.getClass().equals(JumpStmt.class)) {
				continue;
			}
			
			if (insertableIndex == 0) {
				insertableIndex = i;
			}
			
			if (useDefs.contains(stmt)) {
				index = i;
				break;
			}
		}
		
		if (index == -1) {
			block.getStatements().add(insertableIndex, store);
		}
		else if (index+1 != block.getStatements().size()) {
			block.getStatements().add(index+1, store); // Will add after it
		}
		else {
			block.getStatements().add(store);
		}
	}

	private Web selectWebToSplit() {
		this.generateSplitPoints();
		
		Web splitWeb = null;
		float minHeuristic = 2147483647;
		
		for (Web w: this.splitPoints.keySet()) {
			Integer[] pt = this.splitPoints.get(w);
			int dist = -1 * (pt[2] - pt[0]); // Dist (want max)
			
			if (dist > 0) {
				System.out.println("ERROR: nextBlock < prevBlock for WEB: " + w.getIdentifier());
				System.exit(-1);
			}
			
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
		this.currentWeb = w;
		
		CFGBlock start = findProgramPointBlock();
		this.ppBlock = start;
		int previousDefUseIndex = findPreviousDefUse(w);
		CFGBlock previousUseBlock = getBlockWithStmtIndex(previousDefUseIndex);
		int nextUseIndex = findNextUse(w);
		CFGBlock nextUseBlock = getBlockWithStmtIndex(nextUseIndex);
		
		/**
		 * GET EARLIEST PREDECESSOR
		 */
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
		
		/**
		 * GET LATEST CHILD
		 */
		CFGBlock oldNext = null;
		CFGBlock currentNext = start;
		
		while (!currentNext.equals(oldNext)) {
			oldNext = currentNext;
			
//			System.out.println("OLD NEXT!");
//			System.out.println(oldNext);
			
			// Find parent common node			
			currentNext = getNextCommonNode(currentNext);
			
			// Child went after use
			if (currentNext.getIndex() > nextUseBlock.getIndex()) {
				currentNext = oldNext;
			}
		}
		
		/**
		 * SET DEPTH + BLOCK INDEX OF PARENT
`		 */
		indices[0] = previousUseBlock.getIndex();
		indices[1] = previousUseBlock.getStatements().get(0).getDepth();
		
		/**
		 * SET DEPTH + BLOCK INDEX OF CHILD
		 */
		indices[2] = nextUseBlock.getIndex();
		indices[3] = nextUseBlock.getStatements().get(0).getDepth();
		
		return indices;
	}
	
//	private CFGBlock findCommonParentWithDepth(CFGBlock start, Integer integer) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	private CFGBlock findCommonChildWithDepth(CFGBlock start, Integer integer) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	private CFGBlock getPreviousCommonNode(CFGBlock currentPrevious) {
//		System.out.println("GETTING PREVIOUS OF BLOCK for " + this.currentWeb.getVariable());
//		System.out.println(currentPrevious);
		
		if (isForTestBlock(currentPrevious)) { // FORK (get node on top of for loop)
			return currentPrevious.getPredecessors().get(0);
		}
		
		if (isForEndNode(currentPrevious)) { // Implicit FORK (get the init block of for loop)
//			return getForInitBlock(currentPrevious);
			assert (currentPrevious.getSuccessors().size() == 2);
			
			CFGBlock left = currentPrevious.getPredecessors().get(0);
			CFGBlock right = currentPrevious.getPredecessors().get(1);
			boolean reachesLeft = this.definitionReaches(left);
			boolean reachesRight = this.definitionReaches(right);
			
			if (reachesLeft && reachesRight) {
				return getForInitBlock(currentPrevious);
			}
			else if (reachesLeft) {
				return left;
			}
			else if (reachesRight) {
				return right;
			}
			else {
				return currentPrevious;
			}			
		}
		
		if (isIfEndNode(currentPrevious)) { // FORK (get the init block of conditional)
//			return getIfTestBlock(currentPrevious);
			assert (currentPrevious.getSuccessors().size() == 2);
			
			CFGBlock left = currentPrevious.getPredecessors().get(0);
			CFGBlock right = currentPrevious.getPredecessors().get(1);
			boolean reachesLeft = this.definitionReaches(left);
			boolean reachesRight = this.definitionReaches(right);
			
			if (reachesLeft && reachesRight) {
				return getIfTestBlock(currentPrevious);
			}
			else if (reachesLeft) {
				return left;
			}
			else if (reachesRight) {
				return right;
			}
			else {
				return currentPrevious;
			}			
		}
		
		if (currentPrevious.getPredecessors().isEmpty()) { // ROOT (return)
			return currentPrevious;
		}
		if (currentPrevious.getPredecessors().size() == 1) { // SINGLE PRED (go to it)
			return currentPrevious.getPredecessors().get(0);
		}
		
		return null;
	}

	private boolean definitionReaches(CFGBlock block) {
		List<LIRStatement> defs = this.reachingDefinitions.getUniqueDefinitions().get(this.currentMethod);
		
		for (LIRStatement def: this.currentWeb.getDefinitions()) {
			int index = defs.indexOf(def);
			
			if (this.reachingDefinitions.getCfgBlocksState().get(block).getOut().get(index)) {
				return true;
			}
		}
		
		return false;
	}

	private CFGBlock getNextCommonNode(CFGBlock currentNext) {
//		System.out.println("GETTING NEXT OF BLOCK");
//		System.out.println(currentNext);
		
		int index = this.livenessAnalysis.getUniqueVariables().get(this.currentMethod).indexOf(this.currentWeb.getVariable());
		//System.out.println("CHECKING: " + this.currentVariable);
		
		if (isForTestBlock(currentNext)) { // FORK (go to end of loop)
//			return currentNext.getSuccessors().get(1);
			assert (currentNext.getSuccessors().size() == 2);
			
			CFGBlock left = currentNext.getSuccessors().get(0);
			CFGBlock right = currentNext.getSuccessors().get(1);
			boolean liveLeft = this.livenessAnalysis.getCfgBlocksState().get(left).getIn().get(index);
			boolean liveRight = this.livenessAnalysis.getCfgBlocksState().get(right).getIn().get(index);
			
			if (liveLeft && liveRight) {
				return getIfEndBlock(currentNext);
			}
			else if (liveLeft) {
				return left;
			}
			else if (liveRight) {
				return right;
			}
			else {
				return currentNext;
			}			
		}
		
		if (isForInitNode(currentNext)) { // Implicit FORK (go to end of loop)
//			return getForEndBlock(currentNext);
			return currentNext.getSuccessors().get(0);
		}
		
		if (isForBodyNode(currentNext)) { // Implicit FORK (go to end of loop)
//			return getForEndBlock(currentNext);
			return currentNext.getSuccessors().get(0);
		}
		
		if (isIfTestNode(currentNext)) { // FORK
			assert (currentNext.getSuccessors().size() == 2);
			
			CFGBlock left = currentNext.getSuccessors().get(0);
			CFGBlock right = currentNext.getSuccessors().get(1);
			boolean liveLeft = this.livenessAnalysis.getCfgBlocksState().get(left).getIn().get(index);
			boolean liveRight = this.livenessAnalysis.getCfgBlocksState().get(right).getIn().get(index);
			
			if (liveLeft && liveRight) {
				return getIfEndBlock(currentNext);
			}
			else if (liveLeft) {
				return left;
			}
			else if (liveRight) {
				return right;
			}
			else {
				return currentNext;
			}			
		}
		
		if (currentNext.getSuccessors().isEmpty()) { // LEAF (return)
			return currentNext;
		}
		if (currentNext.getSuccessors().size() == 1) { // SINGLE CHILD (go to it)
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
		int i = this.programPointIndex;
		
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
//				System.out.println("VAR: " + w.getVariable() +" DU: " + du + " ==== STMT:" + stmt);
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
			LIRStatement next = null;
			if (this.mMap.get(this.currentMethod).getStatements().size() > i+1) {
				stmt = this.mMap.get(this.currentMethod).getStatements().get(i+1);
			}
			
			if (stmt.getClass().equals(LabelStmt.class)) continue; // Ignore labels as program points
			
			System.out.println("CHECKING STATEMENT: " + stmt);
			List<Web> potentialLiveWebs = findLiveWebs(stmt, next);
			
//			System.out.println("CHECKING: " + stmt + "  ;  " + stmt.getLiveInSet());
//			System.out.println("# webs: " + potentialLiveWebs.size());
			
			//System.out.println("FINDING PROGRAM POINT: POTENTIAL WEBS...");
			
			if (potentialLiveWebs.size() <= WebColorer.regCount) {
				System.out.println("NOT ENOUGH POTENTIAL WEBS");
				continue; // Not enough potential live webs
			}
			
			//System.out.println("FINDING PROGRAM POINT: POTENTIAL DONE...");
			
			List<Web> liveWebs = doDefsReach(stmt, potentialLiveWebs);
			
			if (liveWebs.size() <= WebColorer.regCount) {
				System.out.println("NOT ENGOUH LIVE WEBS");
				continue; // Not enough live webs
			}
			
			// stmt is the required program point
			System.out.println("PROGRAM POINT FOUND: " + this.mMap.get(this.currentMethod).getStatements().get(i) + " at " + (i) + " with " + stmt.getLiveInSet());
			
			this.programPointIndex = i;
			
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

	private List<Web> findLiveWebs(LIRStatement stmt, LIRStatement next) {
		List<Name> globalVars = this.livenessAnalysis.getUniqueVariables().get(this.currentMethod);
		
		BitSet liveVars = new BitSet(globalVars.size());
		liveVars.clear();
		liveVars.or(stmt.getLiveInSet());
		if (next != null) {
			//liveVars.or(next.getLiveInSet());
		}
		
		List<Web> temp = new ArrayList<Web>();
		
		System.out.println("GLOABL VARS => " + this.livenessAnalysis.getUniqueVariables().get(this.currentMethod));
		System.out.println(stmt + " : LIVE => " + stmt.getLiveInSet());
		
		for (int i = 0; i < globalVars.size(); i++) {
			if (liveVars.get(i)) {
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
	
	private boolean isForBodyNode(CFGBlock block) {
		if (!block.getStatements().isEmpty()) {
			LIRStatement stmt = block.getStatements().get(0);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt label = (LabelStmt) stmt;
				return (label.getLabelString().matches(ForBodyRegex));
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
				if (l.getLabelString().matches(ForInitRegex) ||
						l.getLabelString().matches(ForBodyRegex)) {
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
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(IfTestRegex)) {
					return true;
				}
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
