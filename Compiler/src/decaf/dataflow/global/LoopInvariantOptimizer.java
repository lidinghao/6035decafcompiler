package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ExpressionFlattenerVisitor;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class LoopInvariantOptimizer {
	private HashMap<String, MethodIR> mMap;
	private LoopInvariantGenerator loopInvariantGenerator;
	private BlockReachingDefinitionGenerator reachingDefGenerator;
	private BlockLivenessGenerator livenessGenerator;
	// Loop body id => CFGBlock map where the CFGBlock is the block containing the
	// init label for the loop with the given id
	private HashMap<String, CFGBlock> loopIdToLoopInitCFGBlock;
	// Loop body id => CFGBlock map where the CFGBlock is the block containing the
	// end label for the loop with the given id
	private HashMap<String, CFGBlock> loopIdToLoopEndCFGBlock;
	// Loop body id => CFGBlock map where the CFGBlock is the block containing the
	// body label for the loop with the given id
	private HashMap<String, CFGBlock> loopIdToLoopBodyCFGBlock;
	// Loop body id => CFGBlock map where the CFGBlock is the block containing the
	// test label for the loop with the given id
	private HashMap<String, CFGBlock> loopIdToLoopTestCFGBlock;
	private HashMap<String, String> loopIdToMethod;
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	private static String ForTestLabelRegex = "[a-zA-z_]\\w*.for\\d+.test";
	private static String ArrayPassLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.pass";
   private static String ArrayBeginLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.begin";
   private static String ArrayFailLabelRegex = "[a-zA-z_]\\w*.array.[a-zA-z_]\\w*.\\d+.fail";
	
	public LoopInvariantOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.loopInvariantGenerator = new LoopInvariantGenerator(mMap);
		this.reachingDefGenerator = new BlockReachingDefinitionGenerator(mMap, ConfluenceOperator.AND);
		this.livenessGenerator = new BlockLivenessGenerator(mMap);
	}
	
	public void performLoopInvariantOptimization() {
		livenessGenerator.generate();
		loopInvariantGenerator.generateLoopInvariants();
		generateLoopIdToCFGBlockMaps();
		HashSet<QuadrupletStmt> hoistedQStmts = new HashSet<QuadrupletStmt>();
		List<LoopQuadrupletStmt> hoistedLQStmts = new ArrayList<LoopQuadrupletStmt>();
		for (LoopQuadrupletStmt lqs : loopInvariantGenerator.getLoopInvariantStmts()) {
			if (canBeHoisted(lqs)) {
				hoistedLQStmts.add(lqs);
				hoistedQStmts.add(lqs.getqStmt());
			}
		}
		
		System.out.println("LOOP INVARIANT STMTS WHICH CAN BE HOISTED: " + hoistedLQStmts);
		
		// Add all hoisted LoopQuadrupletStmts at the end of the loop init block
		for (LoopQuadrupletStmt lqs : hoistedLQStmts) {
			hoist(lqs);
		}
		// Regenerate statements
		for (String s : mMap.keySet()) {
			mMap.get(s).regenerateStmts();
		}
	}
	
	// A loop invariant statement can be hoisted if it satisifes the 
	//	following two conditions:
	// 1. The statement dominates all loop exits 
	//		(the stmt reaches - with AND confluence op - the beginning of the 
	//		block containing the for loop end)
	// 2. There is only one definition of dest in the loop
	// 3. Destination is either not live out of loop preheader or if it is, it is not
	//		used before the loop invariant statement
	private boolean canBeHoisted(LoopQuadrupletStmt lqs) {
		System.out.println("TRYING TO HOIST " + lqs);
		String loopId = lqs.getLoopBodyBlockId();
		int stmtIndex = lqs.getStmtIndex();
		QuadrupletStmt qStmt = lqs.getqStmt();
		Name dest = qStmt.getDestination();
		CFGBlock forEndBlock = loopIdToLoopEndCFGBlock.get(loopId);
		CFGBlock forTestBlock = loopIdToLoopTestCFGBlock.get(loopId);
		CFGBlock forInitBlock = loopIdToLoopInitCFGBlock.get(loopId);
		reachingDefGenerator.generateForForLoop(forTestBlock, forEndBlock, forInitBlock);
		BlockDataFlowState endBlockReachDefState = reachingDefGenerator.getBlockReachingDefs().get(forEndBlock);
		int stmtId = qStmt.getMyId();
		if (endBlockReachDefState.getIn().get(stmtId)) {
			// Satisfies condition 1
			if (oneDefinitionOfDestInLoop(dest, loopId)) {
				// Satisfied condition 2
				// If The dest is not live out of the loop preheader (the end of the block containing 
				// the for loop init), the simply add the statement
				BlockDataFlowState initBlockLivenessState = livenessGenerator.getBlockLiveVars().get(forInitBlock);
				if (notLiveOutOfLoopPreheader(qStmt.getDestination(), initBlockLivenessState)) {
					// Satisfied condition 3
					return true;
				} else {
					// Need to check whether the destination is used in the loop body before the loop invariant statement
					boolean destUsed = false;
					LIRStatement stmt;
					List<LIRStatement> methodStmts = mMap.get(loopIdToMethod.get(loopId)).getStatements();
					int bodyLabelIndex = loopInvariantGenerator.getLoopIdToBodyStmtIndex().get(loopId);
					for (int i = bodyLabelIndex; i < stmtIndex; i++) {
						stmt = methodStmts.get(i);
						if (stmtUsesName(stmt, qStmt.getDestination())) {
							destUsed = true;
							break;
						}
					}
					if (!destUsed) {
						// Satisified condition 3
						lqs.setNeedConditionalCheck(true);
						return true;
					}
					System.out.println(lqs.getqStmt() + " cannot be hoisted because dest is used before invariant stmt");
				}
			} else {
				System.out.println(lqs.getqStmt() + " cannot be hoisted because it doesn't satisfy condition 2");
			}
		} else {
			System.out.println(lqs.getqStmt() + " cannot be hoisted because it doesn't satisfy condition 1");
		}
		return false;
	}
	
	private boolean notLiveOutOfLoopPreheader(Name dest, BlockDataFlowState initBlockLivenessState) {
		Variable destLivenessVar = livenessGenerator.getNameToVar().get(dest);
		if (!initBlockLivenessState.getOut().get(destLivenessVar.getMyId())) {
			// If dest is ArrayName, make sure the index is live
			if (dest.getClass().equals(ArrayName.class)) {
				return notLiveOutOfLoopPreheader(((ArrayName)dest).getIndex(), initBlockLivenessState);
			}
			return true;
		}
		return false;
	}
	
	// Generates the loopIdToLoopInitCFGBlock and loopIdToLoopEndCFGBlock maps
	private void generateLoopIdToCFGBlockMaps() {
		loopIdToLoopInitCFGBlock = new HashMap<String, CFGBlock>();
		loopIdToLoopEndCFGBlock = new HashMap<String, CFGBlock>();
		loopIdToLoopBodyCFGBlock = new HashMap<String, CFGBlock>();
		loopIdToLoopTestCFGBlock = new HashMap<String, CFGBlock>();
		loopIdToMethod = new HashMap<String, String>();
		String forLabel, forId;
		for (String s : mMap.keySet()) {
			for (CFGBlock block : mMap.get(s).getCfgBlocks()) {
				for (LIRStatement stmt : block.getStatements()) {
					if (stmt.getClass().equals(LabelStmt.class)) {
						forLabel = ((LabelStmt)stmt).getLabelString();
						boolean isForLabel = false;
						if (forLabel.matches(ForInitLabelRegex)) {
							forId = getIdFromForLabel(forLabel);
							loopIdToLoopInitCFGBlock.put(forId, block);
							isForLabel = true;
						} else if (forLabel.matches(ForEndLabelRegex)) {
							forId = getIdFromForLabel(forLabel);
							loopIdToLoopEndCFGBlock.put(forId, block);
							isForLabel = true;
						} else if (forLabel.matches(ForBodyLabelRegex)) {
							forId = getIdFromForLabel(forLabel);
							loopIdToLoopBodyCFGBlock.put(forId, block);
							isForLabel = true;
						} else if (forLabel.matches(ForTestLabelRegex)) {
							forId = getIdFromForLabel(forLabel);
							loopIdToLoopTestCFGBlock.put(forId, block);
							isForLabel = true;
						}
						if (isForLabel) {
							forId = getIdFromForLabel(forLabel);
							loopIdToMethod.put(forId, s);
						}
					}
				}
			}
		}
	}
	
	private String getIdFromForLabel(String label) {
		String[] forInfo = label.split("\\.");
		return forInfo[0] + "." + forInfo[1];
	}
	
	// Hoist the given LoopQuadrupletStmt outside of the loop it is in
	// If the QuadrupletStmt contains array references, move the associated bound checks as well
	private void hoist(LoopQuadrupletStmt lqs) {
		String loopId = lqs.getLoopBodyBlockId();
		CFGBlock loopInitBlock = loopIdToLoopInitCFGBlock.get(loopId);
		List<LIRStatement> loopInitStmtList = loopInitBlock.getStatements();
		if (lqs.getNeedConditionalCheck()) {
			CFGBlock loopTestBlock = loopIdToLoopTestCFGBlock.get(loopId);
			List<LIRStatement> testStmts = new ArrayList<LIRStatement>(loopTestBlock.getStatements());
			// Remove label
			testStmts.remove(0);
			loopInitStmtList.addAll(testStmts);
		}
		hoistArrayBoundsChecks(lqs.getqStmt(), loopId);
		for (String s : mMap.keySet()) {
			for (CFGBlock block : mMap.get(s).getCfgBlocks()) {
				List<LIRStatement> blockStmts =  block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt == lqs.getqStmt()) {
						// Remove statement from existing location
						blockStmts.remove(i);
						// Add statement to init block
						lqs.getqStmt().setDepth(loopInitStmtList.get(0).getDepth());
						// Add statement right before the init label
						loopInitStmtList.add(lqs.getqStmt());
					}
				}
			}
		}
		if (lqs.getNeedConditionalCheck()) {
			// Add jmp statement to body
			JumpStmt jmp = new JumpStmt(JumpCondOp.NONE, new LabelStmt(loopId + "." + "body"));
			loopInitStmtList.add(jmp);
		}
	}
	
//	private int findInitLabelIndex(String loopId) {
//		CFGBlock loopInitBlock = loopIdToLoopInitCFGBlock.get(loopId);
//		List<LIRStatement> loopInitStmtList = loopInitBlock.getStatements();
//		// Find the init label index
//		for (int j = 0; j < loopInitStmtList.size(); j++) {
//			LIRStatement initStmt = loopInitStmtList.get(j);
//			if (initStmt.getClass().equals(LabelStmt.class)) {
//				String labelStr = ((LabelStmt)initStmt).getLabelString();
//				if (labelStr.equals(loopId+".init")) {
//					return j;
//				}
//			}
//		}
//		return -1;
//	}
	
	private void hoistArrayBoundsChecks(QuadrupletStmt qStmt, String loopId) {
		// Find the CFGBlock and the statement index within the CFGBlock for the qStmt
		CFGBlock loopInitBlock = loopIdToLoopInitCFGBlock.get(loopId);
		CFGBlock blockWithQStmt = null;
		int indexOfQStmtInBlock = -1;
		pf:
		for (String s : mMap.keySet()) {
			for (CFGBlock block : mMap.get(s).getCfgBlocks()) {
				List<LIRStatement> blockStmts =  block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt == qStmt) {
						blockWithQStmt = block;
						indexOfQStmtInBlock = i;
						break pf;
					}
				}
			}
		}
		List<LIRStatement> loopInitStmtList = loopInitBlock.getStatements();
		List<LIRStatement> boundCheck = getBoundCheck(qStmt.getDestination(), 
				blockWithQStmt, indexOfQStmtInBlock);
		
		// NOTE: The array bound checks are not removed from the loop body since the array bounds DC
		// optimizer will take care of that
		if (boundCheck != null) {
			for (LIRStatement bcStmt : boundCheck) {
				bcStmt.setDepth(loopInitStmtList.get(0).getDepth());
			}
			loopInitStmtList.addAll(boundCheck);
		}
		boundCheck = getBoundCheck(qStmt.getArg1(), blockWithQStmt, indexOfQStmtInBlock);
		if (boundCheck != null) {
			for (LIRStatement bcStmt : boundCheck) {
				bcStmt.setDepth(loopInitStmtList.get(0).getDepth());
			}
			loopInitStmtList.addAll(boundCheck);
		}
		boundCheck = getBoundCheck(qStmt.getArg2(), blockWithQStmt, indexOfQStmtInBlock);
		if (boundCheck != null) {
			for (LIRStatement bcStmt : boundCheck) {
				bcStmt.setDepth(loopInitStmtList.get(0).getDepth());
			}
			loopInitStmtList.addAll(boundCheck);
		}
	}
	
	// Returns true if stmt uses or can potentially (with ArrayName) use name
	private boolean stmtUsesName(LIRStatement stmt, Name name) {
		// Check QuadrupletStmt, PushStmt, PopStmt, CmpStmt, LoadStmt
		QuadrupletStmt qStmt;
		PushStmt pushStmt;
		PopStmt popStmt;
		CmpStmt cmpStmt;
		LoadStmt loadStmt;
		if (stmt.getClass().equals(QuadrupletStmt.class)) {
			qStmt = (QuadrupletStmt)stmt;
			if (nameUsesName(qStmt.getDestination(), name) || nameUsesName(qStmt.getArg1(), name) 
					|| nameUsesName(qStmt.getArg2(), name)) {
				return true;
			}
		} else if (stmt.getClass().equals(PushStmt.class)) {
			pushStmt = (PushStmt)stmt;
			if (nameUsesName(pushStmt.getName(), name)) {
				return true;
			}
		} else if (stmt.getClass().equals(PopStmt.class)) {
			popStmt = (PopStmt)stmt;
			if (nameUsesName(popStmt.getName(), name)) {
				return true;
			}
		} else if (stmt.getClass().equals(CmpStmt.class)) {
			cmpStmt = (CmpStmt)stmt;
			if (nameUsesName(cmpStmt.getArg1(), name) || nameUsesName(cmpStmt.getArg2(), name)) {
				return true;
			}
		} else if (stmt.getClass().equals(LoadStmt.class)) {
			loadStmt = (LoadStmt)stmt;
			if (nameUsesName(loadStmt.getVariable(), name)) {
				return true;
			}
		}
		return false;
	}
	
	// Return true if name1 uses or can potentially (with ArrayName) use name2
	private boolean nameUsesName(Name name1, Name name2) {
		if (name1 == null || name2 == null) {
			return false;
		}
		// If name1 is not ArrayName and name2 is ArrayName, return false
		if (!(name1.getClass().equals(ArrayName.class)) && name2.getClass().equals(ArrayName.class)) {
			return false;
		}
		// Trivial case
		if (name1.equals(name2)) {
			return true;
		}
		// If both are ArrayName, if they both don't have unequal constant indices, return true
		if (name1.getClass().equals(ArrayName.class) && name2.getClass().equals(ArrayName.class)) {
			Name name1Index = ((ArrayName)name1).getIndex();
			Name name2Index = ((ArrayName)name2).getIndex();
			if (name1Index.getClass().equals(ConstantName.class) && name2Index.getClass().equals(ConstantName.class)) {
				if (name1Index.equals(name2Index)) {
					return true;
				}
			}
		}
		// Recursive case, checking nested indices
		if (name1.getClass().equals(ArrayName.class)) {
			return nameUsesName(((ArrayName)name1).getIndex(), name2);
		}
		return false;
	}
	
	// Returns true if there is only one definition of dest in the given loopId, false otherwise
	// Special case for ArrayName
	private boolean oneDefinitionOfDestInLoop(Name dest, String loopId) {
		boolean oneDef = false;
		for (LoopQuadrupletStmt lqs : loopInvariantGenerator.getAllLoopBodyQStmts().get(loopId)) {
			QuadrupletStmt qStmt = lqs.getqStmt();
			Name stmtDest = qStmt.getDestination();
			if (stmtDest.equals(dest)) {
				if (!oneDef) {
					oneDef = true;
				} else {
					return false;
				}
			} else if (dest.getClass().equals(ArrayName.class) && stmtDest.getClass().equals(ArrayName.class)) {
				ArrayName destArr = (ArrayName)dest;
				ArrayName stmtDestArr = (ArrayName)stmtDest;
				Name arrIndex = destArr.getIndex();
				if (stmtDestArr.getId().equals(destArr.getId())) {
					if (arrIndex.getClass().equals(ConstantName.class)) {
						if (!(stmtDestArr.getIndex().getClass().equals(ConstantName.class))) {
							return false;
						}
					} else {
						return false;
					}
				}
			}
		}
		return oneDef;
	}
	
	// Following methods are copied from NaiveLoadAdder.java
	// TODO: Put this logic into a utility class so it does not have to be copied
	
	private List<LIRStatement> getBoundCheck(Name name, CFGBlock block, int stmtIndex) {
		if (name == null) return null;
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
				if (lStmt.getLabelString().matches(LoopInvariantOptimizer.ArrayPassLabelRegex) &&
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
				if (lStmt.getLabelString().matches(LoopInvariantOptimizer.ArrayBeginLabelRegex)) {
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
				
				if (lStmt.getLabelString().matches(LoopInvariantOptimizer.ArrayPassLabelRegex)) {
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
		if (lStmt.getLabelString().matches(LoopInvariantOptimizer.ArrayPassLabelRegex)) {
			return new LabelStmt(getArrayBoundPass(getArrayIDFromArrayLabelStmt(lStmt, "pass"), methodName));
		}
		else if (lStmt.getLabelString().matches(LoopInvariantOptimizer.ArrayBeginLabelRegex)) {
			return new LabelStmt(getArrayBoundBegin(getArrayIDFromArrayLabelStmt(lStmt, "begin"), methodName));
		}
		else if (lStmt.getLabelString().matches(LoopInvariantOptimizer.ArrayFailLabelRegex)) {
			return new LabelStmt(getArrayBoundFail(getArrayIDFromArrayLabelStmt(lStmt, "fail"), methodName));
		}
		
		return lStmt;
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
	
	public void setmMap(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
	}
}
