// Djang10Converter.java

package ed.appserver.templates;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import ed.appserver.templates.djang10.Context;
import ed.appserver.templates.djang10.JSHelper;
import ed.appserver.templates.djang10.Node;
import ed.appserver.templates.djang10.Parser;
import ed.appserver.templates.djang10.Variable;
import ed.appserver.templates.djang10.filters.DateFilter;
import ed.appserver.templates.djang10.filters.DefaultFilter;
import ed.appserver.templates.djang10.filters.DictSortFilter;
import ed.appserver.templates.djang10.filters.Filter;
import ed.appserver.templates.djang10.filters.LengthFilter;
import ed.appserver.templates.djang10.filters.LengthIsFilter;
import ed.appserver.templates.djang10.filters.LowerFilter;
import ed.appserver.templates.djang10.filters.UpperFilter;
import ed.appserver.templates.djang10.filters.UrlEncodeFilter;
import ed.appserver.templates.djang10.generator.JSWriter;
import ed.appserver.templates.djang10.tagHandlers.BlockTagHandler;
import ed.appserver.templates.djang10.tagHandlers.CallTagHandler;
import ed.appserver.templates.djang10.tagHandlers.CommentTagHandler;
import ed.appserver.templates.djang10.tagHandlers.CycleTagHandler;
import ed.appserver.templates.djang10.tagHandlers.ExtendsTagHandler;
import ed.appserver.templates.djang10.tagHandlers.FilterTagHandler;
import ed.appserver.templates.djang10.tagHandlers.FirstOfTagHandler;
import ed.appserver.templates.djang10.tagHandlers.ForTagHandler;
import ed.appserver.templates.djang10.tagHandlers.IfEqualTagHandler;
import ed.appserver.templates.djang10.tagHandlers.IfTagHandler;
import ed.appserver.templates.djang10.tagHandlers.IncludeTagHandler;
import ed.appserver.templates.djang10.tagHandlers.SetTagHandler;
import ed.appserver.templates.djang10.tagHandlers.TagHandler;
import ed.js.JSFunction;
import ed.js.JSObject;
import ed.js.engine.Scope;


public class Djang10Converter implements TemplateConverter {
	public static final String extension = "djang10";
	public Djang10Converter(){

    }

	public Result convert(Template t) {
		if(!(extension).equals( t._extension))
			return null;
		
		Parser parser = new Parser(t._content);
		LinkedList<Node> nodeList = parser.parse();
		JSWriter preamble = new JSWriter();
		JSWriter writer = new JSWriter();
		
		//if no arguments were passed in use the calling scope for the context
		preamble.append("var "+JSWriter.CONTEXT_STACK_VAR+" = (arguments.length == 0 || arguments[0] == null)? scope : arguments[0];\n");

		//wrap the passed in object in a context,  if necessary
		preamble.append("if(!("+JSWriter.CONTEXT_STACK_VAR+" instanceof "+ JSHelper.NS + "." + JSHelper.CONTEXT_CLASS+"))\n");
		preamble.append(JSWriter.CONTEXT_STACK_VAR+" = new "+ JSHelper.NS + "." + JSHelper.CONTEXT_CLASS+"("+JSWriter.CONTEXT_STACK_VAR+");\n");

		preamble.append("var "+JSWriter.RENDER_OPTIONS_VAR+" = (arguments.length < 2)? {} : arguments[1];\n");
		preamble.append(JSWriter.CONTEXT_STACK_VAR + ".push();\n");
		
		for(Node node : nodeList) {
			node.getRenderJSFn(preamble, writer);
		}
		
		writer.append(1, JSWriter.CONTEXT_STACK_VAR + ".pop();\n");
		
		StringBuilder newTemplate = new StringBuilder(preamble.toString());
		Map<Integer, Integer> newTemplateLineMapping = new HashMap<Integer, Integer>(preamble.getLineMap());
		
		newTemplate.append(writer.toString());
		
		for(Entry<Integer, Integer> lineMapping : writer.getLineMap().entrySet()) {
			
			int newOffsetLine = lineMapping.getKey() + preamble.getLineCount();
			newTemplateLineMapping.put(newOffsetLine, lineMapping.getValue());
		}
		
		
		String newName = t.getName().replaceAll( "\\.("+extension+")+$" , "_$1.js" );
		return new Result(new Template(newName, newTemplate.toString(), t.getSourceLanguage()), newTemplateLineMapping);
	}
	
	
	public static Object resolveVariable(Scope scope, String unresolvedValue, boolean allowGlobal) {
		String varName = unresolvedValue;
		
		if(varName == null)
			return null;

		if(Parser.isQuoted(varName))
			return Parser.dequote(varName);
		
		try {
			return Integer.valueOf(varName).toString();
		} catch(Exception e) {}
		try {
			return Float.valueOf(varName).toString();
		} catch(Exception e) {}		
		
		String[] varNameParts = varName.split("\\.");
		
		// find the starting point
		Context contextStack = (Context)scope.get(JSWriter.CONTEXT_STACK_VAR);
		Object varValue = null;
		
		varValue = contextStack.get(varNameParts[0]);
		
		if(varValue == null && allowGlobal) {
			varValue = scope.get(varNameParts[0]);
		}
		
		if(varValue == null)
			return Variable.UNDEFINED_VALUE;
		
		// find the rest of the variable members
		for(int i=1; i<varNameParts.length; i++) {
			String varNamePart = varNameParts[i];

			if(varValue == null || !(varValue instanceof JSObject))
				return Variable.UNDEFINED_VALUE;
			
			JSObject varValueJsObj = (JSObject)varValue;
			
			if(!varValueJsObj.containsKey(varNamePart))
				return Variable.UNDEFINED_VALUE;

			varValue = varValueJsObj.get(varNamePart);
			
			if(varValue instanceof JSFunction)
				varValue = ((JSFunction)varValue).callAndSetThis(scope.child(), varValueJsObj, new Object[0]);
		}

		return varValue;
	}

	
	
    //Helpers
    public static void injectHelpers(Scope scope) {
    	scope.set(JSHelper.NS, new JSHelper());
    }
    
    //TagHandler Registration
    public static Map<String, TagHandler> getTagHandlers() {
    	return _tagHandlers;
    }
    private static HashMap<String, TagHandler> _tagHandlers = new HashMap<String, TagHandler>();
    static {
    	_tagHandlers.put("if", new IfTagHandler());
    	_tagHandlers.put("for", new ForTagHandler());
    	_tagHandlers.put("include", new IncludeTagHandler());
    	_tagHandlers.put("block", new BlockTagHandler());
    	_tagHandlers.put("extends", new ExtendsTagHandler());
    	_tagHandlers.put("ifequal", new IfEqualTagHandler(false));
    	_tagHandlers.put("ifnotequal", new IfEqualTagHandler(true));
    	_tagHandlers.put("comment", new CommentTagHandler());
    	_tagHandlers.put("filter", new FilterTagHandler());
    	_tagHandlers.put("cycle", new CycleTagHandler());
    	_tagHandlers.put("firstof", new FirstOfTagHandler());
    	_tagHandlers.put("set",  new SetTagHandler());
    	_tagHandlers.put("call", new CallTagHandler());
    }

    
    //Filter Registration
    public static Map<String, Filter> getFilters() {
    	return _filters;
    }
    private static Map<String, Filter> _filters = new HashMap<String, Filter>();
    static {
    	_filters.put("default", new DefaultFilter());
    	_filters.put("urlencode", new UrlEncodeFilter());
    	_filters.put("date", new DateFilter());
    	_filters.put("upper", new UpperFilter());
    	_filters.put("lower", new LowerFilter());
    	_filters.put("dictsort", new DictSortFilter(false));
    	_filters.put("dictsortreverse", new DictSortFilter(true));
    	_filters.put("length_is", new LengthIsFilter());
    	_filters.put("length", new LengthFilter());
    }
}
