package decaf.ir.semcheck;

import java.util.List;

import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.MethodDecl;
import decaf.test.PrettyPrintVisitor;
import decaf.test.Error;

public class SemanticChecker {

	public static void performSemanticChecks(ClassDecl cd) {
		PrettyPrintVisitor pv = new PrettyPrintVisitor();
		cd.accept(pv);

		// Check if main method with no params exists
		Error mainMethodError = checkMainMethod(cd);
		if (mainMethodError != null) {
			System.out.println(mainMethodError);
		}
		
		// Check integer overflow
		IntOverflowCheckVisitor ibv = new IntOverflowCheckVisitor();
		cd.accept(ibv);
		System.out.println(ibv.getErrors());

		//
		BreakContinueStmtCheckVisitor tc = new BreakContinueStmtCheckVisitor();
		cd.accept(tc);
		System.out.println(tc.getErrors());

		//
		ArraySizeCheckVisitor av = new ArraySizeCheckVisitor();
		cd.accept(av);
		System.out.println(av.getErrors());

		// Generate SymbolTables
		SymbolTableGenerationVisitor stv = new SymbolTableGenerationVisitor();
		cd.accept(stv);
		System.out.println(stv.getErrors());
	}
	
	// Checks whether the program contains a main method with no parameters
	private static Error checkMainMethod(ClassDecl cd) {
		List<MethodDecl> methodDecls = cd.getMethodDeclarations();
		for (MethodDecl md : methodDecls) {
			if (md.getId().equals("main")) {
				if (md.getParameters().size() == 0) {
					return null;
				}
			}
		}
		return new Error(cd.getLineNumber(), cd.getColumnNumber(),
				"Class does not contain 'main' method with no parameters.");
	}
}
