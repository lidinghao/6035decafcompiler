package decaf.optimize;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LeaveStmt;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.StoreStmt;
import decaf.codegen.flatir.TempName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.MethodDecl;
import decaf.ir.ast.Parameter;

public class MethodInliner {
	private ProgramFlattener pf;
	private ClassDecl cd;
	private HashMap<String, MethodDecl> methods;
	private HashMap<String,HashMap<String, VarName>> idToVar;
	private int counter;
	private int maxStatements = 50; 
	private int maxParameters = 6;
	
	//need to go through main only
	public MethodInliner(ProgramFlattener p, ClassDecl c) {
		this.cd = c;
		this.pf = p;
		this.methods = new HashMap<String, MethodDecl>();
		this.idToVar = new HashMap<String,HashMap<String, VarName>>();
		counter = 0;
		for(MethodDecl md: this.cd.getMethodDeclarations()){
			methods.put(md.getId(), md);
		}
	}
	
	
	public void InlineMethods() {
			String methodName = "main";
			processMethod(methodName, pf.getLirMap().get(methodName));
	}
	
	
	private void processMethod(String methodName, List<LIRStatement> statements) {
		String callee;
		LabelStmt endLabelStmt = null;
		LabelStmt beginLabelStmt = null;
		for(int i = 0; i < statements.size(); i++) {
			LIRStatement stmt = statements.get(i);
			if(stmt.getClass().equals(LabelStmt.class)){ // label
				
				String label = ((LabelStmt) stmt).getLabelString();
				if(label.contains(".begin") && label.contains(".mcall.") ){
					callee = getLabelCallee(label); 
					beginLabelStmt = (LabelStmt) stmt;
					for(int j = i+1; j<statements.size(); j++){ 
						LIRStatement methodStmt = statements.get(j);
						if(methodStmt.getClass().equals(LabelStmt.class)){
							String endLabel = ((LabelStmt) methodStmt).getLabelString();
							if(endLabel.contains(".end") && endLabel.contains(callee)) {
								endLabelStmt = (LabelStmt) methodStmt;
								break;
							} 
						}
					}
					if(this.methods.get(callee)!= null && 
							this.pf.getLirMap().get(callee).size() <= maxStatements && 
							this.methods.get(callee).getParameters().size() <= maxParameters &&
							!isRecursive(callee)) {
						counter++;
						processCall(statements,beginLabelStmt, endLabelStmt, methodName);
					}
				}
			}
		}
	}
	
	
	private void processCall(List<LIRStatement> statements, LabelStmt beginLabel, LabelStmt endLabel, String caller){
		int start = statements.indexOf(beginLabel);
		int end = statements.indexOf(endLabel);
		String callee = getLabelCallee(beginLabel.getLabelString());
		List<LIRStatement> calleeStmts = this.pf.getLirMap().get(callee);
		beginLabel.setLabel(updateMcallLabel(beginLabel.getLabelString()));
		endLabel.setLabel(updateMcallLabel(endLabel.getLabelString()));
		if(this.methods.get(callee) != null) {
			updateIdToVarMap(calleeStmts, statements, start, callee); // updates the variables in the inlined method		
		}
		for(int i = start;i<end; i++) {
			LIRStatement stmt = statements.get(i);
			if(stmt.getClass().equals(CallStmt.class) && ((CallStmt) stmt).getMethodLabel().equals(callee)){ // find the function call
				statements.remove(stmt); // remove the call
				int parameters = this.methods.get(callee).getParameters().size();
				for(int j = 2; j < calleeStmts.size()- 2-parameters; j++) { // to avoid enter statement and skip parameters 
					LIRStatement calleeStmt = calleeStmts.get(j+parameters);
						
					if (calleeStmt.getClass().equals(LeaveStmt.class)) {
						processLeaveStmt(statements, endLabel, i, j);
					} else if(calleeStmt.getClass().equals(QuadrupletStmt.class)) {
						processQuadrupletStmt(statements, i, j,calleeStmt, callee);	
					} else if(calleeStmt.getClass().equals(CmpStmt.class)) {
						processCmpStmt(statements, i, j, calleeStmt, callee);
					} else if(calleeStmt.getClass().equals(PushStmt.class)) {
						processPushStmt(statements, i, j, calleeStmt, callee);
					} else if (calleeStmt.getClass().equals(PopStmt.class)) {
						processPopStmt(statements, i, j, calleeStmt, callee);
					} else if (calleeStmt.getClass().equals(LabelStmt.class)) {
						processLabelStmt(calleeStmts, statements, i, j, calleeStmt, callee);
					} else if (calleeStmt.getClass().equals(JumpStmt.class)) {
						processJumpStmt(statements, i, j, calleeStmt);
					} else {
						statements.add(i+j-2, (LIRStatement) calleeStmt.clone());
					}		
				}
				break;
			} 	
		}	
		propagateRAX(statements, endLabel);

	}


	private void propagateRAX(List<LIRStatement> statements, LabelStmt endLabel) {
		
		int end;
		end = statements.indexOf(endLabel);
		if(statements.get(end-1).getClass().equals(QuadrupletStmt.class)){
			QuadrupletStmt qStmt = (QuadrupletStmt) statements.get(end-1);
			if(qStmt.getOperator() == QuadrupletOp.MOVE) {
				if(qStmt.getArg1().getClass().equals(RegisterName.class)) {
					if(((RegisterName) qStmt.getArg1()).getMyRegister() == Register.RAX) {
						statements.add(end+1,qStmt);
					}
				}
			}
		}
	}
	
	private boolean isRecursive(String MethodName) {
		for(LIRStatement s:this.pf.getLirMap().get(MethodName)){
			if(s.getClass().equals(CallStmt.class)) {
				CallStmt cStmt = (CallStmt) s;
				if(cStmt.getMethodLabel().equals(MethodName)){
					return true;
				}
			}
		}
		return false;
	}
	
	
	private void processJumpStmt(List<LIRStatement> statements, int i, int j,
			LIRStatement calleeStmt) {
		JumpStmt jcStmt = (JumpStmt) calleeStmt;
		JumpCondOp cond = jcStmt.getCondition();
		String label = jcStmt.getLabel().getLabelString();
		LabelStmt lStmt = new LabelStmt(label);
		JumpStmt jStmt = new JumpStmt(cond, lStmt);
		String newLabel = updateLabel(jStmt.getLabel().getLabelString());
		jStmt.getLabel().setLabel(newLabel);
		statements.add(i+j-2, jStmt);
	}
	private void processLabelStmt(List<LIRStatement> calleeStmts, List<LIRStatement> statements, int i, int j,
			LIRStatement calleeStmt, String caller) {
		LabelStmt lcStmt = (LabelStmt) calleeStmt;
		String label = lcStmt.getLabelString();
		if(label.contains(".mcall.") && label.contains(".begin") && !label.contains(".print.")) {
			String newLabel = updateMcallLabel(label);
			LabelStmt lStmt = new LabelStmt(newLabel);
			statements.add(i+j-2, lStmt);
			String callee = getLabelCallee(label);
			LabelStmt endLabelStmt = null;
			LabelStmt beginLabelStmt = (LabelStmt) lcStmt;
			int start = calleeStmts.indexOf(calleeStmt);
			for(int k = start+1; k<statements.size(); k++){ 
				LIRStatement methodStmt = calleeStmts.get(k);
				if(methodStmt.getClass().equals(LabelStmt.class)){
					String endLabel = ((LabelStmt) methodStmt).getLabelString();
					if(endLabel.contains(".end") && endLabel.contains(callee)) {
						endLabelStmt = (LabelStmt) methodStmt;
						break;
					} 
				}
			}
			if(this.methods.get(callee)!= null && 
					this.pf.getLirMap().get(callee).size() <= maxStatements && 
					this.methods.get(callee).getParameters().size() <= maxParameters &&
					!isRecursive(callee)) {
				counter++;
				processCall(calleeStmts,beginLabelStmt, endLabelStmt, callee);
			}
		} else {
			String newLabel;
			if(label.contains("mcall")) {
				newLabel = updateMcallLabel(label);
			}else {
				newLabel = updateLabel(label);
			}
			LabelStmt lStmt = new LabelStmt(newLabel);
			statements.add(i+j-2, lStmt);
		}
	}

	private void processPopStmt(List<LIRStatement> statements, int i, int j,
			LIRStatement calleeStmt, String callee) {
		PopStmt pStmt  = (PopStmt) calleeStmt.clone();
		if(pStmt.getName().getClass().equals(VarName.class)) {
			String id = ((VarName) pStmt.getName()).getId();
			if(idToVar.get(callee).containsKey(id)) {
				pStmt.setName(idToVar.get(callee).get(id));
			}
		}
		statements.add(i+j-2, pStmt);
	}

	private void processPushStmt(List<LIRStatement> statements, int i, int j,
			LIRStatement calleeStmt, String callee) {
		PushStmt pStmt  = (PushStmt) calleeStmt.clone();
		if(pStmt.getName().getClass().equals(VarName.class)) {
			String id = ((VarName) pStmt.getName()).getId();
			if(idToVar.get(callee).containsKey(id)) {
				pStmt.setName(idToVar.get(callee).get(id));
			}
		}
		statements.add(i+j-2, pStmt);
	}

	private void processCmpStmt(List<LIRStatement> statements, int i, int j,
			LIRStatement calleeStmt, String callee) {
		CmpStmt cStmt  = (CmpStmt) calleeStmt.clone();
		if(cStmt.getArg1().getClass().equals(VarName.class)) {
			String id = ((VarName) cStmt.getArg1()).getId();
			if(idToVar.containsKey(callee)&&idToVar.get(callee).containsKey(id)) {
				cStmt.setArg1(idToVar.get(callee).get(id));
			}
		}
		if(cStmt.getArg2().getClass().equals(VarName.class)) {
			String id = ((VarName) cStmt.getArg2()).getId();
			if(idToVar.containsKey(callee)&&idToVar.get(callee).containsKey(id)) {
				cStmt.setArg2(idToVar.get(callee).get(id));
			}
		}
		statements.add(i+j-2, cStmt);
	}

	private void processQuadrupletStmt(List<LIRStatement> statements, int i,
			int j, LIRStatement calleeStmt, String callee) {
		QuadrupletStmt qCalleeStmt = (QuadrupletStmt) calleeStmt.clone();
		if(qCalleeStmt.getArg1().getClass().equals(VarName.class)) {
			String id = ((VarName) qCalleeStmt.getArg1()).getId();
			if(idToVar.containsKey(callee)&&idToVar.get(callee).containsKey(id)) {
				qCalleeStmt.setArg1(idToVar.get(callee).get(id));
			}
		}
		if(qCalleeStmt.getArg2()!= null && qCalleeStmt.getArg2().getClass().equals(VarName.class)) {
			String id = ((VarName) qCalleeStmt.getArg2()).getId();
			if(idToVar.containsKey(callee)&&idToVar.get(callee).containsKey(id)) {
				qCalleeStmt.setArg2(idToVar.get(callee).get(id));
			}
		}
			
		if(qCalleeStmt.getDestination().getClass().equals(VarName.class)) {
			String id = ((VarName) qCalleeStmt.getDestination()).getId();
			if(idToVar.containsKey(callee)&&idToVar.get(callee).containsKey(id)) {
				qCalleeStmt.setDestination(idToVar.get(callee).get(id));
			}
		}
		statements.add(i+j-2, qCalleeStmt);
	}

	private void processLeaveStmt(List<LIRStatement> statements,
			LabelStmt endLabel, int i, int j) {
		JumpStmt jump = new JumpStmt(JumpCondOp.NONE, endLabel);
		statements.add(i+j-2, jump);
	}

	private void updateIdToVarMap(List<LIRStatement> calleeStmts,List<LIRStatement> statements, int start,
			String callee) {
		Random r = new Random(); // random block id
		int k = r.nextInt();
		List<Parameter> parameters = this.methods.get(callee).getParameters();
		for(int j = start; j <= start + parameters.size(); j++){ // go through all the parameters
			LIRStatement stmt = statements.get(j);
			if(stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				String varId = "$"+ parameters.get(j-start-1).getId();
				VarName var = new VarName(varId);
				var.setBlockId(k);
				qStmt.setDestination(var);
				if(idToVar.containsKey(callee)) {
					idToVar.get(callee).put(parameters.get(j-start-1).getId(), var);
				} else {
					idToVar.put(callee, new HashMap<String, VarName>());
					idToVar.get(callee).put(parameters.get(j-start-1).getId(), var);
				}
			} else if (stmt.getClass().equals(PushStmt.class)){
				PushStmt pStmt = (PushStmt) stmt;
				int varBlockId = k;
				String varId = "$"+ parameters.get(j-start - 1).getId();
				VarName var = new VarName(varId);
				var.setBlockId(varBlockId);
				QuadrupletStmt newStmt = new QuadrupletStmt(QuadrupletOp.MOVE, var, pStmt.getName(), null);
				statements.set(j, newStmt);
				if(idToVar.containsKey(callee)) {
					idToVar.get(callee).put(parameters.get(j-start-1).getId(), var);
				} else {
					idToVar.put(callee, new HashMap<String, VarName>());
				}
			}
		}
	}
	
	private String getLabelCallee(String label) {
		int dots = 0;
		String callee = null;
		int begin = 0;
		boolean inCallee = false;
		for(int i = 0; i < label.length(); i++) {
			if (label.charAt(i) == '.') {
				dots++;
			}
			if(dots==2 && inCallee == false) {
				begin = i + 1;
				inCallee = true;
			}
			if(dots == 3){
				callee = label.substring(begin, i);
				break;
			}
		}
		return callee;
	}
	
	private String updateMcallLabel(String mcall) {
		int dot = 0;
		for(int i = 0; i < mcall.length(); i++) {
			if (mcall.charAt(i) == '.') {
				dot++;
			}
			if(dot == 2) {
				String end = mcall.substring(i+1);
				String start = mcall.substring(0, i+1);
				mcall = start + "inline." + counter + "." + end;
				break;
			}
		}
		return mcall;
	}
	private String updateLabel(String label) {
		if(!label.contains(".inline.")){
			int dot = 0;
			for(int i = 0; i < label.length(); i++) {
				if (label.charAt(i) == '.') {
					dot++;
				}
				if(dot == 1) {
					String end = label.substring(i);
					String start = label.substring(0, i);
					label = start + "." + counter + end;
					break;
				}
			}
			//label =  label + "." + counter;
		} else {
			label = updateMcallLabel(label);
		}
		return label;
	}

	
	
}
