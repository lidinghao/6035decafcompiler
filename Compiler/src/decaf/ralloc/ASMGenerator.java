package decaf.ralloc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.DataStmt;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LeaveStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;

public class ASMGenerator {
	private static String CallBeginRegex = "[a-zA-z_]\\w*.mcall.[a-zA-z_]\\w*.\\d+.begin";
	private static String CallEndRegex = "[a-zA-z_]\\w*.mcall.[a-zA-z_]\\w*.\\d+.end";
	private ProgramFlattener pf;
	private WebColorer wc;
	private PrintStream out;
	private List<Register> callee;
	private List<Name> liveAtCaller;

	public ASMGenerator(ProgramFlattener pf, WebColorer wc, PrintStream out) {
		this.pf = pf;
		this.out = out;
		this.wc = wc;
		this.callee = new ArrayList<Register>();
		this.liveAtCaller = new ArrayList<Name>();
	}
	
	public ASMGenerator(ProgramFlattener pf, WebColorer wc, String filename) throws FileNotFoundException {
		File f = new File(filename);
		this.out = new PrintStream(f);
		this.pf = pf;
		this.wc = wc;
		this.callee = new ArrayList<Register>();
		this.liveAtCaller = new ArrayList<Name>();
	}

	public void generateAssembly() {
		out.println(".data");
		for (DataStmt s : pf.getDataStmtList()) {
			s.generateRegAllocAssembly(out);
		}

		out.println();
		out.println(".text");
		for (String methodName : pf.getLirMap().keySet()) {
			generateCalleeSave(methodName);

			out.println();
			List<LIRStatement> lirList = pf.getLirMap().get(methodName);
			if (methodName.equals("main")) {
				out.println("\t.globl main");
			}

			for (int i = 0; i < lirList.size(); i++) {
				LIRStatement s = lirList.get(i);

				// Caller save handling
				if (s.getClass().equals(LabelStmt.class)) {
					LabelStmt lStmt = (LabelStmt) s;
					if (lStmt.getLabelString().matches(CallBeginRegex)) {
						i = processMethodCall(methodName, i);
					}
					else {
						s.generateRegAllocAssembly(out);
					}
				}

				// Callee save handling
				else if (s.getClass().equals(EnterStmt.class)) {
					s.generateRegAllocAssembly(out);
					saveeCallee();
				} else if (s.getClass().equals(LeaveStmt.class)) {
					restoreCallee();
					s.generateRegAllocAssembly(out);
				}

				else {
					s.generateRegAllocAssembly(out);
				}
			}
		}

		// generateExceptionHanlder();
	}

	// Start label of mcall NOT already printed
	private int processMethodCall(String methodName, int i) {
		int callIndex = i;
		LIRStatement s = this.pf.getLirMap().get(methodName).get(callIndex);

		while (!s.getClass().equals(CallStmt.class)) {
			callIndex++;
			s = this.pf.getLirMap().get(methodName).get(callIndex);
		}

		CallStmt call = (CallStmt) s;

		List<Web> liveWebs = call.getLiveWebs();
		generateCallerSave(liveWebs);

		List<Register> invalidated = new ArrayList<Register>();
		boolean passedCall = false;
		boolean restored = false;

		while (true) {
			s = this.pf.getLirMap().get(methodName).get(i);
			i++;
			if (s.getClass().equals(CallStmt.class)) {
				s.generateRegAllocAssembly(out);
				passedCall = true;
			}
			if (s.getClass().equals(LabelStmt.class)) {
				LabelStmt lStmt = (LabelStmt) s;
				if (lStmt.getLabelString().matches(CallEndRegex)) {
					if (!restored) {
						restoreCaller();
					}
					s.generateRegAllocAssembly(out);
					break;
				} else if (lStmt.getLabelString().matches(CallBeginRegex)) {
					s.generateRegAllocAssembly(out);
					saveCaller(); // Save caller chutiap
					continue;
				}
			} else if (s.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) s;
				if (qStmt.getDestination().getClass().equals(RegisterName.class)) {
					RegisterName r = (RegisterName) qStmt.getDestination();
					Name var = qStmt.getArg1();

					if (passedCall) { // CALLER RESTORE before %rax = <>
						restoreCaller();
						restored = true;
					} else { // Invalidating regs on the fly
						if (invalidated.contains(var.getRegister())) {
							out.println("\tmov\t" + this.getLocation(var) + ", "
									+ r.getRegister());
							continue;
						} else {
							invalidated.add(r.getMyRegister()); // Cant reuse r now
						}
					}
				}

				s.generateRegAllocAssembly(out);
			}
		}

		return i;
	}

	private String getLocation(Name var) {
		String to = "";

		if (var.isGlobal()) {
			if (var.isArray()) {
				ArrayName arr = (ArrayName) var;

				to = arr.getId() + "(, " + arr.getIndex().getRegister() + ", 8)"; // TODO: CAN FUCK UP?
			} else {
				VarName myVar = (VarName) var;
				if (myVar.isString()) {
					to = "$." + myVar.getId();
				} else {
					to = myVar.getId();
				}
			}
		} else {
			to = var.getLocation().getASMRepresentation();
		}

		return to;
	}

	private void saveCaller() {
		out.println("\t// caller save");

		for (Name var : this.liveAtCaller) {
			out.println("\tmov\t" + var.getRegister() + ", "
					+ getLocationForName(var, out, true));
		}
		out.println("\t//");
	}

	private void restoreCaller() {
		out.println("\t// caller restore");

		for (int i = this.liveAtCaller.size()-1; i >=0 ; i--) {
			Name var = this.liveAtCaller.get(i);
			out.println("\tmov\t" + getLocationForName(var, out, true) + ", " + var.getRegister());
		}
		out.println("\t//");
	}

	private void generateCallerSave(List<Web> liveWebs) {
		for (Web w : liveWebs) {
			if (w.getRegister() != null) {
				Register live = w.getRegister();
				for (Register save : Register.callerSaved) {
					if (live == save) {
						this.liveAtCaller.add(w.getVariable());
					}
				}
			}
		}
	}

	private void saveeCallee() {
		for (Register r : this.callee) {
			out.println("\tpush\t" + r);
		}
	}

	private void restoreCallee() {
		for (int i = this.callee.size() - 1; i >= 0; i--) {
			Register r = this.callee.get(i);
			out.println("\tpop\t" + r);
		}
	}

	private void generateCalleeSave(String methodName) {
		this.callee.clear();

		if (methodName.equals(ProgramFlattener.exceptionHandlerLabel))
			return;

		for (Register used : this.wc.getRegistersUsed().get(methodName)) {
			for (Register save : Register.calleeSaved) {
				if (used == save) {
					this.callee.add(used);
				}
			}
		}
	}
	
	public static String getLocationForName(Name name, PrintStream out, boolean skipRegister) {
		if (name.isGlobal() && !name.isArray()) {
			VarName var = (VarName) name;
			if (var.isString()) return var.getLocation().getASMRepresentation();
		}
		
		if (name.getMyRegister() != null && !skipRegister) {
			return name.getRegister();
		}
		
		if (name.isArray()) {
			return getArrayName((ArrayName)name, out);
		}
		
		return name.getLocation().getASMRepresentation();
	}

	private static String getArrayName(ArrayName name, PrintStream out) {
		if (name.getMyRegister() != null) return name.getRegister();
		
		if (name.getIndex().isArray()) {
			getArrayName((ArrayName)name.getIndex(), out);
		}
		
		if (name.getIndex().getMyRegister() != null) {
			name.setOffsetRegister(name.getIndex().getMyRegister());
			return name.getLocation().getASMRepresentation();
		}
		else {
			out.println("\tmov\t" + name.getIndex().getLocation().getASMRepresentation() + ", " + Register.RCX);
			name.setOffsetRegister(Register.RCX);
			return name.getLocation().getASMRepresentation();
		}
	}
}
