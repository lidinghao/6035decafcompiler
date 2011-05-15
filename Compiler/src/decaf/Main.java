package decaf;

import java.io.*;
import java.util.HashMap;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flattener.CodeGenerator;
import decaf.codegen.flattener.LocationResolver;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.block.BlockConsPropagationOptimizer;
import decaf.dataflow.block.BlockOptimizer;
import decaf.dataflow.block.BlockVarDCOptimizer;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.CFGBuilder;
import decaf.dataflow.cfg.CFGDataflowOptimizer;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.ConstReachingDef;
import decaf.dataflow.global.GlobalOptimizer;
import decaf.ir.ast.ClassDecl;
import decaf.ir.semcheck.*;
import decaf.memory.NaiveLoadAdder;
import decaf.optimize.ArrayAccessOptimizer;
import decaf.optimize.PostDataFlowOptimizer;
import decaf.optimize.StaticJumpEvaluator;
import decaf.ralloc.ASMGenerator;
import decaf.ralloc.LivenessAnalysis;
import decaf.ralloc.LocalLoadStoreDC;
import decaf.ralloc.Web;
import decaf.ralloc.WebColorer;
import decaf.ralloc.WebGenerator;
import decaf.test.Error;
import antlr.Token;
import java6035.tools.CLI.*;

;

class Main {
	public static void main(String[] args) {
		try {
			String[] optnames = {"all", "cse", "cp", "const", "dc" };
			CLI.parse(args, optnames);

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
				if (!SemanticChecker.performSemanticChecks(cd, System.out, false)) {
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
				if (!SemanticChecker.performSemanticChecks(cd, System.out, true)) { // Figure out opt flag
					System.exit(-1);
				}

				// Generate low-level ir
				ProgramFlattener pf = new ProgramFlattener(cd);
				pf.flatten();
				
				if (CLI.debug) {
					System.out.println("Low-level IR:");
					pf.printLIR(System.out);
					System.out.println();
				}
				
				// Generate CFGs for methods
				CFGBuilder cb = new CFGBuilder(pf);
				cb.generateCFGs();

				
				HashMap<String, MethodIR> mMap = MethodIR.generateMethodIRs(pf, cb);
				
				if (CLI.opts[0] || CLI.opts[1] || CLI.opts[2] || CLI.opts[3] || CLI.opts[4]) {
					if (CLI.debug) {
						cb.printCFG(System.out);
					}
					
					System.out.println("BEFORE CFG DATAFLOW OPTIMIZATIONS");
					pf.printLIR(System.out);
					System.out.println();
					cb.printCFG(System.out);
					System.out.println();
					
					BlockOptimizer bo = new BlockOptimizer(mMap);
					GlobalOptimizer go = new GlobalOptimizer(mMap);
					CFGDataflowOptimizer cfgdo = new CFGDataflowOptimizer(mMap, pf, bo, go, CLI.opts);
					cfgdo.optimizeCFGDataflow();
					
					System.out.println("AFTER CFG DATAFLOW OPTIMIZATIONS");
					pf.printLIR(System.out);
					
					PostDataFlowOptimizer pdfo = new PostDataFlowOptimizer(pf, cb);
					pdfo.optimize();
					
					System.out.println("AFTER POST DATAFLOW OPTIMIZATIONS");
					pf.printLIR(System.out);
					System.out.println();
					cb.printCFG(System.out);
				} 
				
				// Must gen after static jump eval
//				cb.setMergeBoundChecks(true);
//				pf.printLIR(System.out);
//				
//				
//				cb.generateCFGs();
//				mMap = MethodIR.generateMethodIRs(pf, cb);
//				
//				BlockConsPropagationOptimizer bco = new BlockConsPropagationOptimizer(mMap);
//				bco.performConsPropagation();
////				
//				System.out.println("INITIALIZING WEB COLORER");
//				WebColorer wc = new WebColorer(mMap);
//				wc.colorWebs();
//				
//				pf.printLIR(System.out);
//				
//				for (Web w: wc.getWebGen().getWebMap().get("foo")) {
//					System.out.println(w.getIdentifier() + " ==> " + w.getRegister());
//					System.out.println(w);
//				}
//				
				// Resolve names to locations (and sets stack size)
				LocationResolver lr = new LocationResolver(pf, cd);
				lr.resolveLocations();
				
				pf.printLIR(System.out);
				
//				lr.printLocations(System.out);
				
				if (CLI.debug) {
					System.out.println("Name -> Locations Mapping:");
					lr.printLocations(System.out);
				}
				
//				 Generate code to file
				CodeGenerator cg = new CodeGenerator(pf, cd, CLI.outfile);
				cg.generateCode();
//				
//				ConstReachingDef crd = new ConstReachingDef(mMap);
//				crd.analyze();
				
				//System.out.println("RALLOC ASM: \n");
//				
//				ASMGenerator asm = new ASMGenerator(pf, wc, CLI.outfile);
//				asm.generateAssembly();
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
