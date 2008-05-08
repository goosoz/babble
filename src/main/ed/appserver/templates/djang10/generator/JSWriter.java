package ed.appserver.templates.djang10.generator;

import java.util.HashMap;
import java.util.Map;

import ed.appserver.templates.djang10.Util;


public class JSWriter {
	public static final String CONTEXT_STACK_VAR = "obj";
	public static final String RENDER_OPTIONS_VAR = "renderOpts";
	public static final String NS = "_djang10Helper";
	public static final String VAR_EXPAND = "djangoVarExpand";
	public static final String CALL_PATH = "callPath";
	
	private final StringBuilder buffer;
	private final Map<Integer, Integer> lineMap;
	private int currentLine;
	
	public JSWriter() {
		buffer = new StringBuilder();
		lineMap = new HashMap<Integer, Integer>();
		currentLine = 1;
	}
	
	public void append(String code) {
		currentLine += Util.countOccurance(code, '\n');
		
		buffer.append(code);
	}
	
	public void append(int srcLine, String code) {
		int startOutput = currentLine;
		
		append(code);
		
		int endOutput = currentLine + (code.endsWith("\n")? 0 : 1);
		
		for(int i=startOutput; i<endOutput; i++)
			lineMap.put(i, srcLine);
	}
	
	public void appendHelper(int srcLine, String name) {
		append(srcLine, NS + "." + name);
	}
	
	public void appendVarExpansion(int srcLine, String varName, String defaultValue) {
		appendHelper(srcLine, VAR_EXPAND);
		append("(\"");
		append(varName);
		append("\",");
		append(defaultValue);
		append(")");
	}
	public void appendCurrentContextVar(int srcLine, String name) {
		append(srcLine, CONTEXT_STACK_VAR);
		append(srcLine, "[");
		append(srcLine, CONTEXT_STACK_VAR);
		append(srcLine, ".length - 1].");
		append(name);
	}
	public void appendPopContext(int srcLine) {
		append(srcLine, CONTEXT_STACK_VAR);
		append(srcLine, ".pop();\n");
	}
	public Map<Integer, Integer> getLineMap() {
		return lineMap;
	}
	
	@Override
	public String toString() {
		return buffer.toString();
	}
}
