// PythonJxpSource.java

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

import java.io.*;
import java.util.*;

import org.python.core.*;
import org.python.Version;

import ed.js.*;
import ed.js.engine.*;
import ed.util.*;
import ed.appserver.*;
import ed.appserver.jxp.JxpSource;
import ed.log.Logger;

public class PythonJxpSource extends JxpSource {

    static {
        System.setProperty( "python.cachedir", ed.io.WorkingFiles.TMP_DIR + "/jython-cache/" + Version.PY_VERSION );
    }

    public synchronized JSFunction getFunction()
        throws IOException {
        
        final PyCode code = _getCode();

        return new ed.js.func.JSFunctionCalls0(){
            public Object call( Scope s , Object extra[] ){

                final AppContext ac = getAppContext();

                Scope siteScope;

                if ( ac != null )
                    siteScope = ac.getScope();
                else 
                    siteScope = s.getGlobal( true );

                SiteSystemState ss = Python.getSiteSystemState( ac , siteScope );

                PyObject globals = new PyDictionary();

                PyObject result = runPythonCode(code, ac, ss, globals, _lib, _file);

                if (usePassedInScope()){
                    PyObject keys = globals.invoke("keys");
                    if( ! ( keys instanceof PyList ) ){
                        throw new RuntimeException("couldn't use passed in scope: keys not dictionary [" + keys.getClass() + "]");
                    }

                    PyList keysL = (PyList)keys;
                    for(int i = 0; i < keysL.size(); i++){
                        PyObject key = keysL.pyget(i);
                        if( ! ( key instanceof PyString ) ){
                            System.out.println("Non-string key in globals : " + key + " [skipping]");
                            continue;
                        }

                        s.put( key.toString(), Python.toJS( globals.__finditem__(key) ) , true );
                    }
                }
                return Python.toJS( result );
            }
        };
    }

    /**
     * Provides a sensible environment to run Python code. This includes
     * providing a globals dictionary, a module to run in (for import tracking),
     * setting __name__ and __file__, making sure the path includes some
     * important directories, etc.
     */
    public static PyObject runPythonCode(PyCode code, AppContext ac, SiteSystemState ss, PyObject globals,
                                         JSFileLibrary lib, File file) {

        PySystemState pyOld = Py.getSystemState();

        ss.flushOld();

        ss.ensurePath( file.getParent());
        ss.ensurePath( lib.getRoot().getAbsolutePath() );
        ss.ensurePath( lib.getTopParent().getRoot().getAbsolutePath() );

        PyObject result = null;
        
        try {
            Py.setSystemState( ss.getPyState() );

            globals.__setitem__( "__file__", Py.newString( file.toString() ) );

            // FIXME: Needs to use path info, so foo/bar.py -> foo.bar
            // Right now I only want this for _init.py
            String name = file.getName();
            if( name.endsWith( ".py" ) )
                name = name.substring( 0 , name.length() - 3 );

            /*
             * In order to track dependencies, we need to know what module is doing imports
             * We just care that _init doing imports is registered at all.
             */
            PyModule module = new PyModule( name , globals );

            PyObject locals = module.__dict__; // FIXME: locals == globals ?
            result = Py.runCode( code, locals, globals );
            if( ac != null ) ss.addRecursive( "_init" , ac );
        }
        finally {
            Py.setSystemState( pyOld );
        }
        return result;
    }

    private static void _globalRestore( PyObject globals , String name , PyObject value ){
        if( value != null ){
            globals.__setitem__( name , value  );
        }
        else{
            globals.__delitem__( name );
        }
    }

    private PyCode _getCode()
        throws IOException {
        PyCode c = _code;
	final long lastModified = _file.lastModified();
        if ( c == null || _lastCompile < lastModified ){
            c = Python.compile( _file );
            _code = c;
            _lastCompile = lastModified;
        }
        return c;
    }

    final File _file;
    final JSFileLibrary _lib;

    private PyCode _code;
    private long _lastCompile;
    
    void addDependency( String to ){
        super.addDependency( new FileDependency( new File( to ) ) );
    }

    public PythonJxpSource( File f , JSFileLibrary lib ){
        _file = f;
        _lib = lib;
    }

    protected String getContent(){
        throw new RuntimeException( "you can't do this" );
    }

    protected InputStream getInputStream(){
        throw new RuntimeException( "you can't do this" );
    }

    public long lastUpdated(Set<Dependency> visitedDeps){
        return _file.lastModified();
    }

    public String getName(){
        return _file.toString();
    }

    public File getFile(){
        return _file;
    }
    // static b/c it has to use ThreadLocal anyway
    final static Logger _log = Logger.getLogger( "python" );
}
