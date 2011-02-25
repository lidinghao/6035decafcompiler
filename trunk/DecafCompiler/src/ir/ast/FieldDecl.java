package ir.ast;

import java.util.ArrayList;

public class FieldDecl extends MemberDecl {
	private ArrayList<Field> fields;
	private Type type;
	
	public FieldDecl() {
		fields = new ArrayList<Field>();
	}
	
	public FieldDecl(Field f, Type t) {
		fields = new ArrayList<Field>();
		type = t;
		addField(f);
	}
	
	public FieldDecl(ArrayList<Field> f) {
		if (f != null)
			fields = f;
		else
			fields = new ArrayList<Field>();
	}
	
	public void addField(Field f) {
		fields.add(f);
	}
	
	public void setFields(ArrayList<Field> f) {
		fields = f;
	}
	
	public ArrayList<Field> getFields() {
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
}
