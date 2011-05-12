package decaf.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.DataStmt;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LeaveStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.MethodIR;

// Given a loopId, this handles the job of creating a method out of the loop which takes
// in a thread ID, manipulating the loop boundaries based on the thread ID and updating
// the original location of the loop to call the pthread library
// Also, this creates two globals for each loopId which maintains the loop iteration boundaries
// before the loop method is called
public class LoopParallelizer {
	private HashMap<String, MethodIR> mMap;
	private ProgramFlattener pf;
	private List<String> parallelLoops;
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String ForTestLabelRegex = "[a-zA-z_]\\w*.for\\d+.test";
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	private static int NumThreads = 4;
	private static int BaseBlockId = 1000;
	private static int BaseMethodId = 1000;
	// Minimum number of iterations to allow parallelization (if we know the number of iterations)
	private static int BaseGlobalId = 1;
	
	public LoopParallelizer(HashMap<String, MethodIR> mMap, List<String> parallelLoops, ProgramFlattener pf) {
		this.mMap = mMap;
		this.pf = pf;
		this.parallelLoops = parallelLoops;
	}
	
	public void parallelize() {
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
		mainMethodStmts.addAll(2, pthreadCall);
	}
	
	public void parallelizeLoop(String loopId) {
		// Get all statements from for init to for end
		int forInitLabelIndex = getForLabelStmtIndexInMethod(loopId, ForInitLabelRegex);
		int forEndLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		int forTestLabelIndex = getForLabelStmtIndexInMethod(loopId, ForTestLabelRegex);
		int forBodyLabelIndex = getForLabelStmtIndexInMethod(loopId, ForBodyLabelRegex);
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		List<LIRStatement> loopMethodStmts = new ArrayList<LIRStatement>();
		List<DataStmt> dataStmts = new ArrayList<DataStmt>();
		
		VarName globalLoopMax = new VarName(".glmax" + loopId);
		dataStmts.add(new DataStmt(".glmax" + loopId));
		VarName globalLoopMin = new VarName(".glmin" + loopId);
		dataStmts.add(new DataStmt(".glmin" + loopId));
		Name loopMax = null, loopMin = null;
		HashMap<Name, VarName> localToGlobal = new HashMap<Name, VarName>();
		boolean minBoundFound = false;
		String forLabel;
		loopMethodStmts.add(methodStmts.get(forInitLabelIndex));
		for (int i = forInitLabelIndex+1; i < forTestLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				Name dest = ((QuadrupletStmt)stmt).getDestination();
				if (dest.getClass().equals(VarName.class)) {
					// It is min bound
					loopMin = dest;
					loopMethodStmts.add(stmt);
					minBoundFound = true;
				}
			}
			if (minBoundFound) {
				loopMethodStmts.add(stmt);
			}
		}
		loopMethodStmts.add(methodStmts.get(forTestLabelIndex));
		for (int i = forTestLabelIndex+1; i < forBodyLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (stmt.getClass().equals(CmpStmt.class)) {
				Name dest = ((CmpStmt)stmt).getArg2();
				loopMax = dest;
			}
		}
		// Get the method variables used in the init and test blocks of the for loop (and nested for loops)
		// Store them in globals and perform the move statements in the method before calling the loop
		boolean checkLocals = false;
		NamesDefinedTest ndt = new NamesDefinedTest(mMap);
		for (int i = forBodyLabelIndex; i <= forEndLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			if (checkLocals) {
				List<Name> invalidNames = ndt.validStmt(stmt, loopId);
				if (invalidNames.size() != 0) {
					for (Name in : invalidNames) {
						localToGlobal.put(in, new VarName(".glpar"+BaseGlobalId));
						dataStmts.add(new DataStmt(".glpar"+BaseGlobalId));
						BaseGlobalId++;
					}
				}
			} 
			loopMethodStmts.add(stmt);
			
			if (stmt.getClass().equals(LabelStmt.class)) {
				forLabel = ((LabelStmt)stmt).getLabelString();
				if (forLabel.matches(ForInitLabelRegex)) {
					checkLocals = true;
				} else if (forLabel.matches(ForBodyLabelRegex)) {
					checkLocals = false;
				}
			}
		}
		
		// Temporary bounds in loop method
		VarName tempLoopMin = new VarName(".tlmin" + loopId);
		tempLoopMin.setBlockId(BaseBlockId);
		VarName tempLoopMax = new VarName(".tlmax" + loopId);
		tempLoopMax.setBlockId(BaseBlockId);
		
		// Thread id parameter
		VarName threadId = new VarName(".tid" + loopId);
		threadId.setBlockId(-2);
		
		// Add the conditional logic to determine loop boundaries based on thread id
		List<LIRStatement> conditionalBoundaryStmts = new ArrayList<LIRStatement>();
		VarName boundDiff = new VarName(".tldiff" + loopId);
		boundDiff.setBlockId(BaseBlockId);
		QuadrupletStmt boundDiffCalc = new QuadrupletStmt(QuadrupletOp.SUB, boundDiff, globalLoopMax, globalLoopMin);
		conditionalBoundaryStmts.add(boundDiffCalc);
		
		// Add the call to the pthread method in the original method statements
		List<LIRStatement> pthreadCall = new ArrayList<LIRStatement>();
		// Create two global vars to store the start and end for this loop
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, globalLoopMin, loopMin, null));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, globalLoopMax, loopMax, null));
		for (Name local : localToGlobal.keySet()) {
			pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, localToGlobal.get(local), local, null));
		}
		pthreadCall.add(new LabelStmt(loopInfo[0]+".mcall."+"create_and_run_thread."+BaseMethodId+".begin"));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RDI), new ConstantName(loopId), null));
		pthreadCall.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RAX), new ConstantName(0), null));
		pthreadCall.add(new CallStmt("create_and_run_threads"));
		pthreadCall.add(new LabelStmt(loopInfo[0]+".mcall."+"create_and_run_thread."+BaseMethodId+".end"));
		BaseMethodId++;
		methodStmts.addAll(forBodyLabelIndex, pthreadCall);
		
		System.out.println("ABCDEF Method has end label: " + (getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex) != -1));
		forBodyLabelIndex = getForLabelStmtIndexInMethod(loopId, ForBodyLabelRegex);
		forEndLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		List<LIRStatement> temp = new ArrayList<LIRStatement>();
		// Remove the loop statements starting from the body label from the method
		for (int i = forBodyLabelIndex; i < forEndLabelIndex; i++) {
			temp.add(methodStmts.get(i));
		}
		methodStmts.removeAll(temp);
		
		// Replace LabelStmt in loop which began with the original method to instead begin with loopId
		for (LIRStatement stmt : loopMethodStmts) {
			if (stmt.getClass().equals(LabelStmt.class)) {
				String labelStr = ((LabelStmt)stmt).getLabelString();
				((LabelStmt)stmt).setLabel(labelStr.replaceFirst(loopInfo[0], loopId));
			}
			if (stmt.getClass().equals(JumpStmt.class)) {
				LabelStmt jumpLabel = ((JumpStmt)stmt).getLabel();
				String labelStr = jumpLabel.getLabelString();
				if (!labelStr.equals(ProgramFlattener.exceptionHandlerLabel)) {
					((LabelStmt)jumpLabel).setLabel(labelStr.replaceFirst(loopInfo[0], loopId));
				}
			}
		}
		
		// Replace references to old local method variables with their global counterparts
		for (LIRStatement loopStmt : loopMethodStmts) {
			Name dest = null, arg1 = null, arg2 = null;
			if (loopStmt.getClass().equals(QuadrupletStmt.class)) {
				dest = ((QuadrupletStmt)loopStmt).getDestination();
				arg1 = ((QuadrupletStmt)loopStmt).getArg1();
				arg2 = ((QuadrupletStmt)loopStmt).getArg2();
				if (dest != null) {
					if (localToGlobal.containsKey(dest)) {
						((QuadrupletStmt)loopStmt).setDestination(localToGlobal.get(dest));
					}
				}
				if (arg1 != null) {
					if (localToGlobal.containsKey(arg1)) {
						((QuadrupletStmt)loopStmt).setArg1(localToGlobal.get(arg1));
					}
				}
				if (arg2 != null) {
					if (localToGlobal.containsKey(arg2)) {
						((QuadrupletStmt)loopStmt).setArg2(localToGlobal.get(arg2));
					}
				}
			} else if (loopStmt.getClass().equals(CmpStmt.class)) {
				arg1 = ((CmpStmt)loopStmt).getArg1();
				arg2 = ((CmpStmt)loopStmt).getArg2();
				if (arg1 != null) {
					if (localToGlobal.containsKey(arg1)) {
						((CmpStmt)loopStmt).setArg1(localToGlobal.get(arg1));
					}
				}
				if (arg2 != null) {
					if (localToGlobal.containsKey(arg2)) {
						((CmpStmt)loopStmt).setArg2(localToGlobal.get(arg2));
					}
				}
			} else if (loopStmt.getClass().equals(PopStmt.class)) {
				arg1 = ((PopStmt)loopStmt).getName();
				if (arg1 != null) {
					if (localToGlobal.containsKey(arg1)) {
						((PopStmt)loopStmt).setName(localToGlobal.get(arg1));
					}
				}
			} else if (loopStmt.getClass().equals(PushStmt.class)) {
				arg1 = ((PushStmt)loopStmt).getName();
				if (arg1 != null) {
					if (localToGlobal.containsKey(arg1)) {
						((PushStmt)loopStmt).setName(localToGlobal.get(arg1));
					}
				}
			}
		}
		
		// Calculate chunk size and temp loop min
		VarName chunkSize = new VarName(".tchunk" + loopId);
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
		VarName maxIndex = new VarName(".timax" + loopId);
		QuadrupletStmt threadIdPlusOne = new QuadrupletStmt(QuadrupletOp.ADD, maxIndex, threadId, new ConstantName(1));
		QuadrupletStmt tempLoopMaxCalc = new QuadrupletStmt(QuadrupletOp.MUL, tempLoopMax, chunkSize, maxIndex);
		conditionalBoundaryStmts.add(threadIdPlusOne);
		conditionalBoundaryStmts.add(tempLoopMaxCalc);
		conditionalBoundaryStmts.add(tempAssignEnd);
		loopMethodStmts.addAll(0,conditionalBoundaryStmts);

		// Add method header and footer statements
		loopMethodStmts.add(0, new QuadrupletStmt(QuadrupletOp.MOVE, threadId, new RegisterName(Register.RDI), null));
		loopMethodStmts.add(0, new EnterStmt());
		loopMethodStmts.add(0, new LabelStmt(loopId));
		loopMethodStmts.add(new LeaveStmt());
		
		pf.getLirMap().put(loopId, loopMethodStmts);
		pf.getDataStmtList().addAll(dataStmts);
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
