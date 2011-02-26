package decaf.test;

public class Error {
	int lineNumber;
	int columnNumber;
	String description;
	
	public Error() {
		lineNumber = -1;
		columnNumber = -1;
		description = "Unspecified Error";
	}
	
	public Error(int ln, int cn, String desc) {
		lineNumber = ln + 1; // Remove zero index offset
		columnNumber = cn + 1;
		description = desc;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber + 1;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	public void setColumnNumber(int columnNumber) {
		this.columnNumber = columnNumber + 1;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return "[" + lineNumber + ", " + columnNumber + "]" + " " + description;
	}
}
