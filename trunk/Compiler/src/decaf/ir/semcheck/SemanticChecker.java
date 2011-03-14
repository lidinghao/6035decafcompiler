package decaf.ir.semcheck;

import java.io.PrintStream;
import java.util.List;
import java6035.tools.CLI.CLI;

import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.MethodDecl;
import decaf.test.PrettyPrintVisitor;
import decaf.test.Error;

public class SemanticChecker {

	public static boolean performSemanticChecks(ClassDecl cd, PrintStream out) {		
		// Check integer overflow (must do before symbol table generation)
		IntOverflowCheckVisitor ibv = new IntOverflowCheckVisitor();
		cd.accept(ibv);

		// Generate SymbolTables
		SymbolTableGenerationVisitor stv = new SymbolTableGenerationVisitor();
		cd.accept(stv);
		
		// Type checking and evaluation
		TypeEvaluationVisitor tev = new TypeEvaluationVisitor(
				stv.getClassDescriptor());
		cd.accept(tev);
		
		// Method calls and return statement type checking
		MethodCheckVisitor pmv = new MethodCheckVisitor(
				stv.getClassDescriptor());
		cd.accept(pmv);
		
		// Check main method
		Error mainMethodError = checkMainMethod(cd);
		
		// Break Continue check
		BreakContinueStmtCheckVisitor tc = new BreakContinueStmtCheckVisitor();
		cd.accept(tc);
		
		// Array Size check
		ArraySizeCheckVisitor av = new ArraySizeCheckVisitor();
		cd.accept(av);
		
		// Print errors if -debug on
		if (CLI.debug) {
			// Print AST
			out.println("AST:");
			PrettyPrintVisitor pv = new PrettyPrintVisitor();
			cd.accept(pv);
			
			// Print Symbol Tables
			out.println("Symbol Tables:");
			out.println(stv.getClassDescriptor());
			
			// Print errors for integer overflow
			out.println("Integer overflow check:");
			out.println(ibv.getErrors());
			
			// Print errors for symbol table generation
			out.println("Symbol table generation:");
			out.println(stv.getErrors());
			
			// Print type checking errors
			out.println("Type checking and evaluation:");
			out.println(tev.getErrors());
			
			// Print mehtod check errros
			out.println("Method argument and return type matching:");
			out.println(pmv.getErrors());
			
			// Print main method errors
			out.println("'main' method check:");
			if (mainMethodError != null) {
				out.println(mainMethodError);
			}
			
			// Print break/continue errors
			out.println("Break/continue statement check:");
			out.println(tc.getErrors());

			// Print size check errors
			out.println("Array size check:");
			out.println(av.getErrors());
		}
		
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
