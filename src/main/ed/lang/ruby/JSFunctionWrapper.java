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

import java.util.List;
import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

import ed.js.engine.Scope;
import ed.js.func.JSFunctionCalls0;
import static ed.lang.ruby.RubyObjectWrapper.toJS;
import static ed.lang.ruby.RubyObjectWrapper.toRuby;

/**
 * JSFunctionWrapper acts as a bridge between Ruby blocks and JSFunctions.
 * This is a JSFunction that contains and calls a Ruby block.
 *
 * @see RubyJSFunctionWrapper
 */
public class JSFunctionWrapper extends JSFunctionCalls0 {

    static final IRubyObject[] EMPTY_IRUBY_OBJECT_ARRAY = new IRubyObject[0];

    private Scope _scope;
    private Ruby _runtime;
    private Block _block;

    public JSFunctionWrapper(Scope scope, Ruby runtime, Block block) {
	if (RubyObjectWrapper.DEBUG)
	    System.err.println("wrapping a block in a JSFunctionWrapper");
	_scope = scope;
	_runtime = runtime;
	_block = block;
    }

    public Object callBlock(Object ... args) {
	if (RubyObjectWrapper.DEBUG)
	    System.err.println("calling Ruby block");
	List<IRubyObject> rargs = new ArrayList<IRubyObject>();
	if (args != null)
	    for (Object obj : args)
		rargs.add(toRuby(_scope, _runtime, obj));
	return toJS(_scope, _block.call(_runtime.getCurrentContext(), (IRubyObject[])rargs.toArray(EMPTY_IRUBY_OBJECT_ARRAY)));
    }

    public Block getBlock() { return _block; }

    public RubyProc getProc() {
	RubyProc p = _block.getProcObject();
	if (p != null)
	    return p;

	p = RubyProc.newProc(_runtime, _block, _block.type);
	_block.setProcObject(p);
	return p;
    }

    public Object call(Scope scope, Object[] extra) { return callBlock(extra); }
}