package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.global.BlockDataFlowState;

public class WebGenerator {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private DefDataFlowAnalyzer defAnalyzer;
	private HashMap<String, List<Web>> webMap;
	private HashMap<Name, List<QuadrupletStmt>> nameToDef;
	private HashMap<QuadrupletStmt, Web> defToWeb;
	
	public WebGenerator(HashMap<String, List<CFGBlock>> cfgMap) {
		this.cfgMap = cfgMap;
		this.defAnalyzer = new DefDataFlowAnalyzer(cfgMap);
		this.webMap = new HashMap<String, List<Web>>();
		this.nameToDef = new HashMap<Name, List<QuadrupletStmt>>();
		this.defToWeb = new HashMap<QuadrupletStmt, Web>();
	}
	
	public void generateWebs() {
		this.defAnalyzer.analyze();
		System.out.println("SIZE: " + this.defAnalyzer.getCfgBlocksState().size());
		System.out.println(this.defAnalyzer.getCfgBlocksState());
		
		for (String methodName: this.cfgMap.keySet()) {
			this.webMap.put(methodName, new ArrayList<Web>());
			this.generateWeb(methodName);
		}
	}
	
	private void generateWeb(String methodName) {		
		if (this.defAnalyzer.getUniqueDefinitions().get(methodName) == null) return;
		
		this.defToWeb.clear();
		
		for (QuadrupletStmt stmt: this.defAnalyzer.getUniqueDefinitions().get(methodName)) {
			Web w = new Web(stmt.getDestination());
			w.addDefinition(stmt);
			this.defToWeb.put(stmt, w);
			this.webMap.get(methodName).add(w);
		}
		
		for (CFGBlock block: this.cfgMap.get(methodName)) {
			if (block == null) continue;
			
			this.nameToDef.clear();
			processBlock(block);
		}
	}

	private void processBlock(CFGBlock block) {
		setReachingDefinitions(block);
		
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.isUseStatement()) continue;
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				Name arg1 = qStmt.getArg1();
				Name arg2 = qStmt.getArg2();
				Name dest = qStmt.getDestination();
				
				if (arg1 != null) {
					if (!this.nameToDef.containsKey(arg1)) break;
					for (QuadrupletStmt s: this.nameToDef.get(arg1)) {
						this.defToWeb.get(s).addUse(qStmt);
					}
				}
				
				if (arg2 != null) {
					if (!this.nameToDef.containsKey(arg2)) break;
					for (QuadrupletStmt s: this.nameToDef.get(arg2)) {
						this.defToWeb.get(s).addUse(qStmt);
					}
				}
				
				this.nameToDef.get(dest).clear(); // Reset reaching defs
				this.nameToDef.get(dest).add(qStmt);
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt)stmt;
				Name arg1 = cStmt.getArg1();
				Name arg2 = cStmt.getArg2();
				
				if (arg1 != null) {
					if (!this.nameToDef.containsKey(arg1)) break; // Const, Reg, Global
					for (QuadrupletStmt s: this.nameToDef.get(arg1)) {
						this.defToWeb.get(s).addUse(cStmt);
					}
				}
				
				if (arg2 != null) {
					if (!this.nameToDef.containsKey(arg2)) break;
					for (QuadrupletStmt s: this.nameToDef.get(arg2)) {
						this.defToWeb.get(s).addUse(cStmt);
					}
				}
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;
				Name arg = pStmt.getName();
				
				if (arg != null) {
					if (!this.nameToDef.containsKey(arg)) break;
					for (QuadrupletStmt s: this.nameToDef.get(arg)) {
						this.defToWeb.get(s).addUse(pStmt);
					}
				}
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;
				Name arg = pStmt.getName();
				
				if (arg != null) {
					if (!this.nameToDef.containsKey(arg)) break;
					for (QuadrupletStmt s: this.nameToDef.get(arg)) {
						this.defToWeb.get(s).addUse(pStmt);
					}
				}
			}
		}
	}

	private void setReachingDefinitions(CFGBlock block) {
		BlockDataFlowState dfState = this.defAnalyzer.getCfgBlocksState().get(block);
		System.out.println(block);
		System.out.println(dfState);
		for (int i = 0; i < dfState.getIn().size(); i++) {
			if (dfState.getIn().get(i)) {
				QuadrupletStmt stmt = findStmtWithId(block.getMethodName(), i);
				if (!this.nameToDef.containsKey(stmt.getDestination())) {
					this.nameToDef.put(stmt.getDestination(), new ArrayList<QuadrupletStmt>());
				}
				
				this.nameToDef.get(stmt.getDestination()).add(stmt); // This def for var name reaches block
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
}
