package decaf;

import java.io.*;
import java.util.HashMap;

import decaf.codegen.flattener.CodeGenerator;
import decaf.codegen.flattener.LocationResolver;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.block.BlockOptimizer;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.CFGBuilder;
import decaf.dataflow.cfg.CFGDataflowOptimizer;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BoundCheckCSEOptimizer;
import decaf.dataflow.global.BoundCheckDFAnalyzer;
import decaf.dataflow.global.GlobalCSEOptimizer;
import decaf.dataflow.global.GlobalOptimizer;
import decaf.dataflow.global.LoopOptimizer;
import decaf.ir.ast.ClassDecl;
import decaf.ir.semcheck.*;
import decaf.ralloc.ExplicitGlobalLoadOptimizer;
import decaf.ralloc.ExplicitGlobalLoader;
import decaf.ralloc.GlobalsDefDFAnalyzer;
import decaf.ralloc.Web;
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
				CFGBuilder cb = new CFGBuilder(pf.getLirMap());
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
					System.out.println();
					cb.printCFG(System.out);
				} 
				
				// Resolve names to locations (and sets stack size)
				LocationResolver lr = new LocationResolver(pf, cd);
				lr.resolveLocations();
				
				// Merge bound checks in CFG
				cb.setMergeBoundChecks(true);
				cb.generateCFGs();
				mMap = MethodIR.generateMethodIRs(pf, cb);
				
				ExplicitGlobalLoader gl = new ExplicitGlobalLoader(mMap);
				gl.execute();
				
				ExplicitGlobalLoadOptimizer glo = new ExplicitGlobalLoadOptimizer(mMap);
				glo.execute();
				
				GlobalsDefDFAnalyzer gDef = glo.getDf();
				gDef.analyze();
				System.out.println(gDef.getUniqueGlobals().get("main"));
				for (CFGBlock blk: cb.getCfgMap().get("main")) {
					if (gDef.getCfgBlocksState().containsKey(blk)) {
						System.out.println(blk);
						System.out.println(gDef.getCfgBlocksState().get(blk));
						System.out.println();
					}
				}
				
				
				BoundCheckCSEOptimizer bc = new BoundCheckCSEOptimizer(mMap);
				bc.performCSE();
				System.out.println(bc.getBCAnalyzer().getUniqueIndices().get("main"));
				for (CFGBlock blk: cb.getCfgMap().get("main")) {
					if (bc.getBCAnalyzer().getCfgBlocksState().containsKey(blk)) {
						System.out.println(blk);
						System.out.println(bc.getBCAnalyzer().getCfgBlocksState().get(blk));
						System.out.println();
					}
				}
				
				
				System.out.println();
				LoopOptimizer loopOptimizer = new LoopOptimizer(mMap);
				loopOptimizer.performLoopOptimization();
				System.out.println();
				
				//cb.printCFG(System.out);
				
//				WebGenerator wg = new WebGenerator(mMap);
//				wg.generateWebs();
//				for (Web w: wg.getWebMap().get("main")) {
//					System.out.println(w + "\n");
//				}
				
				if (CLI.debug) {
					System.out.println("Name -> Locations Mapping:");
					lr.printLocations(System.out);
				}
				
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
