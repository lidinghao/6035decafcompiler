package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ExpressionFlattenerVisitor;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class ExplicitGlobalLoader {
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
   private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
   private static String ArrayFailLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.fail";
	private GlobalsDefDFAnalyzer df;
	private HashMap<String, MethodIR> mMap;
	private HashSet<Name> globalsInBlock;
	private boolean seenCall;
	private HashMap<CFGBlock, String> blockState;

	public ExplicitGlobalLoader(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.df = new GlobalsDefDFAnalyzer(mMap);
		this.blockState = new HashMap<CFGBlock, String>();
		this.globalsInBlock = new HashSet<Name>();
		this.seenCall = false;
	}

	public void execute() {
		QuadrupletStmt.setID(0);
		
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
		while (true) {
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

				if (qStmt.getArg1().isGlobal()) {
					if (processName(qStmt.getArg1(), block, i))
						return true;
				}
				if (qStmt.getArg2() != null && qStmt.getArg2().isGlobal()) {
					if (processName(qStmt.getArg2(), block, i))
						return true;
				}
				
				killLocalGlobals(qStmt);
				
				if (qStmt.getDestination().isGlobal()) {
					this.globalsInBlock.add(qStmt.getDestination());
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
			boolean resetName = false;
			
			if (name.isArray()) {
				Name myName = name;
				
				do {
					ArrayName array = (ArrayName) myName;
					if (array.getIndex().equals(qStmt.getDestination())) { // Index being reassigned, KILL!
						resetName = true;
					}
					
					myName = array.getIndex();
					
				} while (myName.isArray());
				
				if (qStmt.getDestination().isArray()) {
					ArrayName dest = (ArrayName) qStmt.getDestination();
					ArrayName arrName = (ArrayName) name;
					if (!arrName.getIndex().getClass().equals(ConstantName.class)) {
						if (arrName.getId().equals(dest.getId())) {
							resetName = true;
						}
					}
				}
			}
			
			if (resetName) {
				remove.add(name);
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
		
		List<Name> uniqueGlobals = this.df.getUniqueGlobals().get(block.getMethodName());
		int i = uniqueGlobals.indexOf(name);
		
		int predCount = 0;
		for (CFGBlock b: block.getPredecessors()) {
			BlockDataFlowState state = this.df.getCfgBlocksState().get(b);
			if (!state.getOut().get(i)) {
				predCount++;
			}
		}
		
		if (this.seenCall || 
				block.getPredecessors().size() == 0 ||
				predCount != 0) {
			LoadStmt load = new LoadStmt(name);
			load.setDepth(block.getStatements().get(index).getDepth()); // set depth
			load.setBoundCheck(getBoundCheck(name, block, index)); // set bound checks
			load.setMyId();
			
			// Check if in array bound check block
			LIRStatement prev = block.getStatements().get(index - 1);
			if (prev != null && prev.getClass().equals(LabelStmt.class)){
				LabelStmt lStmt = (LabelStmt) prev;
				if (lStmt.getLabelString().matches(ExplicitGlobalLoader.ArrayBeginLabelRegex)) {
					index = index - 1;
				}
			}
			
			block.getStatements().add(index, load);
			
			this.globalsInBlock.add(name);
			
			return true;
		}
		
		return false;
	}

	private List<LIRStatement> getBoundCheck(Name name, CFGBlock block, int stmtIndex) {
		if (!name.isArray()) return null;
				
		ArrayName arrName = (ArrayName) name;
		Name index = arrName.getIndex();
		
		boolean inBoundCheck = false;
		boolean inRequiredBC = false;
		int startIndex = -1;
		
		for (int i = stmtIndex; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt)stmt;
				if (lStmt.getLabelString().matches(ExplicitGlobalLoader.ArrayPassLabelRegex) &&
						getArrayIDFromArrayLabelStmt(lStmt, "pass").equals(arrName.getId())) {
					inBoundCheck = true; // Bound check for right array
				}
			}
			
			if (!inBoundCheck) continue;
			
			if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				if (cStmt.getArg1().equals(index) && !inRequiredBC) {
					inRequiredBC = true;
				}
			}
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt)stmt;
				if (lStmt.getLabelString().matches(ExplicitGlobalLoader.ArrayBeginLabelRegex)) {
					if (inRequiredBC) {
						startIndex = i;
						break;
					}
					
					inBoundCheck = false;
				}
			}
		}
		
		List<LIRStatement> stmts = new ArrayList<LIRStatement>();
		for (int i = startIndex; i < block.getStatements().size(); i++) {
			LIRStatement stmt = block.getStatements().get(i);			
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) stmt;
				
				stmts.add(getAlternateLabel(lStmt, block.getMethodName()));
				
				if (lStmt.getLabelString().matches(ExplicitGlobalLoader.ArrayPassLabelRegex)) {
					break;
				}
				
				continue;
			}
			else if (stmt.getClass().equals(JumpStmt.class)) {
				JumpStmt jStmt = (JumpStmt) stmt;
				JumpStmt newJStmt = new JumpStmt(jStmt.getCondition(), getAlternateLabel(jStmt.getLabel(), block.getMethodName()));
				stmts.add(newJStmt);
				continue;
			}
			
			stmts.add(block.getStatements().get(i));
		}
		
		ExpressionFlattenerVisitor.MAXBOUNDCHECKS++;
		
		return stmts;
	}
	
	private LabelStmt getAlternateLabel(LabelStmt lStmt, String methodName) {
		if (lStmt.getLabelString().matches(ExplicitGlobalLoader.ArrayPassLabelRegex)) {
			return new LabelStmt(getArrayBoundPass(getArrayIDFromArrayLabelStmt(lStmt, "pass"), methodName));
		}
		else if (lStmt.getLabelString().matches(ExplicitGlobalLoader.ArrayBeginLabelRegex)) {
			return new LabelStmt(getArrayBoundBegin(getArrayIDFromArrayLabelStmt(lStmt, "begin"), methodName));
		}
		else if (lStmt.getLabelString().matches(ExplicitGlobalLoader.ArrayFailLabelRegex)) {
			return new LabelStmt(getArrayBoundFail(getArrayIDFromArrayLabelStmt(lStmt, "fail"), methodName));
		}
		
		return lStmt;
	}

	public void setGlobalDefAnalyzer(GlobalsDefDFAnalyzer df) {
		this.df = df;
	}

	public GlobalsDefDFAnalyzer getDf() {
		return df;
	}
	
	private String getArrayIDFromArrayLabelStmt(LabelStmt stmt, String end) {
		String name = stmt.getLabelString();
      int i = name.indexOf(".array.");
      name = name.substring(i + 7);
      
      name = name.substring(0, name.length() - end.length() - 1);
      i = name.indexOf(".");
      name = name.substring(0, i);
      
      return name;  
	}
	
	private String getArrayBoundBegin(String name, String methodName) {
		return methodName + ".array." + name + "." + ExpressionFlattenerVisitor.MAXBOUNDCHECKS + ".begin";
	}
	
	private String getArrayBoundFail(String name, String methodName) {
		return methodName + ".array." + name + "." + ExpressionFlattenerVisitor.MAXBOUNDCHECKS + ".fail";
	}
	
	private String getArrayBoundPass(String name, String methodName) {
		return methodName + ".array." + name + "." + ExpressionFlattenerVisitor.MAXBOUNDCHECKS + ".pass";
	}
}
