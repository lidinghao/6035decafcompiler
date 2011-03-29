package decaf.codegen.flattener;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.ConstantName;
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
	public static String arrayExceptionErrorLabel = "outofbounds";
	public static String arrayExceptionMessage = "\"RUNTIME ERROR: Array index out of bounds (%d, %d)\\n\"";
	public static String methodExceptionErrorLabel = "methodcfend";
	public static String methodExceptionMessage = "\"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\\n\"";
	public static String exceptionHandlerLabel = "exception_handler";
	
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

	public void flatten() throws Exception {
		addExceptionHandlers();
		
		for (FieldDecl fd : classDecl.getFieldDeclarations()) {
			processFieldDecl(fd);
		}

		for (MethodDecl md : classDecl.getMethodDeclarations()) {
			processMethodDecl(md);
		}
	}

	private void addExceptionHandlers() {
		// Add array out of bound exception message string
		this.dataStmtList.add(new DataStmt(
				ProgramFlattener.arrayExceptionErrorLabel,
				ProgramFlattener.arrayExceptionMessage));
		
		// Add method cf end exception message string
		this.dataStmtList.add(new DataStmt(
				ProgramFlattener.methodExceptionErrorLabel,
				ProgramFlattener.methodExceptionMessage));

		// Add handler method
		while (isMethodName(ProgramFlattener.exceptionHandlerLabel)) {
			ProgramFlattener.exceptionHandlerLabel += "_"; // Add '_' to make
			// unique
		}

		List<LIRStatement> instructions = new ArrayList<LIRStatement>();

		// Add label
		LabelStmt l = new LabelStmt(ProgramFlattener.exceptionHandlerLabel);
		l.setMethodLabel(true);
		instructions.add(l);

		// Set %rax to 0
		instructions.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(
				Register.RAX), new ConstantName(0), null));

		instructions.add(new CallStmt("printf")); // Argument regs already contain
		// the right stuff

		// Invoke syscall 1
		instructions.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(
				Register.RAX), new ConstantName(1), null));

		// Exit with non-zero code
		instructions.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(
				Register.RBX), new ConstantName(1), null));

		// Call interrupt handler
		instructions.add(new InterruptStmt("$0x80"));

		this.lirMap
				.put(ProgramFlattener.exceptionHandlerLabel, instructions);
	}

	private boolean isMethodName(String label) {
		for (MethodDecl md : classDecl.getMethodDeclarations()) {
			if (md.getId().equals(label)) {
				return true;
			}
		}

		return false;
	}

	private void processMethodDecl(MethodDecl md) throws Exception {
		this.mfv.setMethodName(md.getId());

		int stackSize = md.accept(this.mfv);
		stackSize += tni.indexTemps(this.mfv.getStatements());
		stackSize += Math.min(md.getParameters().size(), 6); // Also save register
		// arguments on stack

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

	public void print(PrintStream out) {
		for (DataStmt ds : dataStmtList) {
			out.println(ds);
		}
		for (Entry<String, List<LIRStatement>> entry : lirMap.entrySet()) {
			for (LIRStatement s : entry.getValue()) {
				if (!s.getClass().equals(LabelStmt.class)) {
					out.println("\t" + s);
				} else {
					out.println(s);
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