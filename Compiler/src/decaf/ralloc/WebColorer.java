package decaf.ralloc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.MethodIR;

public class WebColorer {
	public static int regCount = 3; //Register.availableRegs.length;
	private HashMap<String, MethodIR> mMap;
	private WebGenerator webGen;
	private HashMap<String, Stack<Web>> coloringStack;
	private HashMap<Web, List<Web>> adjacencyList;
	private List<Web> splitList;
	private List<Web> graph;
	private WebSplitter splitter;
	private HashMap<String, List<Register>> registersUsed;
	
	public WebColorer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.webGen = new WebGenerator(this.mMap);
		this.coloringStack = new HashMap<String, Stack<Web>>();	
		this.adjacencyList = new HashMap<Web, List<Web>>();
		this.splitList = new ArrayList<Web>();
		this.graph = new ArrayList<Web>();
		this.splitter = new WebSplitter(mMap, this.webGen.getDefAnalyzer(), this.webGen.getLivenessAnalyzer());
		this.registersUsed = new HashMap<String, List<Register>>();
	}
	
	public void colorWebs() {
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			// Make graph colorable (if not already)
			int i = 0;
			while (true) {
				this.webGen.generateWebs();
				
				for (LIRStatement stmt : this.mMap.get(methodName).getStatements()) {
					System.out.println(stmt);
				}
				
				System.out.println("\nWEBS GENERATED: " + methodName);
				System.out.println("VARS : " + this.webGen.getLivenessAnalyzer().getUniqueVariables().get(methodName) + "\n");
				for (Web w: this.webGen.getWebMap().get(methodName)) {
					System.out.println(w + "\n");
				}
				
				System.out.println("INTERFERENCE GRAPH: ");
				this.webGen.printInterferenceGraph(System.out, methodName);
				System.out.println();
				
				if (tryColoring(methodName)) { // Coloring succeeded, no more splits
					break;
				}
				
				System.out.println("SPLIT LIST: " + this.getWebIdentifiers(this.splitList));
				
				splitWebs(methodName);
//				break;
				i++;
			}
			
			this.mMap.get(methodName).regenerateStmts();
			
			System.out.println("SPLITTING COMPLETED SUCCESSFULLY! \n");
		}
		
		System.out.println("COLORING METHODS");
		
		// Color graph
		colorGraph();
		
		System.out.println("COLORING COMPLETED SUCCESSFULLY");
		
		this.prettyPrintWebsWithRegisters(System.out);
	}

	private void colorGraph() {
		this.webGen.generateWebs();
		
		for (String methodName: this.webGen.getWebMap().keySet()) {
			System.out.println("COLORING: " + methodName);
			colorMethodWebs(methodName, this.webGen.getWebMap().get(methodName));
		}
	}

	private void colorMethodWebs(String methodName, List<Web> list) {
		System.out.println("IS THERE ERROR!: " + list.size() + "; " + this.coloringStack.get(methodName).size());
		
		List<Web> webs = new ArrayList<Web>();
		webs.addAll(list);
		
		// Coloring stack already has list
		Web webToColor = this.getWebToColor(methodName, list);
		
		while (webToColor != null) {
			colorWeb(webToColor);
			webToColor = this.getWebToColor(methodName, list);
		}
		
		Set<Register> used = new HashSet<Register>();
		for (Web w: this.webGen.getWebMap().get(methodName)) {
			if (w.getRegister() == null) {
				System.out.println("WEB NOT ASSIGNED REGISTER F*CK!");
			}
			else {
				used.add(w.getRegister());
			}
		}
		
		this.registersUsed.put(methodName, new ArrayList<Register>());
		this.registersUsed.get(methodName).addAll(used);
	}

	public HashMap<String, List<Register>> getRegistersUsed() {
		return registersUsed;
	}

	private void colorWeb(Web webToColor) {
		List<Register> colors = new ArrayList<Register>();
		for (int i = 0; i < Register.availableRegs.length; i++) {
			colors.add(Register.availableRegs[i]);
		}
		
		for (Web neighbor: webToColor.getInterferingWebs()) {
			if (neighbor.getRegister() != null) {
				colors.remove(neighbor.getRegister());
			}
		}
		
		for (LIRStatement def: webToColor.getDefinitions()) {
			if (def.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) def;
				if (qStmt.getArg1().getClass().equals(RegisterName.class)) {
					RegisterName rName = (RegisterName) qStmt.getArg1();
					if (colors.contains(rName.getMyRegister())) {
						webToColor.setRegister(rName.getMyRegister());
						return;
					}
				}
			}
		}
		
		webToColor.setRegister(colors.get(0));
	}

	private Web getWebToColor(String methodName, List<Web> list) {
		if (this.coloringStack.get(methodName).isEmpty()) return null;
		
		Web top = this.coloringStack.get(methodName).pop();
		
		for (Web w: list) {
			if (w.equals(top)) {
				return w;
			}
		}
		
		return null;
	}

	private void splitWebs(String methodName) {
		this.splitter.setState(methodName, splitList);
		this.splitter.split();
		this.mMap.get(methodName).regenerateStmts();
	}

	private boolean tryColoring(String methodName) {
		System.out.println("COLORING...");
		this.coloringStack.put(methodName, new Stack<Web>());
		this.splitList.clear();
		makeGraph(methodName);
		
		preColorGraph(methodName);
		
		while (true) {
			Web webToRemove = null;
			
			for (Web w: this.graph) {
				if (w.getInterferingWebs().size() < regCount
						&& noColorConflict(w)) {
					System.out.println("CAN COLOR: " + w.getIdentifier());
					webToRemove = w;
					break;
				}
			}
			
			if (webToRemove != null) { // Found web to remove
				this.saveAdjacencyList(webToRemove);
				this.removeFromGraph(webToRemove);
				this.coloringStack.get(methodName).push(webToRemove);
				System.out.println("NEW GRAPH: ");
				prettyPrintGraph(System.out);
				System.out.println();
			}
			else { // Could not remove
				if (this.graph.isEmpty()) { // Graph empty, done coloring
					return true;
				}
				
				this.splitList.addAll(this.graph); // Split one of the nodes left in graph
				
				System.out.println("COLORING FAILED!");
				
				return false;
			}
		}
	}
	
	private void preColorGraph(String methodName) {
		for (Web w: this.graph) {
			for (LIRStatement def: w.getDefinitions()) {
				if (def.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt) def;
					if (qStmt.getArg1().getClass().equals(RegisterName.class)) {
						RegisterName rName = (RegisterName) qStmt.getArg1();
						w.setRegister(rName.getMyRegister());
						break;
					}
				}
			}
		}
	}

	private boolean noColorConflict(Web w) {
		Register color = w.getRegister();
		
		if (color == null) return true;
		
		for (Web neighbor: w.getInterferingWebs()) {
			if (color == neighbor.getRegister()) {
				return false;
			}
		}
		
		return true;
	}

	private void prettyPrintWebsWithRegisters(PrintStream out) {
		for (String methodName: this.webGen.getWebMap().keySet()) {
			System.out.println("WEBS FOR: " + methodName);
			for (Web w: this.webGen.getWebMap().get(methodName)) {
				String rtn = w.getIdentifier() + " -> " + w.getRegister();
				out.println(rtn);
			}

			out.println();
		}
	}
	
	public void prettyPrintGraph(PrintStream out) {
		for (Web w: this.graph) {
			String rtn = "";
			rtn += w.getIdentifier() + " : [";
			for (Web w2: w.getInterferingWebs()) {
				rtn += w2.getIdentifier() + ", ";
			}
			rtn += "]";
			out.println(rtn);
		}
	}

	private void makeGraph(String methodName) {
		this.graph.clear();
		
		List<Web> notAdded = new ArrayList<Web>();
		
		for (Web w: this.webGen.getWebMap().get(methodName)) {
			if (w.getDefinitions().size() == 1) {
				if (w.getUses().isEmpty()) { // Store directly (def must be QuadrupletStmt assigning to global)
					//notAdded.add(w);
					//continue;
				}
				
				if (w.getDefinitions().get(0).getClass().equals(LoadStmt.class)) { // Single use, load directly
					if (w.getUses().size() == 1) {
						//notAdded.add(w);
						//continue;
					}
				}
			}
			
			this.graph.add(w);
		}
		
		// Remove edges for webs not added
		for (Web web: this.graph) {
			for (Web w: notAdded) {
				web.removeInterferingWeb(w);
			}
		}
	}

	private void saveAdjacencyList(Web w) {
		List<Web> neighbors =  new ArrayList<Web>();
		
		for (Web web: w.getInterferingWebs()) {
			neighbors.add(web);
		}
		
		this.adjacencyList.put(w, neighbors);
	}
	
	private void removeFromGraph(Web w) {
		this.graph.remove(w); // remove from graph
		for (Web web: this.adjacencyList.get(w)) {
			w.removeInterferingWeb(web); // Remove edges
		}
	}
	
	private String getWebIdentifiers(List<Web> webs) {
		String rtn = "[ ";
		
		for (Web w: webs) {
			rtn += w.getIdentifier() + ", ";
		}
		
		rtn += " ]";
		
		return rtn;
	}
}
