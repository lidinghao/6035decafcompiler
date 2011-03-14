package decaf.codegen.flattener;

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
			stackOffset = -1;
			
			List<LIRStatement> flatIR = entry.getValue();
			for (LIRStatement stmt: flatIR) {
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					resolveName(qStmt.getArg1());
					resolveName(qStmt.getArg2());
					resolveName(qStmt.getDestination());
				}
				else if (stmt.getClass().equals(PopStmt.class)) {
					PopStmt popStmt = (PopStmt)stmt;
					resolveName(popStmt.getAddress());
				}
				else if (stmt.getClass().equals(PushStmt.class)) {
					PushStmt pushStmt = (PushStmt)stmt;
					resolveName(pushStmt.getAddress());
				}
			}
		}
	}
	
	public void printLocations() {
		for (Entry<String, List<LIRStatement>> entry: this.pf.getLirMap().entrySet()) {	
			System.out.println(entry.getKey() +":");
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
					out.add(getLocationEntry(popStmt.getAddress()));
				}
				else if (stmt.getClass().equals(PushStmt.class)) {
					PushStmt pushStmt = (PushStmt)stmt;
					out.add(getLocationEntry(pushStmt.getAddress()));
				}
			}
			
			for (String s: out) {
				if (!s.equals("null")) {
					System.out.println(s);
				}
			}
			
			System.out.println();
		}
	}
	
	public void resolveName(Name name) {
		if (name == null) return;
		
		if (name.getClass().equals(TempName.class)) {
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
		else if (name.getClass().equals(ArrayName.class)) {
			ArrayName arrayName = (ArrayName)name;
			GlobalLocation loc = new GlobalLocation(arrayName.getId());
			resolveName(arrayName.getIndex());
			loc.setOffset(arrayName.getIndex().getLocation());
			name.setLocation(loc);
		}
		else if (name.getClass().equals(VarName.class)) {
			if (!locationMap.containsKey(name)) {
				VarName varName = (VarName)name;
				
				if (varName.isString()) { // Set as global
					GlobalLocation gLoc = new GlobalLocation(varName.getId(), true, varName.getStringValue());
					name.setLocation(gLoc);
					this.pf.getDataStmtList().add(new DataStmt(varName.getId(), varName.getStringValue()));
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
		else if (name.getClass().equals(RegisterName.class)){
			name.setLocation(new RegisterLocation((RegisterName)name));
		}
		else { // Constant
			Constant c = (Constant) name;
			name.setLocation(new ConstantLocation(c.getValue()));
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
			RegisterLocation loc = null;
			switch (index) {
				case 0:
					loc = new RegisterLocation(Register.RDI);
					break;
				case 1:
					loc = new RegisterLocation(Register.RSI);
					break;
				case 2:
					loc = new RegisterLocation(Register.RDX);
					break;
				case 3:
					loc = new RegisterLocation(Register.RCX);
					break;
				case 4:
					loc = new RegisterLocation(Register.R8);
					break;
				case 5:
					loc = new RegisterLocation(Register.R9);
					break;
			}
			
			name.setLocation(loc);
		}
		else {
			int stackOffset = index - 4; // Start from -15(%rbp)
			name.setLocation(new StackLocation(stackOffset));
		}
	}
}
