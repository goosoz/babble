// JS.java

/**
*      Copyright (C) 2008 10gen Inc.
*  
*    Licensed under the Apache License, Version 2.0 (the "License");
*    you may not use this file except in compliance with the License.
*    You may obtain a copy of the License at
*  
*       http://www.apache.org/licenses/LICENSE-2.0
*  
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

package ed.js;

import java.io.*;
import java.util.*;

import ed.lang.*;
import ed.js.engine.*;
import ed.appserver.jxp.JxpSource;
import ed.appserver.adapter.AdapterType;
import ed.appserver.AppContext;
import ed.appserver.JSFileLibrary;

public class JS extends Language {

    public JS(){
        super( "js" );
    }

    public JSFunction compileLambda( String source ){     
        return Convert.makeAnon( source , true );
    }

    public JxpSource getAdapter(AdapterType type, File f, AppContext context, JSFileLibrary lib) {
        // TODO - fix me
        return null;
    }
    
    public Object eval( Scope scope , String code , boolean[] hasReturn ){
        return scope.eval( code , "eval" , hasReturn );
    }

    public static boolean JNI = false;
    public static final boolean DI = false;
    public static final boolean RAW_EXCPETIONS = ed.util.Config.get().getBoolean( "RAWE" );

    public static void _debugSI( String name , String place ){
        if ( ! DI )
            return;
        System.err.println( "Static Init : " + name + " \t " + place  );
    }

    public static void _debugSIStart( String name ){
        if ( ! DI )
            return;
        System.err.println( "Static Init : " + name + " Start" );
    }

    public static void _debugSIDone( String name ){
        if ( ! DI )
            return;
        System.err.println( "Static Init : " + name + " Done" );
    }

    /** Critical method.
     * @return 17.  Seriously, the only thing this method does is return 17.
     */
    public static final int fun(){
        return 17;
    }
    
    public static boolean bool( Object o ){
        return JSInternalFunctions.JS_evalToBool( o );
    }
    
    /** Takes a string and, if possible, evaluates it as JavaScript.
     * @param js A string of JavaScript code
     * @return Whatever object is created by the evaluating the JavaScript string
     * @throws Throwable if JavaScript expression is invalid
     */
    public static final Object eval( String js ){
        JNI = true;

        try {
            Scope s = Scope.getAScope().child();
            Object ret = s.eval( js );
            return ret;
        }
        catch ( Throwable t ){
            t.printStackTrace();
            return null;
        }
    }

    /** Returns a short description of an object.
     * @param o object to be stringified.
     * @return string version of the object, or null if the object is null.
     */
    public static final String toString( Object o ){
        if ( o == null )
            return null;

        return o.toString();
    }

    public static boolean isBaseObject( Object o ){
        if ( o == null )
            return false;
        return isBaseObject( o.getClass() );
    }
    
    public static boolean isBaseObject( Class c ){
        return c == JSObjectBase.class || c == JSDict.class;
    }

    public static boolean isPrimitive( Object o ){
        return 
            o instanceof Number ||
            o instanceof JSNumber ||
            o instanceof Boolean ||
            o instanceof JSBoolean ||
            o instanceof String ||
            o instanceof JSString;
        
    }
    
    public static JSArray getArray( JSObject o , String name ){
        List l = getList( o , name );
        if ( l instanceof JSArray )
            return (JSArray)l;
        return null;
    }

    public static List getList( JSObject o , String name ){
        if ( o == null )
            return null;
        Object v = o.get( name );
        if ( v == null )
            return null;
        if ( v instanceof List )
            return (List)v;
        return null;
    }

    public static Object path( JSObject o , String path ){
        if ( o == null )
            return null;
        
        if ( path == null )
            return o;
        
        while ( path.length() > 0 ){
            int idx = path.indexOf( "." );
            if ( idx < 0 )
                return o.get( path );
            
            Object next = o.get( path.substring( 0 , idx ) );
            if ( next == null )
                return null;
            
            if ( ! ( next instanceof JSObject ) )
                return null;
            
            o = (JSObject)next;
            path = path.substring( idx + 1 );
        }
        
        return o;
    }

    public static Object eval( JSObject o , String funcName , Object ... args ){
        if ( o == null )
            throw new NullPointerException( "null object" );
        
        JSFunction f = o.getFunction( funcName );

        if ( f == null )
            throw new NullPointerException( "no function named [" + funcName + "]" );

        return f.callAndSetThis( null , o , args );
    }

    public static JSDict build( String[] names , Object[] values ){
        JSDict d = new JSDict();
        
        for ( int i=0; i<names.length; i++ )
            d.set( names[i] , values != null && i < values.length ? values[i] : null );

        return d;
    }
    
    public static void main( String args[] )
        throws Exception {

        for ( String s : args ){
            s = s.trim();
            if ( s.length() == 0 )
                continue;
            System.out.println( "-----" );
            System.out.println( s );

            Convert c = new Convert( new File( s ) );
            c.get().call( Scope.newGlobal().child() );
        }
    }
}
