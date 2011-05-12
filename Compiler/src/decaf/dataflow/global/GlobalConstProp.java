package decaf.dataflow.global;

import java.util.HashMap;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class GlobalConstProp {
	private ConstReachingDef crd;
	private HashMap<String, MethodIR> mMap;
	private String methodName;
	
	public GlobalConstProp(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.crd = new ConstReachingDef(mMap);
	}
	
	public void performConstProp() {
		crd.analyze();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			this.methodName = methodName;
			for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
				optimizeBlock(block);
			}
		}
	}

	private void optimizeBlock(CFGBlock block) {
		for (LIRStatement stmt: block.getStatements()) {
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
				// For each use of a Name, see all the reaching definitions for that Name
				// If all reaching definitions assign the Name to the same constant, replace Name with that constant
				qStmt.setArg1(processArg(qStmt.getArg1(), stmt));
				qStmt.setArg2(processArg(qStmt.getArg2(), stmt));
			// Optimize PopStmt
			} else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt popStmt = (PopStmt)stmt;
				popStmt.setName(processArg(popStmt.getName(), stmt));
			
			// Optimize PushStmt
			} else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pushStmt = (PushStmt)stmt;
				pushStmt.setName(processArg(pushStmt.getName(), stmt));
			// Optimize CmpStmt
			} 
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt)stmt;
				cStmt.setArg1(processArg(cStmt.getArg1(), stmt));
				cStmt.setArg2(processArg(cStmt.getArg2(), stmt));
			}
		}
		
	}
	
	public Name processArg(Name name, LIRStatement stmt) {
		if (name == null) return name;
		
		if (!name.isArray()) return processName(name, stmt);
		
		if (!processName(name, stmt).equals(name)) {
			return processName(name, stmt);
		}
		else {
			ArrayName arrName = (ArrayName) name;
			arrName.setIndex(processArg(arrName.getIndex(), stmt));
			return name;
		}
	}
	
	public Name processName(Name name, LIRStatement stmt) {
		if (name == null) return null;
		
		ConstantName assigned = null;
		
		for (int i = 0; i < this.crd.getUniqueDefinitions().get(this.methodName).size(); i++) {
			if (stmt.getReachingDefInSet().get(i)) {
				LIRStatement def = this.crd.getUniqueDefinitions().get(this.methodName).get(i);
				
				QuadrupletStmt qStmt = (QuadrupletStmt) def;
				if (qStmt.getDestination().equals(name)) {
					if (assigned == null) {
						assigned = (ConstantName) qStmt.getArg1();
					}
					else if (!qStmt.getArg1().equals(assigned)) {
						return name;
					}
				}
			}
		}
		
		if (assigned == null) return name;
		
		return assigned;
	}
}
