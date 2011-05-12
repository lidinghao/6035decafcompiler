package decaf.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.dataflow.cfg.MethodIR;

// Given a loopId, this handles the job of creating a method out of the loop which takes
// in a thread ID, manipulating the loop boundaries based on the thread ID and updating
// the original location of the loop to call the pthread library
// Also, this creates two globals for each loopId which maintains the loop iteration boundaries
// before the loop method is called
public class LoopParallelizer {
	HashMap<String, MethodIR> mMap;
	List<String> parallelizableLoops;
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	private static int NumThreads = 4;
	private static int BaseBlockId = 1000;
	private static int BaseMethodId = 1000;
	// Minimum number of iterations to allow parallelization (if we know the number of iterations)
	private static int MinimumIters = 1000;
	
	public LoopParallelizer(HashMap<String, MethodIR> mMap, List<String> parallelLoops) {
		this.mMap = mMap;
		this.parallelizableLoops = parallelLoops;
		for (String loopId : parallelLoops) {
			parallelizeLoop(loopId);
			BaseBlockId++;
		}
		
		// Set the num pthreads in main
		List<LIRStatement> pthreadCall = new ArrayList<LIRStatement>();
		pthreadCall.add(new LabelStmt("main.mcall.set_num_threads."+BaseMethodId+".begin"));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RDI), new ConstantName(NumThreads), null));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RAX), new ConstantName(0), null));
		pthreadCall.add(new CallStmt("set_num_threads"));
		pthreadCall.add(new LabelStmt("main.mcall.set_num_threads."+BaseMethodId+".end"));
		BaseMethodId++;
		List<LIRStatement> mainMethodStmts = mMap.get("main").getStatements();
		mainMethodStmts.addAll(0, pthreadCall);
	}
	
	public void parallelizeLoop(String loopId) {
		// Get all statements from for init to for end
		int forInitLabelIndex = getForLabelStmtIndexInMethod(loopId, ForInitLabelRegex);
		int forEndLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		List<LIRStatement> loopMethodStmts = new ArrayList<LIRStatement>();
		
		for (int i = forInitLabelIndex; i < forEndLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			loopMethodStmts.add(stmt);
		}
		
		// Get block id and min boundary for loop
		QuadrupletStmt forInit = (QuadrupletStmt)methodStmts.get(forInitLabelIndex+1);
		Name loopMin = forInit.getArg1();
		VarName globalLoopMin = new VarName("$glmin" + loopId);
		VarName tempLoopMin = new VarName("$tlmin" + loopId);
		tempLoopMin.setBlockId(BaseBlockId);
		
		// Get the loop max boundary, look at cmp statement after the for test label
		int forTestLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		CmpStmt forCmp = (CmpStmt)methodStmts.get(forTestLabelIndex+1);
		Name loopMax = forCmp.getArg2();
		VarName globalLoopMax = new VarName("$glmax" + loopId);
		VarName tempLoopMax = new VarName("$tlmax" + loopId);
		tempLoopMax.setBlockId(BaseBlockId);
		
		// Create thread id parameter
		VarName threadId = new VarName("$tid" + loopId);
		threadId.setBlockId(-2);
		
		// Add the conditional logic to determine loop boundaries based on thread id
		List<LIRStatement> conditionalBoundaryStmts = new ArrayList<LIRStatement>();
		VarName boundDiff = new VarName("$tldiff" + loopId);
		boundDiff.setBlockId(BaseBlockId);
		QuadrupletStmt boundDiffCalc;
		if (loopMax.getClass().equals(ConstantName.class) && loopMin.getClass().equals(ConstantName.class)) {
			// Statically calculate difference
			int diff = Integer.parseInt(((ConstantName)loopMax).getValue()) - 
			Integer.parseInt(((ConstantName)loopMin).getValue());
			if (diff < MinimumIters) {
				// Don't bother parallelizing
				return;
			}
			boundDiffCalc = new QuadrupletStmt(QuadrupletOp.MOVE, boundDiff, new ConstantName(diff), null);
			conditionalBoundaryStmts.add(boundDiffCalc);
		} else {
			boundDiffCalc = new QuadrupletStmt(QuadrupletOp.SUB, boundDiff, globalLoopMax, globalLoopMin);
			conditionalBoundaryStmts.add(boundDiffCalc);
		}
		
		// Calculate chunk size and temp loop min
		VarName chunkSize = new VarName("$tchunk" + loopId);
		chunkSize.setBlockId(BaseBlockId);
		QuadrupletStmt chunkSizeCalc = new QuadrupletStmt(QuadrupletOp.DIV, chunkSize, boundDiff, new ConstantName(NumThreads));
		QuadrupletStmt tempLoopMinCalc = new QuadrupletStmt(QuadrupletOp.MUL, tempLoopMin, chunkSize, threadId);
		conditionalBoundaryStmts.add(chunkSizeCalc);
		conditionalBoundaryStmts.add(tempLoopMinCalc);
		
		// Calculate temp loop max via a conditional
		LabelStmt notLastThread = new LabelStmt(loopId+".notLastThread");
		LabelStmt tempAssignEnd = new LabelStmt(loopId+".doneTempAssign");
		conditionalBoundaryStmts.add(new CmpStmt(threadId, new ConstantName(NumThreads-1)));
		conditionalBoundaryStmts.add(new JumpStmt(JumpCondOp.NEQ, notLastThread));
		conditionalBoundaryStmts.add(new QuadrupletStmt(QuadrupletOp.MOVE, tempLoopMax, globalLoopMax, null));
		conditionalBoundaryStmts.add(tempAssignEnd);
		conditionalBoundaryStmts.add(notLastThread);
		VarName maxIndex = new VarName("$timax" + loopId);
		QuadrupletStmt threadIdPlusOne = new QuadrupletStmt(QuadrupletOp.ADD, maxIndex, threadId, new ConstantName(1));
		QuadrupletStmt tempLoopMaxCalc = new QuadrupletStmt(QuadrupletOp.MUL, tempLoopMax, chunkSize, maxIndex);
		conditionalBoundaryStmts.add(threadIdPlusOne);
		conditionalBoundaryStmts.add(tempLoopMaxCalc);
		conditionalBoundaryStmts.add(tempAssignEnd);
		
		// Copy propagate temps to original variables
		forInit.setArg1(tempLoopMin);
		forCmp.setArg2(tempLoopMax);
		loopMethodStmts.addAll(0,conditionalBoundaryStmts);
	
		// Add method header statements
		loopMethodStmts.add(0, new QuadrupletStmt(QuadrupletOp.MOVE, threadId, new RegisterName(Register.RDI), null));
		loopMethodStmts.add(0, new EnterStmt());
		loopMethodStmts.add(0, new LabelStmt(loopId));
		
		// Add the call to the pthread method in the original method statements
		List<LIRStatement> pthreadCall = new ArrayList<LIRStatement>();
		// Create two global vars to store the start and end for this loop
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, globalLoopMin, loopMin, null));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, globalLoopMax, loopMax, null));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RDI), new ConstantName(loopId), null));
		pthreadCall.add(new LabelStmt(loopInfo[0]+".mcall."+"create_and_run_thread."+BaseMethodId+".begin"));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RDI), new ConstantName(loopId), null));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RAX), new ConstantName(0), null));
		pthreadCall.add(new CallStmt("create_and_run_threads"));
		pthreadCall.add(new LabelStmt(loopInfo[0]+".mcall."+"create_and_run_thread."+BaseMethodId+".end"));
		BaseMethodId++;
		methodStmts.addAll(forInitLabelIndex, pthreadCall);
		
		// Remove the loop statements from the method
		methodStmts.removeAll(loopMethodStmts);
	}
	
	private int getForLabelStmtIndexInMethod(String loopId, String label) {
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		for (int i = 0; i < methodStmts.size(); i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt)stmt;
				String labelStr = lStmt.getLabelString();
				if (labelStr.matches(label)) {
					// Label points to this for loop's id
					if (getIdFromForLabel(labelStr).equals(loopId)) {
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	private String getIdFromForLabel(String label) {
		String[] forInfo = label.split("\\.");
		return forInfo[0] + "." + forInfo[1];
	}
}
