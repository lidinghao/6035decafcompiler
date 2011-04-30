package decaf.dataflow.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flattener.ProgramFlattener;

public class MethodIR {
	private static ProgramFlattener pf;
	private String id;
	private List<LIRStatement> statements;
	private List<CFGBlock> cfgBlocks;
	
	public MethodIR(String id, List<LIRStatement> statements, List<CFGBlock> cfgBlocks) {
		this.id = id;
		this.statements = statements;
		this.cfgBlocks = cfgBlocks;
	}
	
	public void regenerateStmts() {
		// Change statements
		List<LIRStatement> stmts = new ArrayList<LIRStatement>();
		
		for (int i = 0; i < this.cfgBlocks.size(); i++) {
			for (LIRStatement stmt : getBlockWithIndex(i).getStatements()) {
				if (stmt != null) {
					stmts.add(stmt);
				}
			}
		}
		
		this.statements = stmts;
		pf.getLirMap().put(id, stmts);
	}
	
	private CFGBlock getBlockWithIndex(int i) {
		for (CFGBlock block: this.cfgBlocks) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		
		return null;
	}

	public List<CFGBlock> getCfgBlocks() {
		return cfgBlocks;
	}

	public void setCfgBlocks(List<CFGBlock> cfgBlocks) {
		this.cfgBlocks = cfgBlocks;
	}

	public void setStatements(List<LIRStatement> statements) {
		this.statements = statements;
		pf.getLirMap().put(id, this.statements);
	}

	public List<LIRStatement> getStatements() {
		return statements;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public static HashMap<String, MethodIR> generateMethodIRs(ProgramFlattener pf, HashMap<String, List<CFGBlock>> cfgMap) {
		MethodIR.pf = pf;
		HashMap<String, MethodIR> rtn = new HashMap<String, MethodIR>();
		
		for (String id: pf.getLirMap().keySet()) {
			rtn.put(id, new MethodIR(id, pf.getLirMap().get(id), cfgMap.get(id)));
		}
		
		return rtn;
	}
}
