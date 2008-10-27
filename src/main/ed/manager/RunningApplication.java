// RunningApplication.java

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

package ed.manager;

import java.io.*;
import java.util.*;

import ed.io.*;
import ed.log.*;
import ed.util.*;

public class RunningApplication extends Thread {

    public RunningApplication( Manager manager , Application app ){
        super( "RunningApplication:" + app.getType() + ":" + app.getId() );

        _manager = manager;
        _app = app;
        
        _fullId = app.getType() + "-" + app.getId();
        
        _logger = _manager._logger.getChild( app.getType() ).getChild( app.getId() );
    }
        
    public void run(){
            
        while ( ! ( _shutdown || _manager.isShutDown() ) ){
            _pid = -1;
                
            final Application app = _app;
            int exitValue = 0;
                
            _logger.info( "STARTING" );
                
            try {
                _timesStarted++;
                _lastStart = System.currentTimeMillis();
                exitValue = run( app );
            }
            catch ( Exception ioe ){
                _logger.error( "error running" , ioe );
                exitValue = -1;
            }
            finally {
                _pid = -1;
                _process = null;
            }

            _logger.info( "exited : " + exitValue );

            if ( ! _app.restart( exitValue ) ){
                _logger.info( "DONE" );
                break;
            }
            
            if ( _shutdown || _manager.isShutDown() )
                break;
            
            _logger.info( "RESTARTING.  exitValue : " + exitValue  );
        }
        
        if ( _shuttingDownThread != null )
            _shuttingDownThread.interrupt();
        
        _done = true;
        _process = null;
        _manager.interrupt();
    }

    private int run( final Application app )
        throws IOException , InterruptedException {
            
        fileRotate( app.getLogDir() , app.getType() , app.getId() );
        
        app.getLogDir().mkdirs();
        File logFile = _getLogFile( app.getLogDir() , app.getType() , app.getId() );
        _logger.debug( "logFile : " + logFile.getAbsolutePath() );
        OutputStream log = new FileOutputStream( logFile );
        
        String[] command = app.getCommand();
        if ( command[0].startsWith( "./" ) ){
            String[] temp = new String[command.length];
            System.arraycopy( command , 0 , temp , 0 , command.length );
            temp[0] = (new File( app.getExecDir() , command[0] ) ).getAbsolutePath();
            command = temp;
        }
        
        _logger.debug( "full command " + Arrays.toString( command ) );

        _process = Runtime.getRuntime().exec( command , SysExec.envMapToArray( app.getEnvironmentVariables() ) , app.getExecDir() );
        _pid = SysExec.getPID( _process );
            
        _logger.info( "pid : " + _pid );
            
        _process.getOutputStream().close(); // closing input because we're not using ut
            
        OutputPiper stdout = new OutputPiper( _process.getInputStream() , app , true , log );
        OutputPiper stderr = new OutputPiper( _process.getErrorStream() , app , false , log );
            
        stdout.start();
        stderr.start();
            
        stdout.join();
        stderr.join();
        
        log.close();
            
        return _process.waitFor();
    }

    void restart( Application app ){
        throw new RuntimeException( "don't know how to restart" );
    }
    
    public void shutdown(){
        
        _shutdown = true;

        if ( _done )
            return;

        while ( _shuttingDownThread != null )
            throw new RuntimeException( "someone got here first" );
        
        try {
            
            if ( _process == null )
                return;
            
            if ( _pid <= 0 ){
                _logger.error( "no pid, so just killing" );
                _destroy();
                return;
            }
            
            try {
                SysExec.exec( "kill " + _pid );
            }
            catch ( Exception e ){
                _logger.error( "couldn't kill" );
                _destroy();
                return;
            }
            
            _shuttingDownThread = Thread.currentThread();
            try {
                Thread.sleep( _app.timeToShutDown() + 300 );
            }
            catch ( InterruptedException ie ){}
            _shuttingDownThread = null;

            if ( ! _done ){
                _logger.error( "not shutdown after waiting.  killing" );
                _destroy();
            }

        }
        finally {
            _shuttingDownThread = null;
        }
        
    }
    
    private void _destroy(){
        if ( _process == null )
            return;
        
        try {
            _process.destroy();
        }
        catch ( Exception e ){
            _logger.error( "destory had an error" , e );
        }
        _process = null;
    }

    public int hashCode(){
        return _fullId.hashCode();
    }

    public boolean equals( Object o ){
        return ((RunningApplication)o)._fullId.equals( _fullId );
    }

    public int getUptimeMinutes(){
        return (int)( ( System.currentTimeMillis() - _lastStart ) / ( 1000 * 60 ) );
    }

    public long getLastStart(){
        return _lastStart;
    }

    public int timesStarted(){
        return _timesStarted;
    }
    
    public String outputLine( int back ){
        return _lastOutput.get( back );
    }

    static void fileRotate( File dir , String type , String id ){
        for ( int i=7; i>=0; i-- ){

            File f = _getLogFile( dir , type , id , i );

            if ( f.exists() )
                f.renameTo( _getLogFile( dir , type , id , i + 1 ) );
                
        }
    }
    
    static File _getLogFile( File dir , String type , String id ){
        return _getLogFile( dir , type , id , 0 );
    }

    static File _getLogFile( File dir , String type , String id , int num ){
        return new File( dir , type + "." + id + ".log" + ( num == 0 ? "" : "." + num ) );
    }

    boolean isDone(){
        return _done;
    }

    Application _app;
    
    final Manager _manager;
    final String _fullId;
    final Logger _logger;
    
    final CircularList<String> _lastOutput = new CircularList<String>( 100 , true );

    private boolean _shutdown = false;
    private boolean _done = false;
    private Thread _shuttingDownThread;

    private int _pid = -1;
    private Process _process;
    private long _lastStart;
    private int _timesStarted = 0;


    class OutputPiper extends Thread {
            
        OutputPiper( InputStream in , Application app , boolean stdout , OutputStream out ){
            super( "OutputPiper" );
            setDaemon( true );
            _in = new BufferedReader( new InputStreamReader( in ) );
            _stdout = stdout;
            _app = app;
            _out = out;
        }
            
        public void run(){
            String line;
            try {
                while ( ( line = _in.readLine() ) != null ){

                    if ( _stdout )
                        if ( ! _app.gotOutputLine( line ) )
                            continue;
                        else 
                            if ( ! _app.gotErrorLine( line ) )
                                continue;
                    
                    synchronized ( _out ){
                        _out.write( line.getBytes() );
                        _out.write( NEWLINE );
                            
                        _lastOutput.add( line );
                    }
                        
                }            
            }
            catch ( IOException ioe ){
                _logger.error( "error piping output" , ioe );
            }
        }
            
        final BufferedReader _in;
        final Application _app;
        final boolean _stdout;
        final OutputStream _out;
        
    }

    private static final byte[] NEWLINE = "\n".getBytes();
}
