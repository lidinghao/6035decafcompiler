package decaf.dataflow.global;

import java.util.BitSet;

public class BlockDataFlowState {
	private BitSet in;
	private BitSet out;
	private BitSet kill;
	private BitSet gen;

	public BlockDataFlowState(int bitSetSize) {
		in = new BitSet(bitSetSize);
		out = new BitSet(bitSetSize);
		kill = new BitSet(bitSetSize);
		gen = new BitSet(bitSetSize);
	}
	
	public BitSet getIn() {
		return in;
	}

	public void setIn(BitSet in) {
		this.in = in;
	}

	public BitSet getOut() {
		return out;
	}

	public void setOut(BitSet out) {
		this.out = out;
	}

	public BitSet getKill() {
		return kill;
	}

	public void setKill(BitSet kill) {
		this.kill = kill;
	}

	public BitSet getGen() {
		return gen;
	}

	public void setGen(BitSet gen) {
		this.gen = gen;
	}
	
	@Override
	public String toString() {
		String o = "IN : " + in.toString() + "\n";
		o += "KILL : " + kill.toString() + "\n";
		o += "GEN : " + gen.toString() + "\n";
		o += "OUT : " + out.toString();
		
		return o;
	}
}
