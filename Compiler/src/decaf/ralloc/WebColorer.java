package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Register;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.MethodIR;

public class WebColorer {
	public static int regCount = Register.availableRegs.length - 2;
	public static int combinedWebId = -1;
	private HashMap<String, MethodIR> mMap;
	private WebGenerator webGen;
	private Stack<Integer> coloringStack;
	private List<Integer> spillList;
	private HashMap<Integer, Set<Integer>> webGraph;
	private HashMap<Integer, Register> registersAssigned;
	private HashMap<String, List<Register>> registersUsed;
	
	public WebColorer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.webGen = new WebGenerator(this.mMap);
		this.coloringStack = new Stack<Integer>();
		this.spillList = new ArrayList<Integer>();
		this.webGraph = new HashMap<Integer, Set<Integer>>();
		this.registersAssigned = new HashMap<Integer, Register>();
		this.setRegistersUsed(new HashMap<String, List<Register>>());
		this.registersUsed = new HashMap<String, List<Register>>();
	}
	
	private void reset() {
		this.coloringStack.clear();
		this.spillList.clear();
		this.webGraph.clear();
		this.registersAssigned.clear();
	}
	
	public void colorWebs() {
		reset();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;

			reset();
			
			this.webGen.generateWebs(); 
			
			buildGraph(methodName); // Build
			
			while (!this.webGraph.isEmpty()) {
				int oldSize = -1;
				int newSize = this.webGraph.keySet().size();
				
				while (oldSize != newSize) {
					simplifyGraph(methodName); // Remove low degree nodes, add to stack
					oldSize = newSize;
					newSize = this.webGraph.keySet().size();
				}
				
				// Potentially spill webs and jump to simplify again
				spillWebs(methodName);
			}
			
			selectWebs(methodName);
			assignRegisters(methodName);
		}
	}
	
	private void assignRegisters(String methodName) {
		for (Web web: this.webGen.getWebMap().get(methodName)) {
			int id = web.getId();
			if (this.spillList.contains(id)) {
				web.setRegister(null);
			}
			else {
				web.setRegister(this.registersAssigned.get(id));
			}
		}
		
		Set<Register> registers = new HashSet<Register>();
		for (Register r: this.registersAssigned.values()) {
			registers.add(r);
		}
		
		List<Register> list = new ArrayList<Register>();
		list.addAll(registers);
		this.registersUsed.put(methodName, list);
	}

	private boolean selectWebs(String methodName) {
		//preColorWebs(methodName);
		
		List<Register> used = new ArrayList<Register>();
		while (!this.coloringStack.isEmpty()) {
			int webId = this.coloringStack.pop();
			
			if (this.spillList.contains(webId)) continue; // spill instead
			
			if (this.registersAssigned.containsKey(webId)) {
				continue;
			}
			
			Web web = getWebFromId(methodName, webId);
			for (Web neighbor: web.getInterferingWebs()) { // Want original state of graph now!
				int wid = neighbor.getId();
				if (this.registersAssigned.containsKey(wid)) {
					used.add(this.registersAssigned.get(wid));
				}
			}
			
			boolean assigned = false;
			for (int i = 0; i < regCount; i++) {
				Register reg = Register.availableRegs[i];
			
				if (used.contains(reg)) continue;
				assigned = true;
				this.registersAssigned.put(webId, reg);
			}
			
			if (!assigned) {
				System.out.println("ERROR!!");
				return false;
			}
		}
		
		return true;
	}

	private void spillWebs(String methodName) {
		if (this.webGraph.isEmpty()) return;
		
		// Determine web to spill based on heuristic
		int minHeuristic = 2147483647;
		int webToSpill = 0;
		for (int webId: this.webGraph.keySet()) {
			int myHeuristic = getHeuristicValue(methodName, webId);
			if (myHeuristic < minHeuristic) {
				minHeuristic = myHeuristic;
				webToSpill = webId;
			}
		}
		
		spillWeb(methodName, webToSpill);
	}

	private void spillWeb(String methodName, int webToSpill) {
		this.spillList.add(webToSpill);
		this.coloringStack.push(webToSpill);
		removeFromGraph(webToSpill); // remove from node
	}

	private int getHeuristicValue(String methodName, int webId) {
		Web web = getWebFromId(methodName, webId);
		int neighbors = this.webGraph.get(webId).size();
		
		List<LIRStatement> defUses = new ArrayList<LIRStatement>();
		defUses.addAll(web.getDefinitions());
		defUses.addAll(web.getUses());
		
		int depth = 0;
		for (LIRStatement stmt: defUses) {
			depth += stmt.getDepth();
		}
		
		return depth + neighbors;		
	}

	private Web getWebFromId(String methodName, int id) {
		for (Web web: this.webGen.getWebMap().get(methodName)) {
			if (web.getId() == id) return web;
		}
		
		return null;
	}

	private void buildGraph(String methodName) {
		for (Web web: this.webGen.getWebMap().get(methodName)) {
			Set<Integer> neighbors = new HashSet<Integer>();
			
			for (Web neighbor: web.getInterferingWebs()) {
				neighbors.add(neighbor.getId());
			}
			
			this.webGraph.put(web.getId(), neighbors);
		}
	}

	private void simplifyGraph(String methodName) {
		List<Integer> remove = new ArrayList<Integer>();
		
		for (Integer webId: this.webGraph.keySet()) {
			if (this.webGraph.get(webId).size() < regCount) { // low degree
				remove.add(webId);
			}
		}
		
		System.out.println("REMOVING FROM SIMPLIFY: " + remove);
		
		for (int webId: remove) {
			removeFromGraph(webId);
			this.coloringStack.push(webId);
		}
	}

	private void removeFromGraph(int webId) {
		// Remove from neighbors
		for (Integer neighbor: this.webGraph.get(webId)) {
			this.webGraph.get(neighbor).remove(webId);
		}
		
		// Remove from graph
		this.webGraph.remove(webId);
	}

	public WebGenerator getWebGen() {
		return webGen;
	}

	public void setWebGen(WebGenerator webGen) {
		this.webGen = webGen;
	}

	public void setRegistersUsed(HashMap<String, List<Register>> registersUsed) {
		this.registersUsed = registersUsed;
	}

	public HashMap<String, List<Register>> getRegistersUsed() {
		return registersUsed;
	}
}
