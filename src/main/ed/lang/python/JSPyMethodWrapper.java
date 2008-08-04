// JSPyMethodWrapper.java

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

package ed.lang.python;

import java.util.*;

import org.python.core.*;

import ed.js.*;
import ed.js.func.*;
import ed.js.engine.*;
import static ed.lang.python.Python.*;

/**
 * Class used when Python sets something in the prototype to be a function.
 *
 * What the Python code has in mind is probably to define a method. Methods
 * get an explicit "self" argument in Python, but in Javascript they can access
 * an implicit "this" object. Of course, you can call the function directly, 
 * through the prototype rather than through an instance..
 * in which case we could decide not to add "this".. or is that too sensible?
 */
public class JSPyMethodWrapper extends JSPyObjectWrapper {

    public JSPyMethodWrapper( JSFunction klass , PyFunction o ){
        super( o );
        _f = o;
        _klass = klass;
    }

    public Object call( Scope s , Object [] params ){
        // Check if scope.getThis indicates that this is a method call
        // If direct, don't add to params -- "this" could be anything
        // Maybe it would be JavaScript-ier to just pass this anyhow
        boolean mcall = JSInternalFunctions.JS_instanceof( s.getThis(),
                                                           _klass );

        return toJS( callPython( s , params , mcall ) );
    }

    public PyObject callPython( Scope s , Object [] params , boolean passThis ){
        int newlength = params.length;
        if( passThis ) newlength++;
        PyObject [] pParams = new PyObject[newlength];
        int offset = 0;
        if( passThis ){
            pParams[offset++] = toPython( s.getThis() );
        }
        for(int i = 0; i < params.length; ++i){
            pParams[ i + offset ] = toPython(params[i]);
        }
        return _f.__call__( pParams , new String[0] );
    }

    public JSObject getSuper(){
        return _prototype;
    }
    
    private PyObject _f;
    private JSFunction _klass;
}
    