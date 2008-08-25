/**
*    Copyright (C) 2008 10gen Inc.
*
*    This program is free software: you can redistribute it and/or  modify
*    it under the terms of the GNU Affero General Public License, version 3,
*    as published by the Free Software Foundation.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU Affero General Public License for more details.
*
*    You should have received a copy of the GNU Affero General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package ed.lang.ruby;

import java.io.*;
import java.util.*;

import org.jruby.*;
import org.jruby.ast.Node;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.KCode;
import static org.jruby.runtime.Visibility.PUBLIC;

import ed.appserver.JSFileLibrary;
import ed.appserver.jxp.JxpSource;
import ed.io.StreamUtil;
import ed.js.JSFunction;
import ed.js.engine.Scope;
import ed.net.httpserver.HttpResponse;
import ed.util.Dependency;
import static ed.lang.ruby.RubyObjectWrapper.toJS;
import static ed.lang.ruby.RubyObjectWrapper.toRuby;

public class RubyJxpSource extends JxpSource {

    static final boolean DEBUG = Boolean.getBoolean("DEBUG.RB");
    static final boolean SKIP_REQUIRED_LIBS = Boolean.getBoolean("DEBUG.RB.SKIP.REQ.LIBS");
    /** Scope top-level functions to avoid loading. */
    static final Collection<String> DO_NOT_LOAD_FUNCS;
    static final RubyInstanceConfig config = new RubyInstanceConfig();

    static {
	if (!SKIP_REQUIRED_LIBS)
	    config.requiredLibraries().add("xgen_internals");
	DO_NOT_LOAD_FUNCS = new ArrayList<String>();
	DO_NOT_LOAD_FUNCS.add("print");
	DO_NOT_LOAD_FUNCS.add("sleep");
	DO_NOT_LOAD_FUNCS.add("fork");
	DO_NOT_LOAD_FUNCS.add("eval");
    }

    /** Determines what major version of Ruby to compile: 1.8 (false) or YARV/1.9 (true). **/
    public static final boolean YARV_COMPILE = false;

    public RubyJxpSource(File f , JSFileLibrary lib) {
        _file = f;
        _lib = lib;
	_runtime = org.jruby.Ruby.newInstance(config);
    }

    /** For testing. */
    protected RubyJxpSource(org.jruby.Ruby runtime) {
	_file = null;
	_lib = null;
	_runtime = runtime;
    }

    protected String getContent() throws IOException {
	return StreamUtil.readFully(_file);
    }

    protected InputStream getInputStream() throws FileNotFoundException {
	return new FileInputStream(_file);
    }

    public long lastUpdated(Set<Dependency> visitedDeps) {
        return _file.lastModified();
    }

    public String getName() {
        return _file.toString();
    }

    public File getFile() {
        return _file;
    }

    public synchronized JSFunction getFunction() throws IOException {
        final Node code = _getCode(); // Parsed Ruby code
        return new ed.js.func.JSFunctionCalls0() {
            public Object call(Scope s , Object unused[]) {
		_addSiteRootToPath(s);
		_setOutput(s);
		_exposeScope(s);

		// See the second part of JRuby's Ruby.executeScript(String, String)
		ThreadContext context = _runtime.getCurrentContext();
        
		String oldFile = context.getFile();
		int oldLine = context.getLine();
		try {
		    context.setFile(code.getPosition().getFile());
		    context.setLine(code.getPosition().getStartLine());
		    return _runtime.runNormally(code, YARV_COMPILE);
		} finally {
		    context.setFile(oldFile);
		    context.setLine(oldLine);
		}
            }
        };
    }

    protected Node _getCode() throws IOException {
	final long lastModified = _file.lastModified();
        if (_code == null || _lastCompile < lastModified) {
	    _code = _parseContent(_file.getPath());
	    _lastCompile = lastModified;
	}
        return _code;
    }

    protected Node _parseContent(String filePath) throws IOException {
	// See the first part of JRuby's Ruby.executeScript(String, String)
	String script = getContent();
	byte[] bytes;
	try {
	    bytes = script.getBytes(KCode.NONE.getKCode());
	} catch (UnsupportedEncodingException e) {
	    bytes = script.getBytes();
	}
	return _runtime.parseInline(new ByteArrayInputStream(bytes), filePath, null);
    }

    protected void _addSiteRootToPath(Scope s) {
	Object appContext = s.get("__instance__");
	if (appContext != null) {
	    RubyString siteRoot = RubyString.newString(_runtime, appContext.toString().replace('\\', '/'));
	    RubyArray loadPath = (RubyArray)_runtime.getLoadService().getLoadPath();
	    if (loadPath.include_p(_runtime.getCurrentContext(), siteRoot).isFalse()) {
		if (DEBUG)
		    System.err.println("adding site root " + siteRoot + " to Ruby load path");
		loadPath.append(siteRoot);
	    }
	}
    }

    /**
     * Set Ruby's $stdout so that print/puts statements output to the right
     * place. If we have no HttpResponse (for example, we're being run outside
     * the app server), then nothing happens.
     */
    protected void _setOutput(Scope s) {
	HttpResponse response = (HttpResponse)s.get("response");
	if (response != null)
	    _runtime.getGlobalVariables().set("$stdout", new RubyIO(_runtime, new RubyJxpOutputStream(response.getWriter())));
    }

    /**
     * Creates the $scope global object and a method_missing method for the
     * top-level object.
     */
    protected void _exposeScope(final Scope scope) {
	_runtime.getGlobalVariables().set("$scope", toRuby(scope, _runtime, scope));

	// Add method missing to top object
	RubyClass eigenclass = ((RubyObject)_runtime.getTopSelf()).getSingletonClass();
	eigenclass.addMethod("method_missing", new JavaMethod(eigenclass, PUBLIC) {
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
		    // args[0] is method name symbol, args[1..-1] are arguments
		    String key = args[0].toString();
		    if (RubyObjectWrapper.DEBUG)
			System.err.println("method_missing called on top-level object; symbol = " + key);

		    // Write
		    if (key.endsWith("=")) {
			key = key.substring(0, key.length() - 1);
			if (RubyObjectWrapper.DEBUG)
			    System.err.println("assigning new value to top-level scope var named " + key);
			return toRuby(scope, _runtime, scope.set(key, toJS(scope, _runtime, args[1])));
		    }

		    Object obj = scope.get(key);

		    // Call function
		    if (obj instanceof JSFunction) {
			if (DO_NOT_LOAD_FUNCS.contains(key))
			    return toRuby(scope, _runtime, ((RubyObject)self).callSuper(context, args, block));

			if (RubyObjectWrapper.DEBUG)
			    System.err.println("calling function " + key);

			Object[] jargs = new Object[args.length - 1 + (block.isGiven() ? 1 : 0)];
			for (int i = 1; i < args.length; ++i)
			    jargs[i-1] = toJS(scope, _runtime, args[i]);
			if (block != null && block.isGiven())
			    jargs[args.length-1] = toJS(scope, _runtime, block);

			return toRuby(scope, _runtime, ((JSFunction)obj).call(scope, jargs));
		    }
		    // Check for certain built-in JSObject methods and call them
		    if (obj == null) {
			return toRuby(scope, _runtime, ((RubyObject)self).callSuper(context, args, block));
		    }

		    // Finally, it's a simple ivar retrieved by get(). Return it.
		    if (RubyObjectWrapper.DEBUG)
			System.err.println("returning value of scope instance var named " + key);
		    return (obj == null) ? _runtime.getNil() : toRuby(scope, _runtime, obj);
		}
	    });
    }

    protected final File _file;
    protected final JSFileLibrary _lib;
    protected final org.jruby.Ruby _runtime;

    protected Node _code;
    protected long _lastCompile;
}
