package decaf.codegen.flattener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import decaf.codegen.flatir.DataStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.MethodDecl;

public class CodeGenerator {
	private ProgramFlattener pf;
	private FileWriter out;
	private ClassDecl cd;
	
	public CodeGenerator(ProgramFlattener pf, ClassDecl cd, File file) throws IOException {
        this.out = new FileWriter(file);
		this.pf = pf;
		this.cd = cd;
	}
	
	public void generateCode() throws IOException {
		out.write(".data");
		for (DataStmt s: pf.getDataStmtList()) {
			s.generateAssembly(out);
		}
		
		out.write("");
		out.write(".text");
		for (MethodDecl md: cd.getMethodDeclarations()) {
			out.write("");
			List<LIRStatement> lirList = pf.getLirMap().get(md.getId());
			if (md.getId().equals("main")) {
				out.write("\t.globl main");
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
		out.close();
	}
}
