package decaf.ir.semcheck;

import java.io.PrintStream;
import java.util.List;

import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.MethodDecl;
import decaf.ir.desc.ClassDescriptor;
import decaf.test.PrettyPrintVisitor;
import decaf.test.Error;

public class SemanticChecker {

	public static boolean performSemanticChecks(ClassDecl cd, PrintStream out) {
		PrettyPrintVisitor pv = new PrettyPrintVisitor();
		cd.accept(pv);
		
		// Check integer overflow (must do before symbol table generation)
		IntOverflowCheckVisitor ibv = new IntOverflowCheckVisitor();
		cd.accept(ibv);

		// Generate SymbolTables
		SymbolTableGenerationVisitor stv = new SymbolTableGenerationVisitor();
		cd.accept(stv);
		
		// Print Symbol Tables
		out.println(stv.getClassDescriptor());
		
		// Print errors for integer overflow and symbol table generation
		out.println("Integer overflow check:");
		out.println(ibv.getErrors());
		out.println("Symbol table generation:");
		out.println(stv.getErrors());

		// Type checking and evaluation
		TypeEvaluationVisitor tev = new TypeEvaluationVisitor(
				stv.getClassDescriptor());
		cd.accept(tev);
		out.println("Type checking and evaluation:");
		out.println(tev.getErrors());

		// Method calls and return statement type checking
		MethodCheckVisitor pmv = new MethodCheckVisitor(
				stv.getClassDescriptor());
		cd.accept(pmv);
		out.println("Method argument and return type matching:");
		out.println(pmv.getErrors());

		// Check if main method with no params exists
		out.println("'main' method check:");
		Error mainMethodError = checkMainMethod(cd);
		if (mainMethodError != null) {
			out.println(mainMethodError);
		}

		// Break Continue check
		BreakContinueStmtCheckVisitor tc = new BreakContinueStmtCheckVisitor();
		cd.accept(tc);
		out.println("Break/continue statement check:");
		out.println(tc.getErrors());

		// Array Size check
		ArraySizeCheckVisitor av = new ArraySizeCheckVisitor();
		cd.accept(av);
		System.out.println("Array size check:");
		System.out.println(av.getErrors());
		
		if (ibv.getErrors().size() > 0 ||
				stv.getErrors().size() > 0 ||
				tev.getErrors().size() > 0 ||
				pmv.getErrors().size() > 0 ||
				tc.getErrors().size() > 0 ||
				av.getErrors().size() > 0 ||
				mainMethodError != null) {
			return false;
		}
		
		return true;
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
