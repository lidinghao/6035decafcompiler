package decaf.ralloc;

import java.util.HashMap;

import decaf.codegen.flatir.LoadStmt;
import decaf.dataflow.cfg.MethodIR;

public class ExplicitGlobalLoadOptimizer {
	private static String ForTestLabelRegex = "[a-zA-z_]\\w*.for\\d+.test";
	private static String ForInitLabelRegex = "[a-zA-z_]\\w*.for\\d+.init";
	private static String IfEndLabelRegex = "[a-zA-z.]\\w*.if\\d+.end";
	private HashMap<String, MethodIR> mMap;
	private GlobalsDefDFAnalyzer df;
	private HashMap<LoadStmt, Boolean> loadOptimized;
	
	public ExplicitGlobalLoadOptimizer(HashMap<String, MethodIR> mMap) {
		this.df = new GlobalsDefDFAnalyzer(mMap);
		this.mMap = mMap;
		this.loadOptimized = new HashMap<LoadStmt, Boolean>();
	}
}
