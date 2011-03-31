package decaf;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flattener.CodeGenerator;
import decaf.codegen.flattener.LocationResolver;
import decaf.codegen.flattener.MethodFlatennerVisitor;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.codegen.flattener.TempNameIndexer;
import decaf.dataflow.block.BlockCSEOptimizer;
import decaf.dataflow.block.BlockCopyPropagationOptimizer;
import decaf.dataflow.block.BlockDeadCodeOptimizer;
import decaf.dataflow.block.BlockOptimizer;
import decaf.dataflow.cfg.CFGBuilder;
import decaf.dataflow.cfg.LeaderElector;
import decaf.dataflow.global.GlobalCSEOptimizer;
import decaf.ir.ast.ClassDecl;
import decaf.ir.semcheck.*;
import decaf.test.Error;
import antlr.Token;
import java6035.tools.CLI.*;

;

class Main {
	public static void main(String[] args) {
		try {
			CLI.parse(args, new String[0]);

			InputStream inputStream = args.length == 0 ? System.in
					: new java.io.FileInputStream(CLI.infile);

			if (CLI.target == CLI.SCAN) {
				DecafScanner lexer = new DecafScanner(new DataInputStream(
						inputStream));
				Token token;
				boolean done = false;
				while (!done) {
					try {
						for (token = lexer.nextToken(); token.getType() != DecafParserTokenTypes.EOF; token = lexer
								.nextToken()) {
							String type = "";
							String text = token.getText();

							switch (token.getType()) {
								case DecafScannerTokenTypes.ID:
									type = " IDENTIFIER";
									break;
								case DecafScannerTokenTypes.CHAR:
									type = " CHARLITERAL";
									break;
								case DecafScannerTokenTypes.STRING:
									type = " STRINGLITERAL";
									break;
								case DecafScannerTokenTypes.INTLIT:
									type = " INTLITERAL";
									break;
								case DecafScannerTokenTypes.TK_true:
								case DecafScannerTokenTypes.TK_false:
									type = " BOOLEANLITERAL";
									break;
							}
							System.out.println(token.getLine() + type + " " + text);
						}
						done = true;
					} catch (Exception e) {
						// print the error:
						System.out.println(CLI.infile + " " + e);
						lexer.consume();
					}
				}
			} else if (CLI.target == CLI.PARSE) {
				DecafScanner lexer = new DecafScanner(new DataInputStream(
						inputStream));
				DecafParser parser = new DecafParser(lexer);

				// Check if parse was successful
				if (parser.program() == null) {
					throw new Exception("Class name must be 'Program'");
				}
			} else if (CLI.target == CLI.INTER) {
				DecafScanner lexer = new DecafScanner(new DataInputStream(
						inputStream));
				DecafParser parser = new DecafParser(lexer);

				// Parse and generate AST
				ClassDecl cd = parser.program();

				// Check if parse was successful
				if (cd == null) {
					throw new Exception("Class name must be 'Program'");
				}

				// Set file name
				Error.fileName = getFileName(CLI.infile); 

				// Check for semantic errors
				if (!SemanticChecker.performSemanticChecks(cd, System.out)) {
					System.exit(-1);
				}
			}
			else if (CLI.target == CLI.ASSEMBLY || CLI.target == CLI.DEFAULT) {
				DecafScanner lexer = new DecafScanner(new DataInputStream(
						inputStream));
				DecafParser parser = new DecafParser(lexer);

				// Parse and generate AST
				ClassDecl cd = parser.program();

				// Check if parse was successful
				if (cd == null) {
					throw new Exception("Class name must be 'Program'");
				}

				// Set file name
				Error.fileName = getFileName(CLI.infile);

				// Check for semantic errors
				if (!SemanticChecker.performSemanticChecks(cd, System.out)) {
					System.exit(-1);
				}

				// Generate low-level ir
				ProgramFlattener pf = new ProgramFlattener(cd);
				pf.flatten();
				
				if (CLI.debug) {
					System.out.println("Low-level IR:");
					pf.print(System.out);
					System.out.println();
				}
				
				// Select leaders
				LeaderElector le = new LeaderElector(pf.getLirMap());
				le.electLeaders();
				
				// Generate CFGs for methods
				CFGBuilder cb = new CFGBuilder(pf.getLirMap());
				cb.generateCFGs();
				
				// Block optimizations
				
				BlockOptimizer bo = new BlockOptimizer(cb, pf);
				bo.optimizeBlocks();
				
				pf.print(System.out);
				
				// Global optimizations
				
				GlobalCSEOptimizer globalCSE = new GlobalCSEOptimizer(cb.getCfgMap(), pf);
				System.out.println("\nGLOBAL OPTIMIZATIONS: ");
				globalCSE.getAvailableGenerator().printBlocksAvailableExpressions(System.out);
				globalCSE.performGlobalCSE();
				globalCSE.printExprToTemp(System.out);
				System.out.println("\nAFTER GLOBAL CSE: ");
				pf.print(System.out);
				
				// Resolve names to locations
				LocationResolver lr = new LocationResolver(pf, cd);
				lr.resolveLocations();
				
				if (CLI.debug) {
					System.out.println("Name -> Locations Mapping:");
					lr.printLocations(System.out);
				}
				
				cb.printCFG(System.out);
				
				// Generate code to file
				CodeGenerator cg = new CodeGenerator(pf, cd, CLI.outfile);
				cg.generateCode();				
			}
		} catch (Exception e) {
			// print the error:
			System.out.println(CLI.infile + " " + e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static String getFileName(String name) {
		int slashIndex = -1;
		for (int i = name.length() - 1; i >= 0; i--) {
			if (name.charAt(i) == '/') {
				slashIndex = i;
				break;
			}
		}

		if (slashIndex != -1) {
			return name.substring(slashIndex + 1);
		}

		return name;
	}
}
