package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class GlobalExplicitLoader {
	//private static String ForTestLabelRegex = "[a-zA-z_]\\w*_for\\d+_test";
	private static String IfEndLabelRegex = "[a-zA-z_]\\w*_if\\d+_end";
	private GlobalsDefDFAnalyzer df;
	private HashMap<String, MethodIR> mMap;
	private HashSet<Name> globalsInBlock;
	private boolean seenCall;
	private HashMap<CFGBlock, String> blockState;

	public GlobalExplicitLoader(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.df = new GlobalsDefDFAnalyzer(mMap);
		this.blockState = new HashMap<CFGBlock, String>();
		this.globalsInBlock = new HashSet<Name>();
		this.seenCall = false;
	}

	public void execute() {
		this.df.analyze();

		for (String methodName : this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			processMethod(methodName);
			this.mMap.get(methodName).regenerateStmts();
		}
	}

	public void processMethod(String methodName) {
		this.blockState.clear();

		int i = 100;
		while (i > 0) {
			for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
				this.blockState.put(block, block.toString());
			}
			
			for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
				if (processBlock(block)) { // Returns true if added some load stmt
					break;
				}
			}

			this.df.analyze();
			this.mMap.get(methodName).regenerateStmts();

			boolean isChanged = false;
			for (CFGBlock block : this.mMap.get(methodName).getCfgBlocks()) {
				if (!block.toString().equals(this.blockState.get(block).toString())) {
					isChanged = true;
					break;
				}
			}

			if (!isChanged) {
				break;
			}
			i--;
			
		}
	}

	private boolean processBlock(CFGBlock block) {
		this.globalsInBlock.clear();
		this.seenCall = false;
		
		for (int i = 0; i < block.getStatements().size(); i++) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(LoadStmt.class)) {
				this.globalsInBlock.add(((LoadStmt)stmt).getVariable());
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				
				killLocalGlobals(qStmt);
				
				if (qStmt.getDestination().isGlobal()) {
					this.globalsInBlock.add(qStmt.getDestination());
				}

				if (qStmt.getArg1().isGlobal()) {
					if (processName(qStmt.getArg1(), block, i))
						return true;
				}
				if (qStmt.getArg2() != null && qStmt.getArg2().isGlobal()) {
					if (processName(qStmt.getArg2(), block, i))
						return true;
				}
			} else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				if (cStmt.getArg1().isGlobal()) {
					if (processName(cStmt.getArg1(), block, i))
						return true;
				}
				if (cStmt.getArg2().isGlobal()) {
					if (processName(cStmt.getArg2(), block, i))
						return true;
				}
			} else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;

				if (pStmt.getName().isGlobal()) {
					if (processName(pStmt.getName(), block, i))
						return true;
				}
			} else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;

				if (pStmt.getName().isGlobal()) {
					if (processName(pStmt.getName(), block, i))
						return true;
				}
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				if (((CallStmt)stmt).getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				this.globalsInBlock.clear();
				this.seenCall = true;
			}
		}

		return false;
	}

	private void killLocalGlobals(QuadrupletStmt qStmt) {
		HashSet<Name> remove = new HashSet<Name>();
		for (Name name: this.globalsInBlock) {
			if (name.isArray()) {
				ArrayName array = (ArrayName) name;
				if (array.getIndex().equals(qStmt.getDestination())) {
					remove.add(array);
				}
			}
		}
		
		this.globalsInBlock.removeAll(remove);
	}

	private boolean processName(Name name, CFGBlock block, int index) {
		if (name.getClass().equals(VarName.class)) {
			VarName var = (VarName)name;
			if (var.isString()) return false;
		}
		
		if (this.globalsInBlock.contains(name)) return false;
		
		if (this.seenCall) {
			block.getStatements().add(index, new LoadStmt(name));
			this.globalsInBlock.add(name);
			
			return true;
		}
		
		List<Name> uniqueGlobals = this.df.getUniqueGlobals().get(block.getMethodName());
		int i = uniqueGlobals.indexOf(name);
		
		int predCount = 0;
		CFGBlock predToChange = null;
		for (CFGBlock b: block.getPredecessors()) {
			BlockDataFlowState state = this.df.getCfgBlocksState().get(b);
			if (!state.getOut().get(i)) {
				predCount++;
				predToChange = b;
			}
		}
		
		if (predCount == block.getPredecessors().size()) { // If all predecessors dont have it
			block.getStatements().add(index, new LoadStmt(name));
			this.globalsInBlock.add(name);
			
			return true;
		}
		else if (predToChange != null) {
			addLoadStmt(name, predToChange, block);
			return true;
		}
		
		return false;
	}

	private void addLoadStmt(Name name, CFGBlock block, CFGBlock self) {
//		if (block.getStatements().get(0).getClass().equals(LabelStmt.class)) {
//			LabelStmt label = (LabelStmt)block.getStatements().get(0);
//			if (label.getLabelString().matches(GlobalExplicitLoader.ForTestLabelRegex)) {
//				block = block.getPredecessors().get(0);
//			}
//		}
		if (block.getStatements().get(0).getClass().equals(JumpStmt.class)) {
			JumpStmt jmp = (JumpStmt)block.getStatements().get(0);
			if (jmp.getLabel().getLabelString().matches(GlobalExplicitLoader.IfEndLabelRegex)) {
				block = block.getPredecessors().get(0);
			}
		}
		
		ArrayList<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		boolean added = false;
		for (int i = block.getStatements().size()-1; i >=0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			if (!stmt.getClass().equals(JumpStmt.class) && !stmt.getClass().equals(CmpStmt.class)) {
				if (!added) {
					newStmts.add(0, new LoadStmt(name));
					added = true;
				}
			}
			
			newStmts.add(0, stmt);
		}
		
		block.setStatements(newStmts);
	}

	public void setGlobalDefAnalyzer(GlobalsDefDFAnalyzer df) {
		this.df = df;
	}

	public GlobalsDefDFAnalyzer getDf() {
		return df;
	}
}
