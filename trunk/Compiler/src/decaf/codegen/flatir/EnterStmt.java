package decaf.codegen.flatir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

public class EnterStmt extends LIRStatement {
	private int stackSize;
	
	public EnterStmt(int stackSize) {
		this.setStackSize(stackSize);
	}
	
	public EnterStmt() {
		stackSize = 0;
	}

	public void setStackSize(int stackSize) {
		this.stackSize = stackSize;
	}

	public int getStackSize() {
		return stackSize;
	}
	
	@Override 
	public String toString() {
		String rtn = "enter ";
		if (stackSize == 0) {
			rtn += "<undefined>";
		}
		else {
			rtn += Integer.toString(stackSize);
		}
		
		return rtn;
	}

	@Override
	public void generateAssembly(FileWriter out) throws IOException {
		out.write("\tenter\t" + "$" + (this.stackSize * 8) + ", $0");		
	}
}
