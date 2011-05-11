package decaf.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.MethodIR;
import decaf.parallel.Analyze.AccessPattern;

// Finds loops in the program which pass the array resolver test
public class ArrayIndexResolverTest {
	HashMap<String, MethodIR> mMap;
	ArrayIndexResolver arrIndexResolver;
	private static String ForBodyLabelRegex = "[a-zA-z_]\\w*.for\\d+.body";
	private static String ForEndLabelRegex = "[a-zA-z_]\\w*.for\\d+.end";
	
	public ArrayIndexResolverTest(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.arrIndexResolver = new ArrayIndexResolver(mMap);
	}
	
	// Returns string of loop ids which pass the array resolver test
	public List<String> getLoopIDsWhichPass() {
		List<String> uniqueLoopIds = getAllLoopIds();
		List<String> parallelizableLoops = new ArrayList<String>();
		for (String loopId : uniqueLoopIds) {
			if (passesArrayResolverTest(loopId)) {
				parallelizableLoops.add(loopId);
			}
		}
		return parallelizableLoops;
	}
	
	// Returns true if the loopId passes array resolver test
	// False otherwise
	public boolean passesArrayResolverTest(String loopId) {
		int forBodyLabelIndex = getForLabelStmtIndexInMethod(loopId, ForBodyLabelRegex);
		int forEndLabelIndex = getForLabelStmtIndexInMethod(loopId, ForEndLabelRegex);
		List<Integer[]> arrayResolvesForDests = new ArrayList<Integer[]>();
		List<Integer[]> arrayResolvesForArgs = new ArrayList<Integer[]>();
		QuadrupletStmt qStmt;
		CmpStmt cStmt;
		PopStmt popStmt;
		PushStmt pushStmt;
		Integer[] res;
		
		String[] loopInfo = loopId.split("\\.");
		List<LIRStatement> methodStmts = mMap.get(loopInfo[0]).getStatements();
		
		for (int i = forBodyLabelIndex + 1; i < forEndLabelIndex; i++) {
			LIRStatement stmt = methodStmts.get(i);
			Name dest = null, arg1 = null, arg2 = null;
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				dest = qStmt.getDestination();
				arg1 = qStmt.getArg1();
				arg2 = qStmt.getArg2();
				
			} else if (stmt.getClass().equals(CmpStmt.class)) {
				cStmt = (CmpStmt)stmt;
				arg1 = cStmt.getArg1();
				arg2 = cStmt.getArg2();
				
			} else if (stmt.getClass().equals(PopStmt.class)) {
				popStmt = (PopStmt)stmt;
				arg1 = popStmt.getName();
				
			} else if (stmt.getClass().equals(PushStmt.class)) {
				pushStmt = (PushStmt)stmt;
				arg2 = pushStmt.getName();
				
			}
			
			if (dest != null) {
				if (dest.isArray()) {
					Name index = ((ArrayName)dest).getIndex();
					res = arrIndexResolver.resolveIndex(stmt, index, null);
					System.out.println("##############");
					System.out.println("Loop vars for stmt: " + stmt);
					System.out.println(arrIndexResolver.loopVariablesForStmt(stmt));
					System.out.println("Index resolve for stmt: " + stmt + ", for index " + index);
					System.out.println(Arrays.toString(res));
					if (res == null) {
						return false;
					}
					arrayResolvesForDests.add(res);
				}
			}
			if (arg1 != null) {
				if (arg1.isArray()) {
					Name index = ((ArrayName)arg1).getIndex();
					res = arrIndexResolver.resolveIndex(stmt, index, null);
					System.out.println("##############");
					System.out.println("Loop vars for stmt: " + stmt);
					System.out.println(arrIndexResolver.loopVariablesForStmt(stmt));
					System.out.println("Index resolve for stmt: " + stmt + ", for index " + index);
					System.out.println(Arrays.toString(res));
					if (res == null) {
						return false;
					}
					arrayResolvesForArgs.add(res);
				}
			}
			if (arg2 != null) {
				if (arg2.isArray()) {
					Name index = ((ArrayName)arg2).getIndex();
					res = arrIndexResolver.resolveIndex(stmt, index, null);
					System.out.println("##############");
					System.out.println("Loop vars for stmt: " + stmt);
					System.out.println(arrIndexResolver.loopVariablesForStmt(stmt));
					System.out.println("Index resolve for stmt: " + stmt + ", for index " + index);
					System.out.println(Arrays.toString(res));
					if (res == null) {
						return false;
					}
					arrayResolvesForArgs.add(res);
				}
			}
		}
		
		// Perform pairwise analysis between dest resolutions and arg resolutions of
		// array name indices
		AccessPattern accPattern;
		// Calculate the index of the loop variable corresponding to the given loopId
		int loopVarIndex = arrIndexResolver.loopVariablesForStmt(
				methodStmts.get(forBodyLabelIndex)).size()-1;
		for (Integer[] destRes : arrayResolvesForDests) {
			for (Integer[] argRes : arrayResolvesForArgs) {
				// Standardize since one index may be more nested than the other and
				// thus, the resolution arrays may be of different sizes
				if (destRes.length < argRes.length) {
					Integer[] newDestRes = new Integer[argRes.length];
					int i = 0;
					for (i = 0; i < destRes.length-1; i++) {
						newDestRes[i] = destRes[i];
					}
					for (; i < argRes.length-1; i++) {
						// Place filler 0 to represent no use of loop variable
						newDestRes[i] = 0;
					}
					newDestRes[i] = destRes[destRes.length-1];
					destRes = newDestRes;
					
				} else  if (destRes.length > argRes.length) {
					Integer[] newArgRes = new Integer[destRes.length];
					int i = 0;
					for (i = 0; i < argRes.length-1; i++) {
						newArgRes[i] = destRes[i];
					}
					for (; i < destRes.length-1; i++) {
						// Place filler 0 to represent no use of loop variable
						newArgRes[i] = 0;
					}
					newArgRes[i] = argRes[argRes.length-1];
					argRes = newArgRes;
					
				}
				accPattern = Analyze.getAccessPattern(destRes, argRes);
				
				if (!isParallelizable(accPattern, loopVarIndex)) {
					return false;
				}
			}
		}
		return true;
	}
	
	// Returns true if the loop at index is parallelizable
	// The distance vector has metrics in order of the loops nested level
	private boolean isParallelizable(AccessPattern pattern, int index) {
		if (pattern.distanceExists) {
			Integer[] distanceVector = pattern.distance;
			int numGtThanZero = 0;
			int numNotZero = 0;
			for (int i = 0; i < index; i++) {
				if (distanceVector[i] > 0) {
					numGtThanZero++;
				}
				if (distanceVector[i] != 0) {
					numNotZero++;
				}
			}
			if (numNotZero == 0) {
				if (distanceVector[index] == 0) {
					return true;
				}
			} else if (numGtThanZero == 1) {
				return true;
			}
		}
		return false;
	}
	
	private List<String> getAllLoopIds() {
		List<String> uniqueLoopIds = new ArrayList<String>();
		for (String s : mMap.keySet()) {
			for (LIRStatement stmt : mMap.get(s).getStatements()) {
				if (stmt.getClass().equals(LabelStmt.class)) {
					LabelStmt lStmt = (LabelStmt)stmt;
					String labelStr = lStmt.getLabelString();
					if (labelStr.matches(ForBodyLabelRegex)) {
						uniqueLoopIds.add(getIdFromForLabel(labelStr));
					}
				}
			}
		}
		return uniqueLoopIds;
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
