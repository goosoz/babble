// DBHook.java

package ed.js;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.twmacinta.util.*;

import ed.util.*;
import ed.js.engine.*;

public class DBHook {

    public static final int NO_SCOPE = -1;
    public static final int NO_FUNCTION = -2;

    public static final int INVOKE_ERROR = -3;

    // -----    scope   -------    

    public static long scopeCreate(){
        Scope s = Scope.GLOBAL.child();
        _scopes.put( s.getId() , s );
        return s.getId();
    }

    public static boolean scopeReset( long id ){
        Scope s = _scopes.get( id );
        if ( s == null )
            return false;
        s.reset();
        return true;
    }
    public static void scopeFree( long id ){
        _scopes.remove( id );
    }
    
    // -- getters

    public static double scopeGetNumber( long id , String field ){
        Number n = (Number)_scopeGet( id , field );
        if ( n == null )
            return 0;
        return n.doubleValue();
    }

    public static String scopeGetString( long id , String field ){
        Object o = _scopeGet( id , field );
        if ( o == null )
            return null;
        return o.toString();
    }

    public static JSObject scopeGetObject( long id , String field ){
        Object o = _scopeGet( id , field );
        if ( o == null )
            return null;
        if ( ! ( o instanceof JSObject ) )
            return null;
        return (JSObject)o;
    }
    
    static Object _scopeGet( long id , String field ){
        return _scopes.get( id ).get( field );
    }
    
    private static Map<Long,Scope> _scopes = Collections.synchronizedMap( new HashMap<Long,Scope>() );

    // -----    functions   -------

    public static long functionCreate( String code ){
        String md5 = null;
        synchronized( _myMd5 ){
            _myMd5.Init();
            _myMd5.Update( code.toString() );
            md5 = _myMd5.asHex();
        }
        
        Pair<JSFunction,Long> p = _functions.get( md5 );
        if ( p != null )
            return p.second;
        
        JSFunction f = null;
        
        try {
            Convert c = new Convert( "trigger" + Math.random() , code );
            f = c.get();
        }
        catch ( Throwable t ){
            t.printStackTrace();
            return 0;
        }

        long id = _funcID++;
        p = new Pair<JSFunction,Long>( f , id );
        _functions.put( md5 , p );
        _functionIDS.put( id , f );
        return id;
    }
    private final static MD5 _myMd5 = new MD5();
    private final static Map<String,Pair<JSFunction,Long>> _functions = Collections.synchronizedMap( new HashMap<String,Pair<JSFunction,Long>>() );
    private final static Map<Long,JSFunction> _functionIDS = Collections.synchronizedMap( new HashMap<Long,JSFunction>() );
    private static long _funcID = 1;

    // ------ invoke -----

    public static int invoke( long scopeID , long functionID , ByteBuffer objectByteBuffer ){
        Scope s = _scopes.get( scopeID );
        System.err.println( "scopeID : " + scopeID + " functionID : " + functionID );
        if ( s == null )
            return NO_SCOPE;

        JSFunction f = _functionIDS.get( functionID );
        if ( f == null )
            return NO_FUNCTION;
        
        try {
            Object ret = f.call( s , null );
            s.set( "return" , ret );
            return 0;
        }
        catch ( Throwable t ){
            s.set( "error" , t.toString() );
            return INVOKE_ERROR;
        }

    }

}
