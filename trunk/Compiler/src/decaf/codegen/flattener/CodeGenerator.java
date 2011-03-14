package decaf.codegen.flattener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import decaf.codegen.flatir.DataStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.MethodDecl;

public class CodeGenerator {
	private ProgramFlattener pf;
	private PrintStream out;
	private ClassDecl cd;
	
	public CodeGenerator(ProgramFlattener pf, ClassDecl cd, String filename) throws FileNotFoundException {
		File f = new File(filename);
		this.out = new PrintStream(f);
		this.pf = pf;
		this.cd = cd;
	}
	
	public void generateCode() {
		out.println(".data");
		for (DataStmt s: pf.getDataStmtList()) {
			s.generateAssembly(out);
		}
		
		out.println();
		out.println(".text");
		for (MethodDecl md: cd.getMethodDeclarations()) {
			out.println();
			List<LIRStatement> lirList = pf.getLirMap().get(md.getId());
			if (md.getId().equals("main")) {
				out.println("\t.globl main");
			}
			for (LIRStatement s: lirList) {
				s.generateAssembly(out);
			}
		}
		
		// Generate interrupt handler
		List<LIRStatement> interruptHandler = pf.getLirMap().get(ProgramFlattener.exceptionHandlerLabel);
		for (LIRStatement s: interruptHandler) {
			s.generateAssembly(out);
		}
	}
}
