package decaf.codegen.flatir;

import java.io.PrintStream;

import decaf.codegen.flattener.MethodFlattenerVisitor;

public abstract class LIRStatement {
	protected boolean isLeader;
	protected int depth;
	
	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	protected void setDepth() {
		this.depth = MethodFlattenerVisitor.DEPTH;
	}

	public abstract void generateAssembly(PrintStream out);
	
	public boolean isLeader() {
		return isLeader;
	}
	
	public void setIsLeader(boolean bool) {
		isLeader = bool;
	}

	public boolean isUseStatement() {
		return false;
	}
	
	public boolean isAvailableExpression() {
		return false;
	}
	
	public abstract Object clone();
}
