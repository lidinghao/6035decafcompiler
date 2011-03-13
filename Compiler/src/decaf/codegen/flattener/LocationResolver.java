package decaf.codegen.flattener;

import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.*;

public class LocationResolver {
	private int stackOffset;
	private HashMap<Name, Location> locationMap;
	private ProgramFlattener pf;
	
	public LocationResolver(ProgramFlattener pf) {
		this.stackOffset = 1;
		this.locationMap = new HashMap<Name, Location>();
		this.pf = pf;
	}
	
	public int resolveLocations(List<LIRStatement> flatIR) {
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
		
		return stackOffset; // Should equal x+1, where x is the same as enter 'x'
	}
	
	public void printLocations(List<LIRStatement> flatIR) {
		for (LIRStatement stmt: flatIR) {
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
				printName(qStmt.getArg1());
				printName(qStmt.getArg2());
				printName(qStmt.getDestination());
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt popStmt = (PopStmt)stmt;
				printName(popStmt.getAddress());
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pushStmt = (PushStmt)stmt;
				printName(pushStmt.getAddress());
			}
		}
	}
	
	public void resolveName(Name name) {
		if (name == null) return;
		
		if (name.getClass().equals(TempName.class)) {
			if (!locationMap.containsKey(name)) {
				Location loc = new StackLocation(stackOffset);
				name.setLocation(loc);
				locationMap.put(name, loc);
				stackOffset++;
			}
			else {
				name.setLocation(locationMap.get(name)); // Set already defined location
			}
		}
		else if (name.getClass().equals(ArrayName.class)) {
			ArrayName arrayName = (ArrayName)name;
			GlobalLocation loc = new GlobalLocation(arrayName.getId());
			resolveName(arrayName.getIndex());
			loc.setOffset(arrayName.getIndex());
			name.setLocation(loc);
		}
		else if (name.getClass().equals(VarName.class)) {
			if (!locationMap.containsKey(name)) {
				VarName varName = (VarName)name;
				
				if (varName.isString()) { // Set as global
					GlobalLocation gLoc = new GlobalLocation(varName.getId(), true, varName.getStringVal());
					name.setLocation(gLoc);
					this.pf.getDataStmtList().add(new DataStmt(varName.getId(), varName.getStringVal()));
				}
				else if (varName.getBlockId() == -1) {
					GlobalLocation gLoc = new GlobalLocation(varName.getId());
					name.setLocation(gLoc);
				}
				else {
					Location loc = new StackLocation(stackOffset);
					name.setLocation(loc);
					locationMap.put(name, loc);
					stackOffset++;
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
			name.setLocation(null);
		}
	}
	
	public void printName(Name name) {
		if (name == null) return;
		
		if (name.getClass().equals(Constant.class)) {
			System.out.println(name + ": $" + name);
		}
		else {
			System.out.println(name + ": " + name.getLocation());
		}
	}
}
