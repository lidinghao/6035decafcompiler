package decaf.codegen.flattener;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import decaf.codegen.flatir.*;
import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.MethodDecl;

public class LocationResolver {
	private int stackOffset;
	private HashMap<Name, Location> locationMap;
	private ProgramFlattener pf;
	private ClassDecl cd;
	private String methodName; 
	
	public LocationResolver(ProgramFlattener pf, ClassDecl cd) {
		this.stackOffset = -1;
		this.locationMap = new HashMap<Name, Location>();
		this.pf = pf;
		this.cd = cd;
		this.methodName = null;
	}
	
	public void resolveLocations() {
		for (Entry<String, List<LIRStatement>> entry: this.pf.getLirMap().entrySet()) {	
			methodName = entry.getKey();
			locationMap.clear();
			int offsetForArgs = Math.min(getParamCount(methodName), 6);
			stackOffset = -(1 + offsetForArgs);
			
			List<LIRStatement> flatIR = entry.getValue();
			for (LIRStatement stmt: flatIR) {
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					resolveName(qStmt.getArg1());
					resolveName(qStmt.getArg2());
					resolveName(qStmt.getDestination());
				}
				else if (stmt.getClass().equals(CmpStmt.class)) {
					CmpStmt cStmt = (CmpStmt)stmt;
					resolveName(cStmt.getArg1());
					resolveName(cStmt.getArg2());
				}
				else if (stmt.getClass().equals(PopStmt.class)) {
					PopStmt popStmt = (PopStmt)stmt;
					resolveName(popStmt.getName());
				}
				else if (stmt.getClass().equals(PushStmt.class)) {
					PushStmt pushStmt = (PushStmt)stmt;
					resolveName(pushStmt.getName());
				}
			}
			
			int offset = (this.stackOffset * -1) - 1;
			setStackOffset(entry.getValue(), offset);
		}
	}

	private void setStackOffset(List<LIRStatement> value, int offset) {
		for (LIRStatement stmt: value) {
			if (stmt.getClass().equals(EnterStmt.class)) {
				((EnterStmt)stmt).setStackSize(offset);
			}
		}
	}

	public void printLocations(PrintStream outStream) {
		for (Entry<String, List<LIRStatement>> entry: this.pf.getLirMap().entrySet()) {	
			outStream.println(entry.getKey() +":");
			Set<String> out = new HashSet<String>();
			
			List<LIRStatement> flatIR = entry.getValue();
			for (LIRStatement stmt: flatIR) {
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					out.add(getLocationEntry(qStmt.getArg1()));
					out.add(getLocationEntry(qStmt.getArg2()));
					out.add(getLocationEntry(qStmt.getDestination()));
				}
				else if (stmt.getClass().equals(PopStmt.class)) {
					PopStmt popStmt = (PopStmt)stmt;
					out.add(getLocationEntry(popStmt.getName()));
				}
				else if (stmt.getClass().equals(PushStmt.class)) {
					PushStmt pushStmt = (PushStmt)stmt;
					out.add(getLocationEntry(pushStmt.getName()));
				}
			}
			
			for (String s: out) {
				if (!s.equals("null")) {
					outStream.println(s);
				}
			}
			
			outStream.println();
		}
	}
	
	public void resolveName(Name name) {
		if (name == null) return;
		
		if (name.getClass().equals(TempName.class)) {
			resolveTempName(name);
		}
		else if (name.getClass().equals(ArrayName.class)) {
			resolveArrayName(name);
		}
		else if (name.getClass().equals(VarName.class)) {
			resolveVarName(name);
		}
		else if (name.getClass().equals(RegisterName.class)){
			name.setLocation(new RegisterLocation((RegisterName)name));
		}
		else if (name.getClass().equals(DynamicVarName.class)) {
			resolveDynamicVarName(name);
		}
		else {
			ConstantName c = (ConstantName) name;
			name.setLocation(new ConstantLocation(c.getValue()));
		}
	}

	private void resolveDynamicVarName(Name name) {
		if (!locationMap.containsKey(name)) {
			Location loc = new StackLocation(stackOffset);
			name.setLocation(loc);
			locationMap.put(name, loc);
			stackOffset--;
		}
		else {
			name.setLocation(locationMap.get(name)); // Set already defined location
		}		
	}

	private void resolveTempName(Name name) {
		if (!locationMap.containsKey(name)) {
			Location loc = new StackLocation(stackOffset);
			name.setLocation(loc);
			locationMap.put(name, loc);
			stackOffset--;
		}
		else {
			name.setLocation(locationMap.get(name)); // Set already defined location
		}
	}

	private void resolveArrayName(Name name) {
		ArrayName arrayName = (ArrayName)name;
		GlobalLocation loc = new GlobalLocation(arrayName.getId());
		resolveName(arrayName.getIndex());
		loc.setOffset(arrayName.getIndex().getLocation());
		name.setLocation(loc);
	}

	private void resolveVarName(Name name) {
		if (!locationMap.containsKey(name)) {
			VarName varName = (VarName)name;
			
			if (varName.isString()) { // Set as global
				GlobalLocation gLoc = new GlobalLocation(varName.getId(), true, varName.getStringValue());
				name.setLocation(gLoc);
				if (varName.getStringValue() != null) {
					this.pf.getDataStmtList().add(new DataStmt(varName.getId(), varName.getStringValue()));
				}
			}
			else if (varName.getBlockId() == -1) {
				GlobalLocation gLoc = new GlobalLocation(varName.getId());
				name.setLocation(gLoc);
			}
			else if (varName.getBlockId() == -2) { // Is an argument
				int argIndex = this.findArgumentIndex(methodName, varName.getId());
				this.processArgumentLocation(name, argIndex);
			}
			else {
				Location loc = new StackLocation(stackOffset);
				name.setLocation(loc);
				locationMap.put(name, loc);
				stackOffset--;
			}
		}
		else {
			name.setLocation(locationMap.get(name)); // Set already defined location
		}
	}
	
	public String getLocationEntry(Name name) {
		if (name == null) return "null";
		return (name + ": " + name.getLocation());
	}
	
	public int findArgumentIndex(String methodName, String id) {
		for (MethodDecl md: cd.getMethodDeclarations()) {
			if (md.getId().equals(methodName)) {
				for (int i = 0; i < md.getParameters().size(); i++) {
					if (id.equals(md.getParameters().get(i).getId())) {
						return i;
					}
				}
			}
		}
		
		return 0;
	}
	
	private void processArgumentLocation(Name name, int index) {
		if (index < 6) {
			StackLocation loc = null;
			switch (index) {
				case 0:
					loc = new StackLocation(-1);
					break;
				case 1:
					loc = new StackLocation(-2);
					break;
				case 2:
					loc = new StackLocation(-3);
					break;
				case 3:
					loc = new StackLocation(-4);
					break;
				case 4:
					loc = new StackLocation(-5);
					break;
				case 5:
					loc = new StackLocation(-6);
					break;
			}
			
			name.setLocation(loc);
		}
		else {
			int stackOffset = index - 4; // Start from -16(%rbp)
			name.setLocation(new StackLocation(stackOffset));
		}
	}
	
	private int getParamCount(String methodName) {
		for (MethodDecl md: cd.getMethodDeclarations()) {
			if (md.getId().equals(methodName)) {
				return md.getParameters().size();
			}
		}
		
		return 0;
	}
}
