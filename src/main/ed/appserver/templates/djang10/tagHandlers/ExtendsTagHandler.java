package ed.appserver.templates.djang10.tagHandlers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ed.appserver.templates.djang10.Node;
import ed.appserver.templates.djang10.Parser;
import ed.appserver.templates.djang10.Parser.Token;
import ed.appserver.templates.djang10.generator.JSWriter;
import ed.appserver.templates.djang10.tagHandlers.BlockTagHandler.BlockNode;
import ed.js.JSFunction;

public class ExtendsTagHandler implements TagHandler {

	public Node compile(Parser parser, String command, Token token) {
		String path = Parser.smartSplit(token.contents)[1];
		path = Parser.dequote(path);
		
		
		parser.setStateVariable(this.getClass(), true);
		
		List<Node> nodeList = parser.parse();
		List<Node> blockList = new LinkedList<Node>();
		
		for(Node node : nodeList) {
			if(node instanceof BlockNode)
				blockList.add(node);
		}
		
		return new ExtendsNode(token, path, blockList);
	}

	public Map<String, JSFunction> getHelpers() {
		return new HashMap<String, JSFunction>();
	}

	
	private static class ExtendsNode extends Node {
		private final String path;
		private final List<Node> topLevelBlocks;
		
		public ExtendsNode(Token token, String path, List<Node> topLevelBlocks) {
			super(token);
			this.path = path;
			this.topLevelBlocks = topLevelBlocks;
		}
		
		@Override
		public void getRenderJSFn(JSWriter buffer) {
			for(Node node : topLevelBlocks)
				node.getRenderJSFn(buffer);
			
			
			StringTokenizer tokenizer = new StringTokenizer(path, "/");

			boolean isFirst = true;
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.length() > 0) {
					if (!isFirst) {
						buffer.append("[\"");
						buffer.append(token);
						buffer.append("\"]");
					} else
						buffer.append(token);
				}
				isFirst = false;
			}
			buffer.append(startLine, "(");
			buffer.append(startLine, JSWriter.CONTEXT_STACK_VAR + ", " + JSWriter.RENDER_OPTIONS_VAR);
			buffer.append(startLine, ");\n");

		}
		
	}
}
