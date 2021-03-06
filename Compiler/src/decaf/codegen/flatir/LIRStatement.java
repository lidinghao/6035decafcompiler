package decaf.codegen.flatir;

import java.io.PrintStream;
import java.util.BitSet;

import decaf.codegen.flattener.MethodFlattenerVisitor;

public abstract class LIRStatement {
	protected boolean isLeader;
	protected int depth;
	protected boolean isDead;
	protected BitSet liveInSet;
	protected BitSet reachingDefInSet;
	
	public BitSet getReachingDefInSet() {
		return reachingDefInSet;
	}

	public void setReachingDefInSet(BitSet reachingDefInSet) {
		this.reachingDefInSet = reachingDefInSet;
	}

	public BitSet getLiveInSet() {
		return liveInSet;
	}

	public void setLiveInSet(BitSet inSet) {
		this.liveInSet = inSet;
	}

	public boolean isDead() {
		return isDead;
	}

	public void setDead(boolean isDead) {
		this.isDead = isDead;
	}

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
	
	public abstract void generateRegAllocAssembly(PrintStream out);
	
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
