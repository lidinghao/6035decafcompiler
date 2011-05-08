package decaf.dataflow.cfg;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flattener.ProgramFlattener;

public class CFGBuilder {
	private ProgramFlattener pf;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private LeaderElector le;
	private boolean mergeBoundChecks;

	public CFGBuilder(ProgramFlattener pf) {
		this.pf = pf;
		this.cfgMap = new HashMap<String, List<CFGBlock>>();
		le = new LeaderElector(pf);
		this.setMergeBoundChecks(false);
	}
	
	public HashMap<String, List<CFGBlock>> getCfgMap() {
		return cfgMap;
	}

	public void setCfgMap(HashMap<String, List<CFGBlock>> cfgMap) {
		this.cfgMap = cfgMap;
	}
	
	public void generateCFGs() {
		// Select leaders
		le.setMergeBoundChecks(this.mergeBoundChecks);
		le.electLeaders();
		
		cfgMap.clear();
		
		for (String methodName: pf.getLirMap().keySet()) {
			List<CFGBlock> cfgList = generateCFGBlocks(methodName);
			generateCFG(cfgList);
			cfgMap.put(methodName, cfgList);
		}
	}

	private List<CFGBlock> generateCFGBlocks(String methodName) {
		List<CFGBlock> cfgList = new ArrayList<CFGBlock>();
		CFGBlock cfg = null;
		int i = 0;
		
		for (LIRStatement stmt: this.pf.getLirMap().get(methodName)) {
			if (stmt.isLeader()) {
				if (cfg != null) cfgList.add(cfg);
				cfg = new CFGBlock(methodName);
				cfg.setIndex(i); // Set index
				cfg.setLeader(stmt); // Set leader
				cfg.setStatements(new ArrayList<LIRStatement>()); // Initialize statements
				i++;
			}
			
			cfg.addStatement(stmt);
		}
		
		if (cfg != null) cfgList.add(cfg);
		
		return cfgList;
	}
	
	private void generateCFG(List<CFGBlock> list) {
		for (CFGBlock cfg: list) {
			LIRStatement stmt = cfg.getStatements().get(cfg.getStatements().size() - 1);
			CFGBlock blk;
			if (stmt.getClass().equals(JumpStmt.class)) {
				JumpStmt jmp = (JumpStmt) stmt;
				if (jmp.getCondition() != JumpCondOp.NONE) {
					blk = getCFGWithIndex(list, cfg.getIndex() + 1);
					if (blk != null) {
						cfg.addSuccessor(blk);
						blk.addPredecessor(cfg);
					}
				}
				
				blk = getCFGWithLeaderLabel(list, jmp.getLabel().getLabelString());
				cfg.addSuccessor(blk);
				blk.addPredecessor(cfg);
			}
			else {
				blk = getCFGWithIndex(list, cfg.getIndex() + 1);
				if (blk != null) {
					cfg.addSuccessor(blk);
					blk.addPredecessor(cfg);
				}
			}
		}
	}
	
	private CFGBlock getCFGWithIndex(List<CFGBlock> list, int i) {
		for (CFGBlock cfg: list) {
			if (cfg.getIndex() == i) { 
				return cfg;
			}
		}
		
		return null;
	}
	
	private CFGBlock getCFGWithLeaderLabel(List<CFGBlock> list, String label) {
		for (CFGBlock cfg: list) {
			if (cfg.getLeader().getClass().equals(LabelStmt.class)) {
				LabelStmt stmt = (LabelStmt) cfg.getLeader();
				if (stmt.getLabelString().equals(label)) {
					return cfg;
				}
			}
		}
		
		return null;
	}
	
	public void printCFG(PrintStream out) {
		for (String s: cfgMap.keySet()) {
			out.println(s + ":");
			List<CFGBlock> cfgList = cfgMap.get(s);
			for (CFGBlock cfg: cfgList) {
				out.println(cfg + "\n");
			}
		}
	}

	public void setMergeBoundChecks(boolean mergeBoundChecks) {
		this.mergeBoundChecks = mergeBoundChecks;
	}

	public boolean isMergeBoundChecks() {
		return mergeBoundChecks;
	}

	public LeaderElector getLeaderElector() {
		return le;
	}
}
