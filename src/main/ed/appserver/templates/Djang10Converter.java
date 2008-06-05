// Djang10Converter.java

package ed.appserver.templates;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import ed.appserver.templates.djang10.JSHelper;
import ed.appserver.templates.djang10.Node;
import ed.appserver.templates.djang10.Parser;
import ed.appserver.templates.djang10.TemplateException;
import ed.appserver.templates.djang10.filters.DateFilter;
import ed.appserver.templates.djang10.filters.DefaultFilter;
import ed.appserver.templates.djang10.filters.DictSortFilter;
import ed.appserver.templates.djang10.filters.EscapeFilter;
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
import ed.appserver.templates.djang10.tagHandlers.VariableTagHandler;
import ed.js.engine.Scope;

/**
 * Handles the conversion of Djang10 templates to javascript
 *
 */
public class Djang10Converter implements TemplateConverter {
	public static final String extension = "djang10";
	public Djang10Converter(){

    }

	/**
	 * Parses the djang10 template into Nodes and collects the javascript generated by the nodes into the Result.
	 * The javascript files expected to be invoked with either 0, 1 or 2 parameters.  The first parameter is the Context 
	 * and is left unmodified upon successful rendering of the generated template, if the Context is null then the 
	 * calling scope is used as the context.  The 2nd parameter contains state variables which are necessary for
	 * the generated templates to call each other.
	 */
         public Result convert(Template t , ed.util.DependencyTracker tracker ) {
		if(!(extension).equals( t._extension))
			return null;
		
		Parser parser = new Parser(t._content);
		LinkedList<Node> nodeList;
		try {
			nodeList = parser.parse();
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}
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
			try {
                node.getRenderJSFn(preamble, writer);
            } catch (TemplateException e) {
                throw new RuntimeException(e);
            }
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
	
	
	/**
	 * Injects native helpers into the global scope
	 * @param scope
	 */
    public static void injectHelpers(Scope scope) {
    	scope.set(JSHelper.NS, new JSHelper());
    }
    

    public static TagHandler getVariableTagHandler() {
    	return _variableTagHandler;
    }
    public static Map<String, TagHandler> getTagHandlers() {
    	return _tagHandlers;
    }
    public static final VariableTagHandler _variableTagHandler;
    private static HashMap<String, TagHandler> _tagHandlers = new HashMap<String, TagHandler>();
    static {
    	_variableTagHandler = new VariableTagHandler();
    	
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
    private static final Map<String, Filter> _filters = new HashMap<String, Filter>();
    static {
    	_filters.put("default", new DefaultFilter());
    	_filters.put("urlencode", new UrlEncodeFilter());
    	_filters.put("escape", new EscapeFilter());
    	_filters.put("date", new DateFilter());
    	_filters.put("upper", new UpperFilter());
    	_filters.put("lower", new LowerFilter());
    	_filters.put("dictsort", new DictSortFilter(false));
    	_filters.put("dictsortreverse", new DictSortFilter(true));
    	_filters.put("length_is", new LengthIsFilter());
    	_filters.put("length", new LengthFilter());
    }
}
