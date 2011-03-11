package decaf.codegen.cfg;

import java.util.ArrayList;
import java.util.List;

public class BasicBlockNode extends CFG {
	private List<LIRStatement> statements;
	
	public BasicBlockNode() {
		this.statements = new ArrayList<LIRStatement>();
	}

	public List<LIRStatement> getStatements() {
		return statements;
	}

	public void setStatements(List<LIRStatement> statements) {
		this.statements = statements;
	}
	
	public void AddStatement(LIRStatement statement) {
		this.statements.add(statement);
	}
}
