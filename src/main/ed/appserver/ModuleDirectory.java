// ModuleDirectory.java

package ed.appserver;

import java.io.*;
import java.util.*;

import ed.js.*;
import ed.js.engine.*;

public class ModuleDirectory extends JSObjectLame {
    
    public ModuleDirectory( String root , String name , AppContext context , Scope scope ){
        this( new File( Module._base , root ) , name , context , scope );
    }

    public ModuleDirectory( File root , String name , AppContext context , Scope scope ){
        _root = root;
        _name = name;
        _context = context;
        _scope = scope;
    }
    
    public synchronized Module getModule( String name ){
        Module m = _modules.get( name );
        if ( m != null )
            return m;
        
        m = new Module( new File( _root , name ) , _name + "." + name , true );
        _modules.put( name , m );
        return m;
    }
    
    public synchronized JSFileLibrary getJSFileLibrary( String name ){
        JSFileLibrary lib = _libraries.get( name );
        if ( lib != null )
            return lib;

        Module m = getModule( name );
        lib = m.getLibrary( getDesiredVersion( name ) , _context , _scope );
        _libraries.put( name , lib );
        return lib;
    }
    
    public Object get( Object n ){
        return getJSFileLibrary( n.toString() );
    }

    public String getDesiredVersion( String name ){

        if ( _scope != null )
            return _getDesiredVersion( _scope , name );

        if ( _context != null )
            return _getDesiredVersion( _context._scope , name ); // its very important this not call getScope().  that would cause an inf. loop
        
        return null;
    }

    String _getDesiredVersion( Scope s , String name ){
        return AppContext.getVersionForLibrary( s , name );
    }
    
    final String _name;
    final File _root;
    final AppContext _context;
    final Scope _scope;

    final Map<String,Module> _modules = new HashMap<String,Module>();
    final Map<String,JSFileLibrary> _libraries = new HashMap<String,JSFileLibrary>();
    
}