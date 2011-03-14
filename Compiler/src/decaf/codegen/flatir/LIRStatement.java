package decaf.codegen.flatir;

import java.io.FileWriter;
import java.io.IOException;


public abstract class LIRStatement {
	public abstract void generateAssembly(FileWriter out) throws IOException;
}
