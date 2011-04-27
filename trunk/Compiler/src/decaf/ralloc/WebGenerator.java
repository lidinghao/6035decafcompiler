package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class WebGenerator {
	private HashMap<String, MethodIR> mMap;
	private DefDataFlowAnalyzer defAnalyzer;
	private HashMap<String, List<Web>> webMap;
	private HashMap<Name, List<Web>> nameToWebs;
	private HashMap<QuadrupletStmt, Web> defToWeb;
	
	public WebGenerator(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.defAnalyzer = new DefDataFlowAnalyzer(mMap);
		this.webMap = new HashMap<String, List<Web>>();
		this.nameToWebs = new HashMap<Name, List<Web>>();
		this.defToWeb = new HashMap<QuadrupletStmt, Web>();
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
							
							// Check for common *def* for non-defined vars (global, stack params)
							if (w1.loadExplicitly() && w2.loadExplicitly()) {
								a = w1;
								b = w2;
								break outter;
							}
						}
					}
				}
				
				if (a != null && b != null) { // Union
					webs.remove(b);
					a.getDefinitions().addAll(b.getDefinitions());
					a.getUses().addAll(b.getUses());
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
				int max = 0; // Min index
				
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
				
				if (w.getDefinitions().isEmpty()) {
					min = 0; // Already available in method when you enter it
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
				
				if (w.getDefinitions().isEmpty() && w.getUses().isEmpty()) { // Empty webs
					temp.add(w);
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
			
			this.nameToWebs.clear(); // Clear names to web map
			processBlock(block);
		}
	}

	private void processBlock(CFGBlock block) {
		setReachingDefinitions(block);
		String mName = block.getMethodName();
		
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.isUseStatement()) continue;
			
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
		}
	}

	private void setReachingDefinitions(CFGBlock block) {
		BlockDataFlowState dfState = this.defAnalyzer.getCfgBlocksState().get(block);
		
		for (int i = 0; i < dfState.getIn().size(); i++) {
			if (dfState.getIn().get(i)) {
				QuadrupletStmt stmt = findStmtWithId(block.getMethodName(), i);
				
				// If definition has no Web currently associated to it
				if (!this.defToWeb.containsKey(stmt)) {
					Web w = new Web(stmt.getDestination());
					w.addDefinition(stmt);
					this.defToWeb.put(stmt, w);
					this.webMap.get(block.getMethodName()).add(w); // Add to global web map for method
				}

				// Associate web to name
				if (!this.nameToWebs.containsKey(stmt.getDestination())) {
					this.nameToWebs.put(stmt.getDestination(), new ArrayList<Web>());
				}
				
				this.nameToWebs.get(stmt.getDestination()).add(this.defToWeb.get(stmt)); // All webs reaching for that name (will merge later)
			}
		}
	}
	
	private QuadrupletStmt findStmtWithId(String methodName, int id) {
		for (QuadrupletStmt stmt: this.defAnalyzer.getUniqueDefinitions().get(methodName)) {
			if (stmt.getMyId() == id) return stmt;
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
		if (name != null && !name.getClass().equals(RegisterName.class) && !name.getClass().equals(ConstantName.class)) {
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
}
