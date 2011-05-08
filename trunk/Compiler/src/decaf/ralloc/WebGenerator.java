package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
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
	private DefsDFAnalyzer defAnalyzer;
	private HashMap<String, List<Web>> webMap;
	private HashMap<Name, List<Web>> nameToWebs; // Webs currently mapping to that name
	private HashMap<LIRStatement, Web> defToWeb; // Web associated with definition
	
	public WebGenerator(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.defAnalyzer = new DefsDFAnalyzer(mMap);
		this.webMap = new HashMap<String, List<Web>>();
		this.nameToWebs = new HashMap<Name, List<Web>>();
		this.defToWeb = new HashMap<LIRStatement, Web>();
	}
	
	public void generateWebs() {
		this.defAnalyzer.analyze();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			this.webMap.put(methodName, new ArrayList<Web>());
			this.generateWeb(methodName);
		}
		
		removeRedundantWebs();
		unionWebs();
		indexWebs();
		
		removeDeadCodedLoads();
	}
	
	private void removeDeadCodedLoads() {		
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
					if (w.getDefinitions().isEmpty()) {
						temp.add(w);
					}
					else if (w.getDefinitions().size() == 1) {
						LIRStatement stmt = w.getDefinitions().get(0);
						if (stmt.getClass().equals(LoadStmt.class)) {
							stmt.setDead(true);
							temp.add(w);
						}
					}
				}
			}
			
			for (Web w: temp) {
				this.webMap.get(methodName).remove(w);
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
		
		setReachingDefinitions(block);
		String mName = block.getMethodName();
		
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				Name arg1 = qStmt.getArg1();
				Name arg2 = qStmt.getArg2();
				Name dest = qStmt.getDestination();
				
				if (isValidWebName(mName, arg1)) {
					for (Web w: this.nameToWebs.get(arg1)) {
						w.addUse(qStmt);
					}
				}
				
				if (isValidWebName(mName, arg2)) {
					for (Web w: this.nameToWebs.get(arg2)) {
						w.addUse(qStmt);
					}
				}
				
				if (isValidWebName(mName, dest)) {		
					this.nameToWebs.get(dest).clear();
					
					if (!this.defToWeb.containsKey(qStmt)) {
						Web w = new Web(dest);
						w.addDefinition(qStmt);
						this.defToWeb.put(qStmt, w);
						this.webMap.get(mName).add(w); // Add to global web map for method
					}
					
					this.nameToWebs.get(dest).add(this.defToWeb.get(qStmt));
				}
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt)stmt;
				Name arg1 = cStmt.getArg1();
				Name arg2 = cStmt.getArg2();
				
				if (isValidWebName(mName, arg1)) {
					for (Web w: this.nameToWebs.get(arg1)) {
						w.addUse(cStmt);
					}
				}
				
				if (isValidWebName(mName, arg2)) {
					for (Web w: this.nameToWebs.get(arg2)) {
						w.addUse(cStmt);
					}
				}
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;
				Name arg = pStmt.getName();
				
				if (isValidWebName(mName, arg)) {
					for (Web w: this.nameToWebs.get(arg)) {
						w.addUse(pStmt);
					}
				}
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;
				Name arg = pStmt.getName();
				
				if (isValidWebName(mName, arg)) {
					for (Web w: this.nameToWebs.get(arg)) {
						w.addUse(pStmt);
					}
				}
			}
			else if (stmt.getClass().equals(LoadStmt.class)) {
				LoadStmt lStmt = (LoadStmt) stmt;
				Name dest = lStmt.getVariable();
				
				if (isValidWebName(mName, dest)) {
					this.nameToWebs.get(dest).clear();
					
					if (!this.defToWeb.containsKey(lStmt)) {
						Web w = new Web(dest);
						w.addDefinition(lStmt);
						this.defToWeb.put(lStmt, w);
						this.webMap.get(mName).add(w); // Add to global web map for method
					}
					
					this.nameToWebs.get(dest).add(this.defToWeb.get(lStmt));
					
					if (dest.isArray()) {
						ArrayName aDest = (ArrayName)dest;
						if (isValidWebName(mName, aDest.getIndex())) { // Add use for index variable if load is for array
							for (Web w: this.nameToWebs.get(aDest.getIndex())) {
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
						w.addUse(sStmt);
					}
				}
				
				if (dest.isArray()) { // Add use for index variable if store is for array
					ArrayName aDest = (ArrayName)dest;
					if (isValidWebName(mName, aDest.getIndex())) {
						for (Web w: this.nameToWebs.get(aDest.getIndex())) {
							w.addUse(sStmt);
						}
					}
				}
			}
		}
	}

	private void setReachingDefinitions(CFGBlock block) {
		BlockDataFlowState dfState = this.defAnalyzer.getCfgBlocksState().get(block);
		
		for (int i = 0; i < dfState.getIn().size(); i++) {
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
		for (LIRStatement stmt: this.defAnalyzer.getUniqueDefinitions().get(methodName)) {
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

	public DefsDFAnalyzer getDefAnalyzer() {
		return defAnalyzer;
	}

	public void setDefAnalyzer(DefsDFAnalyzer defAnalyzer) {
		this.defAnalyzer = defAnalyzer;
	}
}
