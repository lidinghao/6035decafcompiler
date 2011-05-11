package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.StoreStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class LocalLoadStoreDC {
	private Set<Name> loads;
	private Set<Name> stores;
	private HashMap<String, MethodIR> mMap;
	
	public LocalLoadStoreDC(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.loads = new HashSet<Name>();
		this.stores = new HashSet<Name>();
	}
	
	public void dcLoadStores()  {
		for (String methodName: this.mMap.keySet()) {
			for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
				DC(block);
			}
		}
	}

	private void DC(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (LIRStatement stmt : block.getStatements()) {
			if (stmt.getClass().equals(LoadStmt.class)) {
				LoadStmt lStmt = (LoadStmt) stmt;
				if (this.loads.contains(lStmt.getVariable())) {
					continue;
				}
				else {
					this.loads.add(lStmt.getVariable());
				}
				this.stores.remove(lStmt.getVariable());
			}
			else if (stmt.getClass().equals(StoreStmt.class)) {
				StoreStmt sStmt = (StoreStmt) stmt;
				if (this.stores.contains(sStmt.getVariable())) {
					continue;
				}
				else {
					this.stores.add(sStmt.getVariable());
				}
				this.loads.remove(sStmt.getVariable());
			}
			else if (stmt.getClass().equals(QuadrupletStmt.class)) {
				killLoadStores((QuadrupletStmt)stmt);
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				CallStmt cStmt = (CallStmt) stmt;
				if (cStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				
				List<Name> globals = new ArrayList<Name>();
				for (Name n: this.loads) {
					if (!n.isGlobal()) {
						globals.add(n);
					}
				}
				
				this.loads.clear();
				this.loads.addAll(globals);
				
				globals.clear();
				for (Name n: this.stores) {
					if (!n.isGlobal()) {
						globals.add(n);
					}
				}
				
				this.stores.clear();
				this.stores.addAll(globals);
			}

			newStmts.add(stmt);
		}
		
		block.setStatements(newStmts);		
	}

	private void killLoadStores(QuadrupletStmt stmt) {
		List<Name> all = new ArrayList<Name>();
		all.addAll(this.loads);
		all.addAll(this.stores);
		
		for (Name name : all) {
			boolean resetName = false;
			
			if (name.isArray()) {
				Name myName = name;
				
				do {
					ArrayName array = (ArrayName) myName;
					if (array.getIndex().equals(stmt.getDestination())) { // Index being reassigned, KILL!
						resetName = true;
					}
					
					myName = array.getIndex();
					
				} while (myName.isArray());
				
				if (stmt.getDestination().isArray()) {
					ArrayName dest = (ArrayName) stmt.getDestination();
					ArrayName arrName = (ArrayName) name;
					if (dest.getIndex().getClass().equals(ConstantName.class) &&
							!arrName.getIndex().getClass().equals(ConstantName.class)) {
						if (arrName.getId().equals(dest.getId())) {
							resetName = true;
						}
					}
				}
			}
			if (resetName) {
				this.loads.remove(stmt.getDestination());
			}
		}
	}
}
