package decaf.codegen.flatir;

import java.io.PrintStream;

public abstract class LIRStatement {
	public abstract void generateAssembly(PrintStream out);
}
