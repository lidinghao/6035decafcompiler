package decaf.ir.desc;

import java.util.ArrayList;

import decaf.ir.LocalSymbolTable;
import decaf.ir.ast.Type;
import decaf.ir.ast.Parameter;

public class MethodDescriptor extends Descriptor {
	private Type returnType;
	private ArrayList<Parameter> parameters;
	private LocalSymbolTable localSymbolTable;
	
	public MethodDescriptor(Type returnType, ArrayList<Parameter> parameters, LocalSymbolTable localSymbolTable) {
		this.setReturnType(returnType);
		this.setParameters(parameters);
		this.setLocalSymbolTable(localSymbolTable);
		
	}

	public void setReturnType(Type returnType) {
		this.returnType = returnType;
	}

	public Type getReturnType() {
		return returnType;
	}

	public void setParameters(ArrayList<Parameter> parameters) {
		this.parameters = parameters;
	}

	public ArrayList<Parameter> getParameters() {
		return parameters;
	}

	public void setLocalSymbolTable(LocalSymbolTable localSymbolTable) {
		this.localSymbolTable = localSymbolTable;
	}

	public LocalSymbolTable getLocalSymbolTable() {
		return localSymbolTable;
	}
	
	
	

}
