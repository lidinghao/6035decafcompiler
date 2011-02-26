package decaf.ir.ast;

import java.util.ArrayList;
import java.util.List;
import decaf.ir.ASTVisitor;

public class FieldDecl extends MemberDecl {
	private List<Field> fields;
	private Type type;
	
	public FieldDecl() {
		fields = new ArrayList<Field>();
	}
	
	public FieldDecl(Field f, Type t) {
		fields = new ArrayList<Field>();
		type = t;
		addField(f);
	}
	
	public FieldDecl(List<Field> f, Type t) {
		if (f != null)
			fields = f;
		else
			fields = new ArrayList<Field>();
		
		type = t;
	}
	
	public void addField(Field f) {
		fields.add(f);
	}
	
	public void setFields(ArrayList<Field> f) {
		fields = f;
	}
	
	public List<Field> getFields() {
		return fields;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		String rtn = type.toString() + " ";
		for (Field f: fields) {
			rtn += f.toString() + ", ";
		}
		
		if (fields.size() > 0) {
			rtn = rtn.substring(0, rtn.length() - 2);
		}
		
		return rtn;
	}

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		// TODO Auto-generated method stub
		return null;
	}
}
