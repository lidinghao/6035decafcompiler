package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.LoopInvariantGenerator.LoopQuadrupletStmt;

// TODO: (Usman) for hoisting statements with arrays, move all the bound check stuff as well
// TODO: for hoisting statements with variables which are local to the loop, move out the variable initialization as well (but then this var might conflict...)

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
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	
	public LoopInvariantOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		System.out.println("START LOOP INVARIANT GENERATOR");
		this.loopInvariantGenerator = new LoopInvariantGenerator(mMap);
		System.out.println("END LOOP INVARIANT GENERATOR");
		this.reachingDefGenerator = new BlockReachingDefinitionGenerator(mMap, ConfluenceOperator.AND);
		this.livenessGenerator = new BlockLivenessGenerator(mMap);
		livenessGenerator.generate();
		loopInvariantGenerator.generateLoopInvariants();
		System.out.println("START AND CONFLUENCE REACHING DEF GENERATOR");
		reachingDefGenerator.generate();
		System.out.println("END AND CONFLUENCE REACHING DEF GENERATOR");
		generateLoopIdToCFGBlockMaps();
	}
	
	public void performLoopInvariantOptimization() {
		HashSet<QuadrupletStmt> hoistedQStmts = new HashSet<QuadrupletStmt>();
		List<LoopQuadrupletStmt> hoistedLQStmts = new ArrayList<LoopQuadrupletStmt>();
		for (LoopQuadrupletStmt lqs : loopInvariantGenerator.getLoopInvariantStmts()) {
			if (canBeHoisted(lqs)) {
				hoistedQStmts.add(lqs.getqStmt());
				hoistedLQStmts.add(lqs);
			}
		}
		
		System.out.println("LOOP INVARIANT STMTS WHICH CAN BE HOISTED: " + hoistedLQStmts);
		
		// Remove all hoisted QuadrupletStmts from CFGs
		for (String s : mMap.keySet()) {
			for (CFGBlock block : mMap.get(s).getCfgBlocks()) {
				List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
				for (LIRStatement stmt : block.getStatements()) {
					if (stmt.getClass().equals(QuadrupletStmt.class)) {
						if (hoistedQStmts.contains(stmt)) {
							// Don't add it back
							continue;
						}
					}
					newStmts.add(stmt);
				}
				block.setStatements(newStmts);
			}
		}
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
	//	following three conditions:
	// 1. The statement dominates all loop exits 
	//		(the stmt reaches - with AND confluence op - the beginning of the 
	//		block containing the for loop end)
	// 2. There is only one definition of dest in the loop
	// 3. The dest is not live out of the loop preheader 
	//		(the end of the block containing the for loop init)
	private boolean canBeHoisted(LoopQuadrupletStmt lqs) {
		String loopId = lqs.getLoopBodyBlockId();
		QuadrupletStmt qStmt = lqs.getqStmt(); 
		int stmtId = qStmt.getMyId();
		Name dest = qStmt.getDestination();
		CFGBlock forInitBlock = loopIdToLoopInitCFGBlock.get(loopId);
		CFGBlock forEndBlock = loopIdToLoopEndCFGBlock.get(loopId);
		BlockDataFlowState initBlockLivenessState = livenessGenerator.getBlockLiveVars().get(forInitBlock);
		BlockDataFlowState endBlockReachDefState = reachingDefGenerator.getBlockReachingDefs().get(forEndBlock);
		if (notLiveOutOfLoopPreheader(dest, initBlockLivenessState)) {
			// Satisfies condition 3
			if (endBlockReachDefState.getIn().get(stmtId)) {
				// Satisfies condition 1
				if (oneDefinitionOfDestInLoop(dest, loopId)) {
					// Satisfied condition 2
					return true;
				}
			}
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
		
		String forLabel, forId;
		for (String s : mMap.keySet()) {
			for (CFGBlock block : mMap.get(s).getCfgBlocks()) {
				for (LIRStatement stmt : block.getStatements()) {
					if (stmt.getClass().equals(LabelStmt.class)) {
						forLabel = ((LabelStmt)stmt).getLabelString();
						if (forLabel.matches(ForInitLabelRegex)) {
							forId = getIdFromForLabel(forLabel);
							loopIdToLoopInitCFGBlock.put(forId, block);
						} else if (forLabel.matches(ForEndLabelRegex)) {
							forId = getIdFromForLabel(forLabel);
							loopIdToLoopEndCFGBlock.put(forId, block);
						}
					}
				}
			}
		}
	}
	
	private String getIdFromForLabel(String label) {
		String[] forInfo = label.split("\\.");
		return forInfo[0] + forInfo[1];
	}
	
	// Hoist the given LoopQuadrupletStmt outside of the loop it is in
	private void hoist(LoopQuadrupletStmt lqs) {
		String loopId = lqs.getLoopBodyBlockId();
		CFGBlock loopInitBlock = loopIdToLoopInitCFGBlock.get(loopId);
		loopInitBlock.getStatements().add(lqs.getqStmt());
	}
	
	// Returns true if there is only one definition of dest in the given loopId, false otherwise
	private boolean oneDefinitionOfDestInLoop(Name dest, String loopId) {
		boolean oneDef = false;
		for (LoopQuadrupletStmt lqs : loopInvariantGenerator.getAllLoopBodyQStmts().get(loopId)) {
			QuadrupletStmt qStmt = lqs.getqStmt();
			if (qStmt.getDestination().equals(dest)) {
				if (!oneDef) {
					oneDef = true;
				} else {
					return false;
				}
			}
		}
		return oneDef;
	}
}
