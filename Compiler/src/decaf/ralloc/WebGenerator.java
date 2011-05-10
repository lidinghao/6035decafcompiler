package decaf.ralloc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.StoreStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

/**
 * TODO: Single definition only or single use only can happen for globals,
 * no need to assign registers to them
 * 
 * First *def* of array uses index variable (def can be load)
 * @author usmanm
 *
 */
public class WebGenerator {
	private HashMap<String, MethodIR> mMap;
	private ReachingDefinitions reachingDef;
	private LivenessAnalysis liveAnalysis;
	private HashMap<String, List<Web>> webMap;
	private HashMap<Name, List<Web>> nameToWebs; // Webs currently mapping to that name
	private HashMap<LIRStatement, Web> defToWeb; // Web associated with definition
	private String currentMethod;
	private List<Name> namesDeadAtOutStmt;
	
	public WebGenerator(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.reachingDef = new ReachingDefinitions(mMap);
		this.liveAnalysis = new LivenessAnalysis(mMap);
		this.webMap = new HashMap<String, List<Web>>();
		this.nameToWebs = new HashMap<Name, List<Web>>();
		this.defToWeb = new HashMap<LIRStatement, Web>();
		this.namesDeadAtOutStmt = new ArrayList<Name>();
	}
	
	public void generateWebs() {	
		Web.ID = 0;
		
		this.reachingDef.analyze();
		this.liveAnalysis.analyze();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			this.currentMethod = methodName;
			
			this.webMap.put(methodName, new ArrayList<Web>());
			this.generateWeb(methodName);
		}
		
		unionWebs();
		removeRedundantWebs();
		indexWebs(); // Don't use this to generate interference graph
		removeDeadCodedInstructions();
	}

	private void removeDeadCodedInstructions() {		
		for (String methodName: this.mMap.keySet()) {
			for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
				List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
				
				for (LIRStatement stmt: block.getStatements()) {
					if (!stmt.isDead()) {
						newStmts.add(stmt);
					}
				}
				
				block.setStatements(newStmts);
			}
			
			this.mMap.get(methodName).regenerateStmts();
		}
	}

	private void unionWebs() {
		for (String methodName: this.webMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			List<Web> webs = this.webMap.get(methodName);
			List<Web> oldWebs = new ArrayList<Web>();
			while (webs.size() != oldWebs.size()) {
				oldWebs = new ArrayList<Web>();
				oldWebs.addAll(webs);
				
				Web a = null;
				Web b = null;
				
				outter:
				for (Web w1: webs) {
					for (Web w2: webs) {
						if (w1 == w2) continue;
						
						if (w1.getVariable().equals(w2.getVariable())) { // Same variable web
							// Check for common use
							for (LIRStatement u1: w1.getUses()) {
								for (LIRStatement u2: w2.getUses()) {
									if (u1 == u2) { 								
										a = w1;
										b = w2;
										break outter;
									}
								}
							}
						}
					}
				}
				
				if (a != null && b != null) { // Union
					webs.remove(b);
					a.combineWeb(b);
				}
			}
		}
	}

	private void indexWebs() {
		for (String methodName: this.webMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			List<Web> webs = this.webMap.get(methodName);
			
			for (Web w: webs) {
				int min = this.mMap.get(methodName).getStatements().size(); // Max index
				int max = -1; // Min index
				
				int index;
				for (LIRStatement stmt: w.getDefinitions()) {
					index = getStmtIndex(methodName, stmt);
					if (index > max) max = index;
					if (index < min) min = index;
				}
				
				for (LIRStatement stmt: w.getUses()) {
					index = getStmtIndex(methodName, stmt);
					if (index > max) max = index;
					if (index < min) min = index;
				}
				
				w.setFirstStmtIndex(min);
				w.setLastStmtIndex(max);
			}
		}
	}

	private void removeRedundantWebs() {
		List<Web> temp = new ArrayList<Web>();
		for (String methodName: this.webMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (Web w: this.webMap.get(methodName)) {
				if (w.getVariable().getClass().equals(VarName.class)) { // Strings are refs only and unmutable
					VarName v = (VarName)w.getVariable();
					if (v.isString()) { 
						temp.add(w);
					}
				}
				
				if (w.getUses().isEmpty()) { // Empty webs
					if (w.getDefinitions().isEmpty()) { // No def
						temp.add(w);
					}
				}
				
				// Not global web with only single def and no use (DC!)
				if (w.getDefinitions().size() == 1 && w.getUses().isEmpty()) {
					if (!w.getVariable().isGlobal()) {
						//temp.add(w);
						//w.getDefinitions().get(0).setDead(true);
					}
				}
				
				// Loads/Stores only
				boolean onlyLoads = true, onlyStores = true;
				for (LIRStatement def: w.getDefinitions()) { // Only load defs
					if (!def.getClass().equals(LoadStmt.class)) {
						onlyLoads = false;
						break;
					}
				}
				
				for (LIRStatement use: w.getUses()) {
					if (!use.getClass().equals(StoreStmt.class)) { // Only store uses
						onlyStores = false;
						break;
					}
				}
				
				if (onlyLoads && onlyStores) {
					for (LIRStatement def: w.getDefinitions()) { // Only load defs
						def.setDead(true);
					}
					
					for (LIRStatement use: w.getUses()) {
						use.setDead(true);
					}
					
					temp.add(w);
				}
			}
			
			for (Web web: temp) { // Remove redundant webs (also fix interference graph)
				List<Web> remove = new ArrayList<Web>();
				for (Web w: web.getInterferingWebs()) {
					remove.add(w);
				}
				for (Web w: remove) {
					w.removeInterferingWeb(web);
				}
				
				this.webMap.get(methodName).remove(web);
			}
			
			temp.clear();
		}
	}

	/**
	 * Some uses will not have definitions!
	 * E.g. use of global vars before assignment, or params on stack
	 * @param methodName
	 */
	private void generateWeb(String methodName) {
		this.defToWeb.clear();
		
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			if (block == null) continue;
			
			processBlock(block);
		}
	}

	private void processBlock(CFGBlock block) {
		this.nameToWebs.clear(); // Clear names to web map
		
//		System.out.println("START: " + this.nameToWebs);
		
		setReachingDefinitions(block);
		
//		System.out.println("BLOW: " + this.nameToWebs);
		String mName = block.getMethodName();
		
		for (int i = 0; i < block.getStatements().size(); i++) {
			LIRStatement stmt = block.getStatements().get(i);
			
//			System.out.println("*************");
//			System.out.println(stmt);
//			System.out.println(stmt.getInSet());
			
//			System.out.println("STMT: " + stmt);
//			System.out.println("LIVE: " + stmt.getLiveInSet());
//			System.out.println("REACH: " + stmt.getReachingDefInSet() + "\n");
			
			updateLiveWebs(block, i);
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				Name arg1 = qStmt.getArg1();
				Name arg2 = qStmt.getArg2();
				Name dest = qStmt.getDestination();
				
				if (isValidWebName(mName, arg1)) {
					for (Web w: this.nameToWebs.get(arg1)) {
						addToInterferingGraph(w, stmt);
						w.addUse(qStmt);
					}
				}
				
				if (isValidWebName(mName, arg2)) {
					for (Web w: this.nameToWebs.get(arg2)) {
						addToInterferingGraph(w, stmt);
						w.addUse(qStmt);
					}
				}
				
				if (isValidWebName(mName, dest)) {		
					this.nameToWebs.put(dest, new ArrayList<Web>());
					
					if (!this.defToWeb.containsKey(qStmt)) {
						Web w = new Web(dest);
						w.addDefinition(qStmt);
						this.defToWeb.put(qStmt, w);
						this.webMap.get(mName).add(w); // Add to global web map for method
					}
					
					Web w = this.defToWeb.get(qStmt);
					this.nameToWebs.get(dest).add(w);
					addToInterferingGraph(w, stmt);
				}
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt)stmt;
				Name arg1 = cStmt.getArg1();
				Name arg2 = cStmt.getArg2();
				
				if (isValidWebName(mName, arg1)) {
					for (Web w: this.nameToWebs.get(arg1)) {
						addToInterferingGraph(w, stmt);
						w.addUse(cStmt);
					}
				}
				
				if (isValidWebName(mName, arg2)) {
					for (Web w: this.nameToWebs.get(arg2)) {
						addToInterferingGraph(w, stmt);
						w.addUse(cStmt);
					}
				}
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;
				Name arg = pStmt.getName();
				
				if (isValidWebName(mName, arg)) {
					for (Web w: this.nameToWebs.get(arg)) {
						addToInterferingGraph(w, stmt);
						w.addUse(pStmt);
					}
				}
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;
				Name arg = pStmt.getName();
				
				if (isValidWebName(mName, arg)) {
					for (Web w: this.nameToWebs.get(arg)) {
						addToInterferingGraph(w, stmt);
						w.addUse(pStmt);
					}
				}
			}
			else if (stmt.getClass().equals(LoadStmt.class)) {
				LoadStmt lStmt = (LoadStmt) stmt;
				Name dest = lStmt.getVariable();
				
				if (isValidWebName(mName, dest)) {
					this.nameToWebs.put(dest, new ArrayList<Web>());
					
					if (!this.defToWeb.containsKey(lStmt)) {
						Web w = new Web(dest);
						w.addDefinition(lStmt);
						this.defToWeb.put(lStmt, w);
						this.webMap.get(mName).add(w); // Add to global web map for method
					}
					
					Web web = this.defToWeb.get(lStmt);
					this.nameToWebs.get(dest).add(web);
					addToInterferingGraph(web, stmt);
					
					if (dest.isArray()) {
						ArrayName aDest = (ArrayName)dest;
						if (isValidWebName(mName, aDest.getIndex())) { // Add use for index variable if load is for array
							for (Web w: this.nameToWebs.get(aDest.getIndex())) {
								addToInterferingGraph(w, stmt);
								w.addUse(lStmt);
							}
						}
					}
				}
			}
			else if (stmt.getClass().equals(StoreStmt.class)) {
				StoreStmt sStmt = (StoreStmt) stmt;
				Name dest = sStmt.getVariable();
				
				if (isValidWebName(mName, dest)) {
					for (Web w: this.nameToWebs.get(dest)) {
						addToInterferingGraph(w, stmt);
						w.addUse(sStmt);
					}
				}
				
				if (dest.isArray()) { // Add use for index variable if store is for array
					ArrayName aDest = (ArrayName)dest;
					if (isValidWebName(mName, aDest.getIndex())) {
						for (Web w: this.nameToWebs.get(aDest.getIndex())) {
							addToInterferingGraph(w, stmt);
							w.addUse(sStmt);
						}
					}
				}
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				CallStmt cStmt = (CallStmt) stmt;
				if (cStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				
				invalidateFunctionCall();
			}
		}
	}

	private void updateLiveWebs(CFGBlock block, int i) {
		LIRStatement stmt = block.getStatements().get(i);
		
		this.namesDeadAtOutStmt.clear();
		
		if (block.getStatements().size() > i+1) { // Has next statement
			LIRStatement next = block.getStatements().get(i+1);
			
			for (int j = 0; j < this.liveAnalysis.getUniqueVariables().get(this.currentMethod).size(); j++) {
				Name name = this.liveAnalysis.getUniqueVariables().get(this.currentMethod).get(j);
				
				if (stmt.getLiveInSet().get(j) && !next.getLiveInSet().get(j)) { // Live now but not live at start of next stmt
					this.namesDeadAtOutStmt.add(name);
				}
			}
		}
		
		tryReducingInterference(stmt);
		
		for (int j = 0; j < this.liveAnalysis.getUniqueVariables().get(this.currentMethod).size(); j++) {
			Name name = this.liveAnalysis.getUniqueVariables().get(this.currentMethod).get(j);
			
			if (!stmt.getLiveInSet().get(j)) { // Not live at in, so clear all *reaching* webs
				this.nameToWebs.remove(name);
			}
		}
	}

	// Tries to remove webs which can't interfere with because this statement is last use
	private void tryReducingInterference(LIRStatement stmt) {
		if (this.namesDeadAtOutStmt.isEmpty()) return; // Can't do anything
		
		List<Name> temp = new ArrayList<Name>();
		
		if (stmt.getClass().equals(QuadrupletStmt.class)) {
			QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
			
			// Unary
			if (qStmt.getOperator() == QuadrupletOp.NOT || qStmt.getOperator() == QuadrupletOp.MINUS) {
				this.nameToWebs.remove(qStmt.getArg1()); // If last use of arg, can reuse its register
			}
			// Binary
			else {
				// Divide/Mod is a bitch, no idea how to handle at this point
				if (qStmt.getOperator() != QuadrupletOp.DIV && qStmt.getOperator() != QuadrupletOp.MOD) {
					if (this.namesDeadAtOutStmt.contains(qStmt.getArg1())) {
						//this.nameToWebs.remove(qStmt.getArg1()); // If last use of arg1, reuse reg
						temp.add(qStmt.getArg1());
					}
					
					if (qStmt.getOperator() == QuadrupletOp.ADD || qStmt.getOperator() == QuadrupletOp.MUL) {
						if (this.namesDeadAtOutStmt.contains(qStmt.getArg2())) {
//							//this.nameToWebs.remove(qStmt.getArg2()); // If last use of arg2, then switch with arg1 and reuse reg
//							temp.add(qStmt.getArg2());
//							Name arg1 = qStmt.getArg1();
//							qStmt.setArg1(qStmt.getArg2());
//							qStmt.setArg2(arg1);
						}
					}
					// For conditionals can reuse either
					else if (qStmt.getOperator() != QuadrupletOp.MINUS) {
						if (this.namesDeadAtOutStmt.contains(qStmt.getArg2())) {
							//this.nameToWebs.remove(qStmt.getArg2());
							temp.add(qStmt.getArg2());
						}
					}
				}
			}
		}
		else if (stmt.getClass().equals(LoadStmt.class)) {
			LoadStmt lStmt = (LoadStmt) stmt;
			if (lStmt.getVariable().isArray()) {
				ArrayName aName = (ArrayName) lStmt.getVariable(); // If index variable dead after this use, then reuse reg
				//this.nameToWebs.remove(aName.getIndex());
				temp.add(aName.getIndex());
			}
		}
		else if (stmt.getClass().equals(StoreStmt.class)) {
			StoreStmt sStmt = (StoreStmt) stmt;
			if (sStmt.getVariable().isArray()) {
				ArrayName aName = (ArrayName) sStmt.getVariable(); // If index variable dead after this use, then reuse reg
				//this.nameToWebs.remove(aName.getIndex());
				temp.add(aName.getIndex());
			}
		}
		
		this.namesDeadAtOutStmt.clear();
		this.namesDeadAtOutStmt.addAll(temp);
	}

	private void invalidateFunctionCall() {
		for (Name name: this.nameToWebs.keySet()) {
			if (name.isGlobal()) {
				this.nameToWebs.get(name).clear();
			}
		}
	}

	/**
	 * Adds to all *live* webs except for ones for arg.
	 * When adding use, don't want to interfere same names as will merge webs later
	 * When defining, don't want to interfere as can reuse that register now (qArg reordering ensures that!)
	 * @param web
	 * @param stmt
	 */
	private void addToInterferingGraph(Web web, LIRStatement stmt) {
		//Name skip = web.getVariable();
	
		for (Name name: this.nameToWebs.keySet()) {
			//if (name.equals(skip)) continue; // Don't interfere with own webs
			
			for (Web w: this.nameToWebs.get(name)) {
				//System.out.println("TRY: "+ name+ " with " + w.getVariable());
				if (this.namesDeadAtOutStmt.contains(name)) continue;
				
				//System.out.println("INTERFERING: "+ name+ " with " + w.getVariable());
				//System.out.println("***************");
				
				w.addInterferingWeb(web);
			}
			
		}
		
//		for (int i = 0; i < this.liveAnalysis.getUniqueVariables().get(this.currentMethod).size(); i++) {
//			if (!stmt.getInSet().get(i)) continue; // Not live
//			
//			Name name = this.liveAnalysis.getUniqueVariables().get(this.currentMethod).get(i);
//			
//			//if (name == skip || !this.nameToWebs.containsKey(name)) continue; // Skip own shit, or undefined
//			if (!this.nameToWebs.containsKey(name)) continue; // Skip undefined
//			
//			for (Web w: this.nameToWebs.get(name)) {
//				//System.out.println("ADD : " + w.getIdentifier() + ", "+  web.getIdentifier());
//				w.addInterferingWeb(web);
//			}
//		}
	}

	private void setReachingDefinitions(CFGBlock block) {
		BlockDataFlowState dfState = this.reachingDef.getCfgBlocksState().get(block);
		
		for (int i = 0; i < this.reachingDef.getUniqueDefinitions().get(block.getMethodName()).size(); i++) {
			if (dfState.getIn().get(i)) {				
				LIRStatement stmt = findStmtWithId(block.getMethodName(), i);
				
				Name dest = null;
				if (stmt.getClass().equals(LoadStmt.class)) {
					dest =  ((LoadStmt)stmt).getVariable();
				}
				else if (stmt.getClass().equals(QuadrupletStmt.class)) {
					dest =  ((QuadrupletStmt)stmt).getDestination();
				}
				
				// If definition has no Web currently associated to it
				if (!this.defToWeb.containsKey(stmt)) {
					Web w = new Web(dest);
					w.addDefinition(stmt);
					this.defToWeb.put(stmt, w);
					this.webMap.get(block.getMethodName()).add(w); // Add to global web map for method
				}

				// Associate web to name
				if (!this.nameToWebs.containsKey(dest)) {
					this.nameToWebs.put(dest, new ArrayList<Web>());
				}
				
				this.nameToWebs.get(dest).add(this.defToWeb.get(stmt)); // All webs reaching for that name (will merge later)
			}
		}
	}
	
	private LIRStatement findStmtWithId(String methodName, int id) {
		for (LIRStatement stmt: this.reachingDef.getUniqueDefinitions().get(methodName)) {
			
			if (stmt.getClass().equals(LoadStmt.class)) {
				if (((LoadStmt)stmt).getMyId() == id) return stmt;
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				if (((QuadrupletStmt)stmt).getMyId() == id) return stmt;
			}
		}
		
		return null;
	}

	public HashMap<String, List<Web>> getWebMap() {
		return webMap;
	}

	public void setWebMap(HashMap<String, List<Web>> webMap) {
		this.webMap = webMap;
	}
	
	private boolean isValidWebName(String methodName, Name name) {
		if (name != null && 
				!name.getClass().equals(RegisterName.class) && 
				!name.getClass().equals(ConstantName.class)) {
			
			if (name.getClass().equals(VarName.class)) {
				VarName var = (VarName) name;
				if (var.isString()) return false;
			}
			
			// Process for Global, Param (on stack)
			if (!this.nameToWebs.containsKey(name)) {
				this.nameToWebs.put(name, new ArrayList<Web>());
				Web w = new Web(name);
				this.nameToWebs.get(name).add(w);
				this.webMap.get(methodName).add(w);
			}
			
			return true;
		}
		
		return false;
	}
	
	private int getStmtIndex(String methodName, LIRStatement stmt) {
		for (int i = 0; i < this.mMap.get(methodName).getStatements().size(); i++) {
			if (stmt == this.mMap.get(methodName).getStatements().get(i)) {
				return i;
			}
		}
		
		return -1;
	}
	
	public LivenessAnalysis getLivenessAnalyzer() {
		return this.liveAnalysis;
	}

	public ReachingDefinitions getDefAnalyzer() {
		return reachingDef;
	}

	public void setDefAnalyzer(ReachingDefinitions defAnalyzer) {
		this.reachingDef = defAnalyzer;
	}
	
	public void printInterferenceGraph(PrintStream out) {
		for (String methodName: this.webMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			out.println(methodName + ":");
			for (Web w: this.webMap.get(methodName)) {
				String rtn = "";
				rtn += w.getIdentifier() + " : [";
				for (Web w2: w.getInterferingWebs()) {
					rtn += w2.getIdentifier() + ", ";
				}
				rtn += "]";
				out.println(rtn);
			}
		}
	}
	
	public void printInterferenceGraph(PrintStream out, String methodName) {
		for (Web w: this.webMap.get(methodName)) {
			String rtn = "";
			rtn += w.getIdentifier() + " : [";
			for (Web w2: w.getInterferingWebs()) {
				rtn += w2.getIdentifier() + ", ";
			}
			rtn += "]";
			out.println(rtn);
		}
	}
}
