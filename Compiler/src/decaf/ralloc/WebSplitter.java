package decaf.ralloc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LeaveStmt;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.StoreStmt;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class WebSplitter {
   private static String IfEndRegex = "[a-zA-z_]\\w*.if\\d+.end";
   private static String IfTestRegex = "[a-zA-z_]\\w*.if\\d+.test";
   private static String ForEndRegex = "[a-zA-z_]\\w*.for\\d+.end";
   private static String ForTestRegex = "[a-zA-z_]\\w*.for\\d+.test";
   
	private HashMap<String, MethodIR> mMap;
	private String currentMethod;
	private List<Web> potentialWebs;
	private int programPointIndex;
	private LIRStatement ppStmt;
	private ReachingDefinitions reachingDefinitions;
	private LivenessAnalysis livenessAnalysis;
	private HashMap<Web, Integer[]> splitPoints;
	private Web currentWeb;
	private CFGBlock previousUse;
	private CFGBlock nextUse;
	
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
			for (LIRStatement s: this.mMap.get(this.currentMethod).getStatements()) {
				System.out.println(s);
			}
			System.exit(-1);
			return;
		}
		
		// Calculate maximal split points for each web
		generateSplitPoints();
		calculateInsertPoints();
		
		// Webs to split from
		for (Web w: this.splitPoints.keySet()) {
			System.out.println("SPLIT PTS: " + w.getIdentifier() + " :: [" + this.splitPoints.get(w)[0] +", " + this.splitPoints.get(w)[2] + "]");
		}
		
		// Select web to split (from potentialWebs)
		Web splitWeb = this.selectWebToSplit();
		
		System.out.println("\nSPLITTING: " + splitWeb.getIdentifier() + " at [" + this.splitPoints.get(splitWeb)[0] + ", " + this.splitPoints.get(splitWeb)[2] + "]");
		
		insertStatements(splitWeb);
		
		// Done
		System.out.println("SPLITTING COMPLETE!");
		
		for (CFGBlock block: mMap.get(this.currentMethod).getCfgBlocks()){
			System.out.println(block);
		}
		
	}
	
	private void calculateInsertPoints() {
		for (Web w: this.splitPoints.keySet()) {
			calculateInsertPoint(w);
		}
	}

	private void calculateInsertPoint(Web web) {
		CFGBlock prevBlock = getBlockWithId(this.splitPoints.get(web)[0]);
		CFGBlock nextBlock = getBlockWithId(this.splitPoints.get(web)[2]);
		CFGBlock ppBlock = getBlockWithStmtIndex(this.programPointIndex);
		
		generateIndicesForStore(web, prevBlock, ppBlock);
		generateIndicesForLoad(web, nextBlock, ppBlock);
		
		// Set global index for distance measurement
		this.splitPoints.get(web)[0] = getIndexInLIR(prevBlock, this.splitPoints.get(web)[0]);
		this.splitPoints.get(web)[2] = getIndexInLIR(nextBlock, this.splitPoints.get(web)[2]);
	}

	private void generateIndicesForStore(Web web, CFGBlock prevBlock,
			CFGBlock ppBlock) {
		if (prevBlock.getIndex() == ppBlock.getIndex()) {
			int ppStmtIndex = getIndexInBlock(prevBlock, this.ppStmt);
			int prologIndex = getPrologIndex(prevBlock);
			
//			System.out.println("******************");
//			System.out.println(web.getVariable() + "==>" + web.getDefinitions() + ", " + web.getUses());
//			System.out.println("PPI: " + ppStmtIndex);
//			System.out.println("POLOGI: " + prologIndex);
			
			if (isDef(web, prevBlock, ppStmtIndex)) { // Def so store after it
				ppStmtIndex++;
			}
			
			boolean added = false;
			for (int i = ppStmtIndex-1; i >= prologIndex; i--) {
				LIRStatement stmt = prevBlock.getStatements().get(i);
				if (isDefUse(web, stmt)) {
//					System.out.println("STORE AFTER: " + stmt + " at " + (i+1));
					this.splitPoints.get(web)[0] = i+1;
					added = true;
					break;
				}
			}
			
			
			if (!added) {
//				System.out.println("STORE AFTER POLOG at " + prologIndex);
				this.splitPoints.get(web)[0] = prologIndex;
			}
		}
		else {
			int epilogIndex = getEpilogIndex(prevBlock);
			int prologIndex = getPrologIndex(prevBlock);
			
			boolean added = false;
			for (int i = epilogIndex; i >= prologIndex; i--) {
				LIRStatement stmt = prevBlock.getStatements().get(i);
				if (isDefUse(web, stmt)) {
					this.splitPoints.get(web)[0] = i+1;
					added = true;
					break;
				}
			}
			
			if (!added) {
				this.splitPoints.get(web)[0] = prologIndex;
			}
		}
	}

	private void generateIndicesForLoad(Web web, CFGBlock nextBlock,
			CFGBlock ppBlock) {
		if (nextBlock.getIndex() == ppBlock.getIndex()) {
			int ppStmtIndex = getIndexInBlock(nextBlock, this.ppStmt);
			int epilogIndex = getEpilogIndex(nextBlock);
			
//			System.out.println("PPI: " + ppStmtIndex);
//			System.out.println("EPILOGI: " + epilogIndex);
			
			if (isDef(web, nextBlock, ppStmtIndex)) { // Def so store after it
				ppStmtIndex++;
			}
			
			boolean added = false;
			for (int i = ppStmtIndex; i < epilogIndex; i++) {
				LIRStatement stmt = nextBlock.getStatements().get(i);
				if (isDefUse(web, stmt)) {
//					System.out.println("LOAD BEFORE: " + stmt + " at " + i);
					this.splitPoints.get(web)[2] = i;
					added = true;
					break;
				}
			}
			
			
			if (!added) {
//				System.out.println("LOAD BEFORE EPILOG at " + epilogIndex);
				this.splitPoints.get(web)[2] = epilogIndex+1;
			}
		}
		else {
			int epilogIndex = getEpilogIndex(nextBlock);
			int prologIndex = getPrologIndex(nextBlock);
			
			boolean added = false;
			for (int i = prologIndex; i < epilogIndex; i++) {
				LIRStatement stmt = nextBlock.getStatements().get(i);
				if (isDefUse(web, stmt)) {
					this.splitPoints.get(web)[2] = i;
					added = true;
					break;
				}
			}
			
			if (!added) {
				this.splitPoints.get(web)[2] = epilogIndex+1;
			}
		}
	}

	private void insertStatements(Web splitWeb) {
		Name var = splitWeb.getVariable();
		StoreStmt storeStmt = new StoreStmt(var);
		LoadStmt loadStmt = new LoadStmt(var);
		
		CFGBlock prevBlock = getBlockWithStmtIndex(this.splitPoints.get(splitWeb)[0]);
		CFGBlock nextBlock = getBlockWithStmtIndex(this.splitPoints.get(splitWeb)[2]);
		CFGBlock ppBlock = getBlockWithStmtIndex(this.programPointIndex);
		
		if (prevBlock.getIndex() == ppBlock.getIndex()) {
			int ppStmtIndex = getIndexInBlock(prevBlock, this.ppStmt);
			int prologIndex = getPrologIndex(prevBlock);
			
			if (isDef(splitWeb, prevBlock, ppStmtIndex)) { // Def so store after it
				ppStmtIndex++;
			}
			
			boolean added = false;
			for (int i = ppStmtIndex-1; i >= prologIndex; i--) {				
				LIRStatement stmt = prevBlock.getStatements().get(i);
				if (isDefUse(splitWeb, stmt)) {
					prevBlock.getStatements().add(i+1, storeStmt);
					added = true;
					break;
				}
			}
			
			if (!added) {
				prevBlock.getStatements().add(prologIndex, storeStmt);
			}
		}
		else {
			int epilogIndex = getEpilogIndex(prevBlock);
			int prologIndex = getPrologIndex(prevBlock);
			
			boolean added = false;
			for (int i = epilogIndex; i >= prologIndex; i--) {
				LIRStatement stmt = prevBlock.getStatements().get(i);
				if (isDefUse(splitWeb, stmt)) {
					prevBlock.getStatements().add(i+1, storeStmt);
					added = true;
					break;
				}
			}
			
			if (!added) {
				prevBlock.getStatements().add(prologIndex, storeStmt);
			}
		}
		
		// NEXT
		
		if (nextBlock.getIndex() == ppBlock.getIndex()) {
			int ppStmtIndex = getIndexInBlock(nextBlock, this.ppStmt);
			int epilogIndex = getEpilogIndex(nextBlock);
			
			if (isDef(splitWeb, prevBlock, ppStmtIndex)) { // Def so start load after def
				ppStmtIndex++;
			}
			
			boolean added = false;
			for (int i = ppStmtIndex; i < epilogIndex; i++) {
				LIRStatement stmt = nextBlock.getStatements().get(i);
				if (isUse(splitWeb, stmt)) {
					nextBlock.getStatements().add(i, loadStmt);
					added = true;
					break;
				}
			}
			
			if (!added) prevBlock.getStatements().add(epilogIndex+1, loadStmt);
		}
		else {
			int epilogIndex = getEpilogIndex(nextBlock);
			int prologIndex = getPrologIndex(nextBlock);
			
			boolean added = false;
			for (int i = prologIndex; i < epilogIndex; i++) {
				LIRStatement stmt = nextBlock.getStatements().get(i);
				if (isDefUse(splitWeb, stmt)) {
					nextBlock.getStatements().add(i, loadStmt);
					added = true;
					break;
				}
			}
			
			if (!added) {
				if (!added) nextBlock.getStatements().add(epilogIndex+1, loadStmt);
			}
		}
	}
	
	private int getEpilogIndex(CFGBlock block) {
		for (int i = block.getStatements().size()-1; i >=0 ; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			if (isEpilogStmt(stmt)) continue;
			
			return i;
		}
		
		return -1;
	}
	
	private boolean isDef(Web w, CFGBlock b, int i) {
		LIRStatement stmt = b.getStatements().get(i);
		
		for (LIRStatement def: w.getDefinitions()) {
			if (def == stmt) return true;
		}
		
		return false;
	}
	
	private boolean isUse(Web w, LIRStatement stmt) {
		for (LIRStatement use: w.getUses()) {
			if (use == stmt) return true;
		}
		
		return false;
	}

	private boolean isDefUse(Web w, LIRStatement stmt) {
		for (LIRStatement use: w.getUses()) {
			if (use == stmt) return true;
		}
		
		for (LIRStatement def: w.getDefinitions()) {
			if (def == stmt) return true;
		}
		
		return false;
	}
	
	private int getPrologIndex(CFGBlock block) {
		for (int i = 0; i < block.getStatements().size(); i++) {
			LIRStatement stmt = block.getStatements().get(i);
			if (isPrologStmt(stmt)) continue;
			
			return i;
		}
		
		return -1;
	}
	
	private int getIndexInBlock(CFGBlock block, LIRStatement stmt) {
		for (int i = 0; i < block.getStatements().size(); i++) {
			LIRStatement s = block.getStatements().get(i);
			if (stmt == s) return i;
		}
		
		return -1;
	}
	
	private int getIndexInLIR(CFGBlock block, int index) {
		if (index >= block.getStatements().size()) {
			index = block.getStatements().size()-1;
		}
		
		LIRStatement stmt = block.getStatements().get(index);
		for (int i = 0; i < this.mMap.get(this.currentMethod).getStatements().size(); i++) {
			LIRStatement s = this.mMap.get(this.currentMethod).getStatements().get(i);
			if (stmt == s) return i;
		}
		
		return -1;
	}
	
	private boolean isEpilogStmt(LIRStatement stmt) {
		return (stmt.getClass().equals(JumpStmt.class)
				|| stmt.getClass().equals(LeaveStmt.class));
	}
	
	private boolean isPrologStmt(LIRStatement stmt) {
		return (stmt.getClass().equals(LabelStmt.class) 
				|| stmt.getClass().equals(EnterStmt.class));
	}

	private CFGBlock getBlockWithId(Integer integer) {
		for (CFGBlock b: this.mMap.get(currentMethod).getCfgBlocks()) {
			if (b.getIndex() == integer) return b;
		}
		
		return null;
	}

	private Web selectWebToSplit() {
		Web splitWeb = null;
		float minHeuristic = 2147483647;
		
		for (Web w: this.splitPoints.keySet()) {
			Integer[] pt = this.splitPoints.get(w);
			int dist = (pt[2] - pt[0]); // Dist
			
			if (dist < 0) {
				System.out.println("ERROR: nextBlock < prevBlock for WEB: " + w.getIdentifier());
				System.exit(-1);
			}
			
			int depth = pt[1] + pt[3]; // Add depth?
			float heuristic = calculateHeuristic(dist, depth, w.getInterferingWebs().size());
			if (heuristic < minHeuristic) {
				minHeuristic = heuristic;
				splitWeb = w;
			}
		}
		
		return splitWeb;
	}
	
	/**
	 * Want minimum heuristic
	 * @param distance
	 * @param depth
	 * @param neighbors
	 * @return
	 */
	private float calculateHeuristic(int distance, int depth, int neighbors) {
		return (float) (-2 * distance);// - depth + neighbors);
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
		int previousDefUseIndex = findPreviousDefUse(w);
		CFGBlock previousUseBlock = getBlockWithStmtIndex(previousDefUseIndex);
		this.previousUse = previousUseBlock;
		int nextUseIndex = findNextUse(w);
		CFGBlock nextUseBlock = getBlockWithStmtIndex(nextUseIndex);
		this.nextUse = nextUseBlock;
		
		/**
		 * GET EARLIEST PREDECESSOR
		 */
		CFGBlock oldPrevious = null;
		CFGBlock currentPrevious = start;
		
		while (oldPrevious != null && 
				currentPrevious.getIndex() != oldPrevious.getIndex()) {
//			System.out.println("CURRENT != PREV (old then new)");
//			System.out.println(oldPrevious);
//			System.out.println(currentPrevious);
//			System.out.println("**********");
			oldPrevious = currentPrevious;
			
			// Find parent common node
			currentPrevious = getPreviousCommonNode(currentPrevious);
		}
		
		/**
		 * GET LATEST CHILD
		 */
		CFGBlock oldNext = null;
		CFGBlock currentNext = start;
		
		while (oldNext != null && 
				currentNext.getIndex() != oldNext.getIndex()) {
			oldNext = currentNext;
			System.out.println("CURRENT != NEXT (old then new)");
			System.out.println(oldNext);
			System.out.println(currentNext);
			System.out.println("**********");
			
			// Find parent common node	
			currentNext = getNextCommonNode(currentNext);
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
		
		if (indices[0] > start.getIndex() || indices[2] < start.getIndex()) {
			System.out.println("INVALID PREV & NEXT BLOCK");
			System.exit(-1);
		}
		
		return indices;
	}

	private CFGBlock getPreviousCommonNode(CFGBlock currentPrevious) {
		CFGBlock nextPrev = null;
		
		if (isForTestBlock(currentPrevious)) { // FORK (get node on top of for loop)
			CFGBlock init = currentPrevious.getPredecessors().get(0); // init
			CFGBlock body = currentPrevious.getPredecessors().get(1); // body
			boolean reachesInit = this.definitionReaches(init);
			boolean reachesBody = this.definitionReaches(body);
			
			if (reachesInit && reachesBody) {
				if (this.previousUse.getIndex() < currentPrevious.getIndex()) {
					nextPrev = currentPrevious.getPredecessors().get(0);
				}
				else {
					nextPrev = currentPrevious;
				}
			}
			else if (reachesInit) { // WILL EVER REACH?
				nextPrev = init; // Move up chain
			}
			else {
				nextPrev = currentPrevious; // shouldn't happen!!
			}	
		}
		
		if (isIfEndNode(currentPrevious)) { // FORK (get the init block of conditional)
			assert (currentPrevious.getSuccessors().size() == 2);
			
			CFGBlock left = currentPrevious.getPredecessors().get(0);
			CFGBlock right = currentPrevious.getPredecessors().get(1);
			boolean reachesLeft = this.definitionReaches(left);
			boolean reachesRight = this.definitionReaches(right);
			
			if (reachesLeft && reachesRight) {
				CFGBlock test = getIfTestBlock(currentPrevious); // will automatically reject if defined in the middle
				if (test.getIndex() >= this.previousUse.getIndex()) {
					nextPrev = test;
				}
				else {
					nextPrev = currentPrevious;
				}
			}
			else if (reachesLeft) {
				nextPrev = left;
			}
			else if (reachesRight) {
				nextPrev = right;
			}
			else {
				nextPrev = currentPrevious; // shouldn't happen
			}			
		}
		
		if (currentPrevious.getPredecessors().isEmpty()) { // ROOT (return)
			nextPrev = currentPrevious;
		}
		if (currentPrevious.getPredecessors().size() == 1) { // SINGLE PRED (go to it)
			nextPrev = currentPrevious.getPredecessors().get(0);
		}
		
		if (nextPrev == null || !this.definitionReaches(nextPrev) || nextPrev.getIndex() < this.previousUse.getIndex()) {
			nextPrev = currentPrevious;
		}
		
		return nextPrev;
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
	
	private boolean isLiveIn(CFGBlock block) {
		int index = this.livenessAnalysis.getUniqueVariables().get(this.currentMethod).indexOf(this.currentWeb.getVariable());
		return this.livenessAnalysis.getCfgBlocksState().get(block).getIn().get(index);
	}

	private CFGBlock getNextCommonNode(CFGBlock currentNext) {			
		CFGBlock next = null;
		
		if (isForTestBlock(currentNext)) { // FORK (go to end of loop)
			assert (currentNext.getSuccessors().size() == 2);
			
			CFGBlock body = currentNext.getSuccessors().get(0); // body
			CFGBlock end = currentNext.getSuccessors().get(1); //end
			boolean liveBody = isLiveIn(body);
			boolean liveEnd = isLiveIn(end);
			
			if (liveBody && liveEnd) {
				CFGBlock myEnd = getForEndBlock(currentNext); // live in both, try end
				if (myEnd.getIndex() > this.nextUse.getIndex()) {
					next = currentNext;
				}
				else {
					next = myEnd;
				}
			}
			else if (liveBody) {
				next = currentNext; 
			}
			else if (liveEnd) {
				next = getForEndBlock(currentNext);
			}
			else {
				System.out.println("WTF");
				next = currentNext; // wtf?
			}
		}
		
		if (isIfTestNode(currentNext)) { // FORK
			assert (currentNext.getSuccessors().size() == 2);
			
			CFGBlock left = currentNext.getSuccessors().get(0);
			CFGBlock right = currentNext.getSuccessors().get(1);
			boolean liveLeft = isLiveIn(left);
			boolean liveRight = isLiveIn(right);
			
			if (liveLeft && liveRight) {
				CFGBlock myEnd = getIfEndBlock(currentNext); // live in both, try end
				if (myEnd.getIndex() > this.nextUse.getIndex()) {
					next = currentNext;
				}
				else {
					next = myEnd;
				}
			}
			else if (liveLeft) {
				next = left;
			}
			else if (liveRight) {
				next = right;
			}
			else {
				next = currentNext; // wtf?
			}
		}
		
		if (currentNext.getSuccessors().isEmpty()) { // LEAF (return)
			next = currentNext;
		}
		if (currentNext.getSuccessors().size() == 1) { // SINGLE CHILD (go to it)
			next = currentNext.getSuccessors().get(0);
		}
		
		if (next == null || !isLiveIn(next) || next.getIndex() > this.nextUse.getIndex()) {
			System.out.println("NEXT: " + next + " with LIVE: " + isLiveIn(next));
			next = currentNext;
		}
		
		return next;
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
				next = this.mMap.get(this.currentMethod).getStatements().get(i+1);
			}
			
			if (stmt.getClass().equals(LabelStmt.class)) continue; // Ignore labels as program points
			
			List<Web> potentialLiveWebs = findLiveWebs(stmt, next);
			
			if (potentialLiveWebs.size() < WebColorer.regCount - 4) {
				continue; // Not enough potential live webs
			}
			
			List<Web> liveWebs = doDefsReach(stmt, potentialLiveWebs);
			
			if (liveWebs.size() < WebColorer.regCount - 4) {
				continue; // Not enough live webs
			}
			
			// stmt is the required program point
			System.out.println("LIVE VARS: " + this.livenessAnalysis.getUniqueVariables().get(this.currentMethod));
			System.out.println("PROGRAM POINT FOUND: " + this.mMap.get(this.currentMethod).getStatements().get(i) + " at " + i + " with " + stmt.getLiveInSet());
			
			this.programPointIndex = i;
			this.ppStmt = stmt;
			
			this.potentialWebs.clear();
			this.potentialWebs.addAll(liveWebs);
			
			return true;
		}
		
		return false;
	}
	
	private List<Web> doDefsReach(LIRStatement stmt, List<Web> potentialLiveWebs) {
		List<LIRStatement> defs = this.reachingDefinitions.getUniqueDefinitions().get(this.currentMethod);
		
//		System.out.println("GLOBAK DEFS: " + defs);
//		System.out.println("CHECKING FOR DEF REACH AT: " + stmt + " WITH DEF OUT: " + stmt.getReachingDefInSet());
//		System.out.println("POTENTIAL WEBS: " + potentialLiveWebs);
		
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
		
//		System.out.println("DEFS FOUND: " + temp + "\n");
		
		return temp;
	}

	private List<Web> findLiveWebs(LIRStatement stmt, LIRStatement next) {
		List<Name> globalVars = this.livenessAnalysis.getUniqueVariables().get(this.currentMethod);
		
		if (next != null) {
			//stmt = next;
		}
		
		BitSet liveVars = new BitSet(globalVars.size());
		liveVars.clear();
		liveVars.or(stmt.getLiveInSet());
		
//		System.out.println("GLOBAL VARS: " + globalVars);
//		System.out.println("CHECKING FOR POTENTIAL LIVE AT: " + stmt + " WITH LIVE IN: " + stmt.getLiveInSet());
		
		List<Web> temp = new ArrayList<Web>();
		
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
		
//		System.out.println("POTENTIALS FOUND: " + temp + "\n");
		
		return temp;
	}
	
	private String getIndexOfFor(String name) {
		int i = name.indexOf(".for");
      name = name.substring(i + 4);
      
      i = name.indexOf(".");
      name = name.substring(0, i);
      
      return name; 
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
	
	private CFGBlock getForEndBlock(CFGBlock block) {
		LabelStmt label = null;
		
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt l = (LabelStmt) stmt;
				if (l.getLabelString().matches(ForTestRegex)) {
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
