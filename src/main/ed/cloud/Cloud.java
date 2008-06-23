// Cloud.java

package ed.cloud;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import ed.js.*;
import ed.js.engine.*;
import ed.log.*;

public class Cloud extends JSObjectBase {

    static Logger _log = Logger.getLogger( "cloud" );
    static {
        _log.setLevel( Level.INFO );
    }

    private static final Cloud INSTANCE = new Cloud();

    public static synchronized Cloud getInstance(){
	return INSTANCE;
    }


    // ---

    private Cloud(){

	File cloudDir = new File( "src/main/ed/cloud/" );
	_scope = Scope.newGlobal().child( "cloud" );
        
        _bad = ! cloudDir.exists();
        if ( _bad ){
            System.err.println( "NO CLOUD" );
            return;
        }


	Shell.addNiceShellStuff( _scope );
	_scope.set( "Cloud" , this );
	_scope.set( "log" , _log );

	try {
	    _scope.set( "SERVER_NAME" , System.getProperty( "SERVER-NAME" , InetAddress.getLocalHost().getHostName() ) );
	}
	catch ( Exception e ){
	    throw new RuntimeException( "should be impossible : " + e );
	}
	
	List<File> toLoad = new ArrayList<File>();
	for ( File f : cloudDir.listFiles() ){

	    if ( ! f.getName().matches( "\\w+\\.js" ) )
		continue;
	    
	    toLoad.add( f );
	}

	final Matcher numPattern = Pattern.compile( "(\\d+)\\.js$" ).matcher( "" );
	Collections.sort( toLoad , new Comparator<File>(){
			      public int compare( File aFile , File bFile ){
				  int a = Integer.MAX_VALUE;
				  int b = Integer.MAX_VALUE;
				  
				  numPattern.reset( aFile.getName() );
				  if ( numPattern.find() )
				      a = Integer.parseInt( numPattern.group(1) );

				  numPattern.reset( bFile.getName() );
				  if ( numPattern.find() )
				      b = Integer.parseInt( numPattern.group(1) );

				  return a - b;
			      }

			      public boolean equals( Object o ){
				  return o == this;
			      }
			  } );

	for ( File f : toLoad ){
	    _log.debug( "loading file : " + f );
	    try {
		_scope.eval( f );
	    }
	    catch ( IOException ioe ){
		throw new RuntimeException( "can't load cloud js file : " + f , ioe );
	    }
	}
	
    }

    public String getDBHost( String name , String environment ){
        if ( _bad )
            return null;

        JSObject site = findSite( name , false );
        if ( site == null )
            return null;
        
        String dbname = evalFunc( site , "getDatabaseServerForEnvironmentName" , environment ).toString();
	if ( dbname == null )
	    throw new RuntimeException( "why is dbname null for : " + name + ":" + environment );
	
	JSObject db = (JSObject)evalFunc( "Cloud.findDBByName" , dbname );
	if ( db == null )
	    throw new RuntimeException( "can't find global db named [" + dbname + "]" );
	
	Object machine = db.get( "machine" );
	if ( machine == null )
	    throw new RuntimeException( "global db [" + dbname + "] doesn't have machine set" );

        return machine.toString();
    }

    public JSObject findSite( String name , boolean create ){
        if ( _bad )
            return null;
        return (JSObject)(evalFunc( "Cloud.Site.forName" , name , create ));
    }

    public Zeus createZeus( String host , String user , String pass )
        throws IOException {
        return new Zeus( host , user , pass );
    }

    Object evalFunc( String funcName , Object ... args ){
        return evalFunc( null , funcName , args );
    }
    
    Object evalFunc( JSObject t , String funcName , Object ... args ){
	
        if ( args != null ){
	    for ( int i=0; i <args.length; i++ ){
		if ( args[i] instanceof String )
		    args[i] = new JSString( (String)args[i] );
	    }
	}
	
        JSFunction func = null;
        
        if ( func == null && t != null ){
            func = (JSFunction)t.get( funcName );
        }

        if ( func == null )
            func = (JSFunction)findObject( funcName );

	if ( func == null )
	    throw new RuntimeException( "can't find func : " + funcName );
        
        Scope s = _scope;
        if ( t != null ){
            s = _scope.child();
            s.setThis( t );
        }

	return func.call( s , args );
    }
    
    Object findObject( String name ){

	if ( ! name.matches( "[\\w\\.]+" ) )
	    throw new RuntimeException( "this is to complex for my stupid code [" + name + "]" );
	
	String pcs[] = name.split( "\\." );
	Object cur = this;
	
	for ( int i=0; i<pcs.length; i++ ){
	
	    if ( i == 0 && pcs[i].equals( "Cloud" ) )
		continue;
	    
	    cur = ((JSObject)cur).get( pcs[i] );
	    if ( cur == null )
		return null;
	}
	return cur;
    }

    public Scope getScope(){
        return _scope;
    }
    
    public boolean isRealServer(){
        if ( _bad )
            return false;
    
        JSObject me = (JSObject)(_scope.get("me"));
        if ( me == null )
            return false;

        return ! JSInternalFunctions.JS_evalToBool( me.get( "bad" ) );
    }
    
    public String toString(){
        return "{ Cloud.  real: " + isRealServer() + "}";
    }

    final Scope _scope;
    final boolean _bad;
}
