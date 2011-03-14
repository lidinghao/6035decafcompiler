package decaf.codegen.flattener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.Constant;
import decaf.codegen.flatir.DataStmt;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.InterruptStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.Field;
import decaf.ir.ast.FieldDecl;
import decaf.ir.ast.IntLiteral;
import decaf.ir.ast.MethodDecl;

public class ProgramFlattener {
	public static String exceptionErrorLabel = "outofbounds";
	public static String exceptionMessage = "\"Array index out of bounds (%d, %d)\\n\"";
	public static String exceptionHandlerLabel = "arrayexception";
	private ClassDecl classDecl;
	private MethodFlatennerVisitor mfv;
	private HashMap<String, List<LIRStatement>> lirMap;
	private List<DataStmt> dataStmtList;
	private TempNameIndexer tni;

	public ProgramFlattener(ClassDecl cd) {
		this.classDecl = cd;
		this.mfv = new MethodFlatennerVisitor(null);
		this.lirMap = new HashMap<String, List<LIRStatement>>();
		this.dataStmtList = new ArrayList<DataStmt>();
		this.tni = new TempNameIndexer();
	}

	public void flatten() {
		for (FieldDecl fd : classDecl.getFieldDeclarations()) {
			processFieldDecl(fd);
		}

		for (MethodDecl md : classDecl.getMethodDeclarations()) {
			processMethodDecl(md);
		}

		// Add array out of bounds handler
		addExceptionHandler();
	}

	private void addExceptionHandler() {
		// Add array out of bound exception message string
		this.dataStmtList.add(new DataStmt(ProgramFlattener.exceptionErrorLabel,
				ProgramFlattener.exceptionMessage));
		
		// Add handler method
		while (isMethodName(ProgramFlattener.exceptionHandlerLabel)) {
			ProgramFlattener.exceptionHandlerLabel += "_"; // Add '_' to make unique
		}

		List<LIRStatement> instructions = new ArrayList<LIRStatement>();

		// Add label
		LabelStmt l = new LabelStmt(ProgramFlattener.exceptionHandlerLabel);
		l.setMethodLabel(true);
		instructions.add(l);		
		
		// Set %rax to 0
		instructions.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(
				Register.RAX), new Constant(0), null));
		
		instructions.add(new CallStmt("printf")); // Argument regs already contain the right stuff
		
		// Invote syscall 1
		instructions.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(
				Register.RAX), new Constant(1), null)); 
		
		// Exit with non-zero code
		instructions.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(
				Register.RBX), new Constant(1), null));
		
		// Call interrupt handler
		instructions.add(new InterruptStmt("$0x80"));
		
		this.lirMap.put(ProgramFlattener.exceptionHandlerLabel, instructions);
	}

	private boolean isMethodName(String label) {
		for (MethodDecl md : classDecl.getMethodDeclarations()) {
			if (md.getId().equals(label)) {
				return true;
			}
		}

		return false;
	}

	private void processMethodDecl(MethodDecl md) {
		this.mfv.setMethodName(md.getId());
		int stackSize = md.accept(this.mfv);
		stackSize += tni.indexTemps(this.mfv.getStatements());
		lirMap.put(md.getId(), this.mfv.getStatements());

		// Set stack size in 'enter' statement
		for (LIRStatement stmt : this.mfv.getStatements()) {
			if (stmt.getClass().equals(EnterStmt.class)) {
				((EnterStmt) stmt).setStackSize(stackSize);
			}
		}
	}

	private void processFieldDecl(FieldDecl fd) {
		for (Field f : fd.getFields()) {
			DataStmt ds;
			IntLiteral arrLen = f.getArrayLength();
			if (arrLen != null) {
				ds = new DataStmt(f.getId(), arrLen.getValue());
			} else {
				ds = new DataStmt(f.getId());
			}

			this.dataStmtList.add(ds);
		}
	}

	public HashMap<String, List<LIRStatement>> getLirMap() {
		return lirMap;
	}

	public void print() {
		for (DataStmt ds : dataStmtList) {
			System.out.println(ds);
		}
		for (Entry<String, List<LIRStatement>> entry : lirMap.entrySet()) {
			for (LIRStatement s : entry.getValue()) {
				if (!s.getClass().equals(LabelStmt.class)) {
					System.out.println("\t" + s);
				} else {
					System.out.println(s);
				}
			}
		}
	}

	public List<DataStmt> getDataStmtList() {
		return dataStmtList;
	}

	public void setDataStmtList(List<DataStmt> dataStmtList) {
		this.dataStmtList = dataStmtList;
	}
}