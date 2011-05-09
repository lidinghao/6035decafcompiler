package decaf.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.MethodIR;

public class WebColorer {
	private static int regCount = Register.availableRegs.length;
	private HashMap<String, MethodIR> mMap;
	private WebGenerator webGen;
	private Stack<Web> coloringStack;
	private HashMap<Web, List<Web>> adjacencyList;
	private List<Web> splitList;
	private List<Web> graph;
	
	public WebColorer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.webGen = new WebGenerator(this.mMap);
		this.coloringStack = new Stack<Web>();	
		this.adjacencyList = new HashMap<Web, List<Web>>();
		this.splitList = new ArrayList<Web>();
		this.graph = new ArrayList<Web>();
	}
	
	public void colorWebs() {
		for (String methodName: this.webGen.getWebMap().keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			while (true) {
				this.webGen.generateWebs();
				
				if (tryColoring(methodName)) {
					break;
				}
				
				splitWebs(methodName);
			}
		}
	}

	private void splitWebs(String methodName) {
		// Select web to split
		// Split at right point
	}

	private boolean tryColoring(String methodName) {
		this.splitList.clear();
		makeGraph(methodName);
		
		while (true) {
			Web webToRemove = null;
			
			for (Web w: this.graph) {
				if (w.getInterferingWebs().size() < regCount) {
					webToRemove = w;
					break;
				}
			}
			
			if (webToRemove != null) { // Found web to remove
				this.saveAdjacencyList(webToRemove);
				this.removeFromGraph(webToRemove);
				this.coloringStack.push(webToRemove);
			}
			else { // Could not remove
				if (this.graph.isEmpty()) { // Graph empty, done coloring
					return true;
				}
				
				this.splitList.addAll(this.graph); // Split one of the nodes left in graph
				
				return false;
			}
		}
	}
	
	private void makeGraph(String methodName) {
		this.graph.clear();
		
		List<Web> notAdded = new ArrayList<Web>();
		
		for (Web w: this.webGen.getWebMap().get(methodName)) {
			if (w.getDefinitions().size() == 1) {
				if (w.getUses().isEmpty()) { // Store directly (def must be Quadruplet)
					notAdded.add(w);
					continue;
				}
				
				if (w.getDefinitions().get(0).getClass().equals(LoadStmt.class)) { // Single use, load directly
					if (w.getUses().size() == 1) {
						notAdded.add(w);
						continue;
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
}
