package decaf.ralloc;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import decaf.dataflow.cfg.MethodIR;

public class WebColorer {
	private HashMap<String, MethodIR> mMap;
	private WebGenerator webGen;
	private Stack<Web> webStack;
	private HashMap<Web, List<Web>> neighbors;
	
	public WebColorer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.webGen = new WebGenerator(this.mMap);
		this.webStack = new Stack<Web>();	
		this.neighbors = new HashMap<Web, List<Web>>();
	}
	
	public void colorWebs() {
		while (true) {
			this.webGen.generateWebs();
			
			if (tryColoring()) {
				break;
			}
			
			splitWebs();
		}
	}

	private void splitWebs() {
		// TODO Auto-generated method stub
		
	}

	private boolean tryColoring() {
		// TODO Auto-generated method stub
		return false;
	}
}
