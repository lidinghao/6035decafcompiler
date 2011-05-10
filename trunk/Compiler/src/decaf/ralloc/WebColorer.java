package decaf.ralloc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.MethodIR;

public class WebColorer {
	public static int regCount = 3; //Register.availableRegs.length;
	private HashMap<String, MethodIR> mMap;
	private WebGenerator webGen;
	private Stack<Web> coloringStack;
	private HashMap<Web, List<Web>> adjacencyList;
	private List<Web> splitList;
	private List<Web> graph;
	private WebSplitter splitter;
	
	public WebColorer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.webGen = new WebGenerator(this.mMap);
		this.coloringStack = new Stack<Web>();	
		this.adjacencyList = new HashMap<Web, List<Web>>();
		this.splitList = new ArrayList<Web>();
		this.graph = new ArrayList<Web>();
		this.splitter = new WebSplitter(mMap, this.webGen.getDefAnalyzer(), this.webGen.getLivenessAnalyzer());
	}
	
	public void colorWebs() {
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			// Make graph colorable (if not already)
			int i = 0;
			while (i < 1) {
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
				i++;
			}
			
			// Color graph
			System.out.println("COLORING COMPLETED SUCCESSFULLY! \n");
		}
	}

	private void splitWebs(String methodName) {
		this.splitter.setState(methodName, splitList);
		this.splitter.split();
		this.mMap.get(methodName).regenerateStmts();
	}

	private boolean tryColoring(String methodName) {
		System.out.println("COLORING...");
		this.splitList.clear();
		makeGraph(methodName);
		
		while (true) {
			Web webToRemove = null;
			
			for (Web w: this.graph) {
				if (w.getInterferingWebs().size() < regCount) {
					System.out.println("CAN COLOR: " + w.getIdentifier());
					webToRemove = w;
					break;
				}
			}
			
			if (webToRemove != null) { // Found web to remove
				this.saveAdjacencyList(webToRemove);
				this.removeFromGraph(webToRemove);
				this.coloringStack.push(webToRemove);
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
