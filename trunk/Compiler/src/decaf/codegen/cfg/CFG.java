package codegen.cfg;

public abstract class CFG {
	private CFG nextNode;
	private CFG beginNode;
	private CFG endNode;
	
	public CFG getNextNode() {
		return nextNode;
	}
	
	public void setNextNode(CFG nextNode) {
		this.nextNode = nextNode;
	}
	
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
