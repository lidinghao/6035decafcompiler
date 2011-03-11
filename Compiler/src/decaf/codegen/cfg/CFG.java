package decaf.codegen.cfg;

public abstract class CFG {
	private CFG beginNode;
	private CFG endNode;
	
	public CFG getBeginNode() {
		return beginNode;
	}
	
	public void setBeginNode(CFG beginNode) {
		this.beginNode = beginNode;
	}
	
	public CFG getEndNode() {
		return endNode;
	}
	
	public void setEndNode(CFG endNode) {
		this.endNode = endNode;
	}
}
