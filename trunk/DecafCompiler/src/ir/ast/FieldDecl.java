package ir.ast;

import java.util.ArrayList;

public class FieldDecl extends MemberDecl {
	private ArrayList<Field> fields;
	
	public FieldDecl() {
		fields = new ArrayList<Field>();
	}
	
	public FieldDecl(Field f) {
		fields = new ArrayList<Field>();
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
}
