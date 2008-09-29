// JSCompiledScript.java

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

package ed.js.engine;

import java.util.*;

import ed.ext.org.mozilla.javascript.*;

import ed.lang.*;
import ed.util.*;
import ed.js.*;
import ed.js.func.*;
import ed.appserver.*;

public abstract class JSCompiledScript extends JSFunctionCalls0 {

    public JSCompiledScript(){
	myInit();
    }

    protected abstract void myInit();
    protected abstract Object _call( Scope scope , Object extra[] ) throws Exception;
    
    public Object call( Scope scope , Object extra[] ){
	if ( _loadOnce )
	    return load( scope );
	return docall( scope , extra );
    }

    Object docall( Scope scope , Object extra[] ){
        try {
            return _call( scope, extra );
        }
        catch ( RuntimeException re ){
            if ( Convert.DJS ) re.printStackTrace();
            _scriptInfo.fixStack( re );
            throw re;
        }
        catch ( Exception e ){
            e.printStackTrace();
            if ( Convert.DJS ) e.printStackTrace();
            _scriptInfo.fixStack( e );
            throw new RuntimeException( "weird error : " + e.getClass().getName() , e );
        }
    }
    
    protected void _throw( Object foo ){
        
        if ( foo instanceof JSException )
            throw ( JSException)foo;
        
        if ( foo instanceof Throwable )
            throw new JSException( foo.toString() , (Throwable)foo );
        
        throw new JSException( foo );
    }

    /**
     * load this file once.
     * if its already defined in the scope, don't include
     */
    public Object load( Scope scope ){
	String name = this.getClass().getName();
	
	Object res = scope.getLoaded( name );
	if ( res != null ){
	    if ( res == _nullMarker )
		return null;
	    return res;
	}
	
	res = docall( scope , null );
	if ( res == null )
	    res = _nullMarker;
	scope.markLoaded( name , res );
	return res;
    }
    
    public Language getFileLanguage(){
        if ( _scriptInfo == null )
            return Language.JS;
        return _scriptInfo._sourceLanguage;
    }
    
    public JSString _string( int id ){
	if ( _jsstrings == null )
	    _jsstrings = new JSString[ _strings.length ];
	
	if ( _jsstrings[id] == null )
	    _jsstrings[id] = new JSString( _strings[id] );

	return _jsstrings[id];
    }

    public JSRegex _regex( int id ){
        Pair<String,String> p = _regex.get( id );
        return new JSRegex( p.first , p.second );
    }
    
    public long approxSize( IdentitySet seen ){
        long size = super.approxSize( seen );

        if ( _strings != null )
            for ( int i=0; i<_strings.length; i++ )
                if ( _strings[i] != null )
                    size += 8 + _strings[i].length() * 2;

        if ( _jsstrings != null )
            for ( int i=0; i<_jsstrings.length; i++ )
                if ( _jsstrings[i] != null )
                    size += 8 + _jsstrings[i].length() * 2;
        
        if ( _regex != null )
            for ( int i=0; i<_regex.size(); i++ )
                size += 32 + ( _regex.get(i).first.length() * 2 ) + ( _regex.get(i).second.length() * 2 );
        
        return size;
    }

    public void setPath( JSFileLibrary path ){
	_path = path;
    }

    Convert.ScriptInfo _scriptInfo;
    protected List<Pair<String,String>> _regex;
    protected String _strings[];
    protected JSString _jsstrings[];
    protected JSFileLibrary _path;
    protected boolean _loadOnce = false;

    static String _nullMarker = "_nullMarker";
}
