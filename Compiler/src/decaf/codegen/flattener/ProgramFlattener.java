package decaf.codegen.flattener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import decaf.codegen.flatir.DataStmt;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.Field;
import decaf.ir.ast.FieldDecl;
import decaf.ir.ast.IntLiteral;
import decaf.ir.ast.MethodDecl;

public class ProgramFlattener {
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
		for (MethodDecl md : classDecl.getMethodDeclarations()) {
			processMethodDecl(md);
		}
		for (FieldDecl fd : classDecl.getFieldDeclarations()) {
			processFieldDecl(fd);
		}
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