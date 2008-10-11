// NIOClient.java

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

package ed.net;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import ed.io.*;
import ed.log.*;
import ed.util.*;
import ed.net.httpserver.*;
import static ed.net.HttpExceptions.*;

public abstract class NIOClient extends Thread {

    public enum ServerErrorType { WEIRD , INVALID , CONNECT , SOCK_TIMEOUT };

    protected enum WhatToDo { CONTINUE , PAUSE , DONE_AND_CLOSE , DONE_AND_CONTINUE , ERROR };

    public static final SimpleDateFormat SHORT_TIME = new SimpleDateFormat( "MM/dd HH:mm:ss.S" );
    static final long AFTER_SHUTDOWN_WAIT = 1000 * 60;

    public NIOClient( String name , int connectionsPerHost , int verboseLevel ){
        super( "NIOClient: " + name );
        _name = name;
        _connectionsPerHost = connectionsPerHost;
        
        _logger = Logger.getLogger( "nioclient-" + name );
	_logger.setLevel( Level.forDebugId( verboseLevel ) );

        _loggerOpen = _logger.getChild( "open" );
        _loggerDrop = _logger.getChild( "drop" );
        
        _addMonitors();

        try {
            _selector = Selector.open();
        }
        catch ( IOException ioe ){
            throw new RuntimeException( "can't open selector" , ioe );
        }
        
    }
    
    protected abstract void serverError( InetSocketAddress addr , ServerErrorType type , Exception why );

    protected void shutdown(){
        _shutdown = true;
        _shutdownTime = System.currentTimeMillis();
        _logger.error( "SHUTDOWN RECEIVED" );
    }
    
    public void run(){
        while ( true ){
            try {
                _run();
            }
            catch ( Exception e ){
                _logger.error( "error in run loop" , e );
            }

            if ( _shutdown && ( System.currentTimeMillis() - _shutdownTime ) > AFTER_SHUTDOWN_WAIT )
                break;
            
        }
    }
    
    public boolean add( Call c ){
        return _newRequests.offer( c );
    }
    
    private void _run(){
        _doNewRequests();
        _doOldStuff();
    }
    
    private void _doOldStuff(){
        int numKeys = 0;
        try {
            numKeys = _selector.select( 10 );                
        }
        catch ( IOException ioe ){
            _logger.error( "can't select" , ioe );
        }
        
        if ( numKeys <= 0 )
            return;
        
        final Iterator<SelectionKey> i = _selector.selectedKeys().iterator();
        while ( i.hasNext() ){
            SelectionKey key = i.next();
            i.remove();
            
            if ( ! key.isValid() )
                continue;
            
            Connection c = (Connection)key.attachment();
            
            if ( c == null ){
                _logger.error( "attachment was null " );
                continue;
            }                

            if ( key.isConnectable() )
                c.handleConnect();
            else if ( key.isReadable() )
                c.handleRead();
            else if ( key.isWritable() )
                c.handleWrite();
        }
        
    }
    
    private void _doNewRequests(){
        List<Call> pushBach = new LinkedList<Call>();
        
        for ( int i=0; i<20; i++ ){ // don't want to just handle new requests
            
            Call c = _newRequests.poll();
            if ( c == null )
                break;
            
            if ( c._cancelled ){
		pushBach.add( c );
                continue;
	    }
            
            if ( c._paused ){
		pushBach.add( c );
                continue;
	    }
	    
            InetSocketAddress addr = null;
            try {
                addr = c.where();
		_logger.debug( 2 , "address" , c , addr );

                if ( addr == null ){
		    pushBach.add( c );
                    continue;
		}
                
                final ConnectionPool pool = getConnectionPool( addr );
                
                Connection conn = pool.get( 0 );
                if ( conn == null ){
                    pushBach.add( c );
                    continue;
                }
                
                if ( ! conn.ready() ){
                    pushBach.add( c );
                    pool.done( conn );
                    continue;
                }

                conn.start( c );

            }
            catch ( CantOpen co ){
                _logger.error( "couldn't open" , co );
                c.error( ServerErrorType.CONNECT , co );
                if ( addr != null )
                    serverError( addr , ServerErrorType.CONNECT , co._ioe );
            }
            catch ( RuntimeException re ){
                _logger.error( "runtime exception in _doNewRequests" , re );
                c.error( ServerErrorType.WEIRD , re );
            }
                
        }
        
        for ( Call c : pushBach ){
            if ( ! _newRequests.offer( c ) ){
                _loggerDrop.error( "couldn't push something back on to queue." );
            }
        }
    }
    
    public boolean isShutDown(){
        return _shutdown;
    }

    public ConnectionPool getConnectionPool( InetSocketAddress addr ){
        ConnectionPool p = _connectionPools.get( addr );
        if ( p != null )
            return p;

        p = new ConnectionPool( addr );
        _connectionPools.put( addr , p );
        
        return p;
    }

    public List<InetSocketAddress> getAllConnections(){
        return new LinkedList<InetSocketAddress>( _connectionPools.keySet() );
    }

    protected class Connection {
        
        Connection( ConnectionPool pool , InetSocketAddress addr ){
            _pool = pool;
            _addr = addr;
            try {
                _sock = SocketChannel.open();
                _sock.configureBlocking( false );
                _sock.connect( _addr );
                _key = _sock.register( _selector , SelectionKey.OP_CONNECT , this );
                
                _loggerOpen.debug( "opening connection to [" + addr + "]" );
            }
            catch ( UnresolvedAddressException e ){
                _error = new UnknownHostException( addr.toString() );
                throw new CantOpen( addr , _error );
            }
            catch ( IOException ioe ){
                _error = ioe;
                throw new CantOpen( addr , ioe );
            }
        }
        
        void handleConnect(){
            IOException err = null;
            try {
                if ( ! _sock.finishConnect() ){
                    err = new IOException( "finishConnect faild silently" );
                    err.fillInStackTrace();
                }
            }
            catch ( IOException ioe ){
                err = ioe;
            }
            catch ( Exception e ){
                err = new IOException( "weird error on finish connect : " + e );
            }
            
            if ( err == null ){
                _loggerOpen.debug( "done opening connection to [" + _addr + "]" );
                _ready = true;
                return;
            }

            _error = err;            
            serverError( _addr , ServerErrorType.CONNECT , err );
            _loggerOpen.error( "error opening connection to [" + _addr + "]" , _error );            
        }
        
        boolean ready(){
            return _ready;
        }
        
        boolean ok(){
            if ( _error != null )
                return false;

            if ( System.currentTimeMillis() - _opened > 1000 * 60 )
                return false;
            
            if ( _closed )
                return false;

            return true;
        }
        
        public int doRead( boolean errorOnEOF ){
            _fromServer.position( 0 );
            _fromServer.limit( _fromServer.capacity() );
            
            int read = 0;

            try {
                read = _sock.read( _fromServer );
            }
            catch ( IOException ioe ){
                _error( ServerErrorType.SOCK_TIMEOUT , ioe );
                return -1;
            }
            
            if ( read < 0 ){
                if ( errorOnEOF )
                    _error( ServerErrorType.SOCK_TIMEOUT ,new IOException( "socket dead" ) );
                done( true );
                return -1;
            }
            
            if ( read != _fromServer.position() )
                throw new RuntimeException( "i'm confused  says i read [" + read + "] but at position [" + _fromServer.position() + "]" );
            
            _fromServer.flip();

            return read;
        }

        void handleRead(){
            // read data from wire
            // pass data to Call
            // response could be
            //   - continue
            //   - pause - turn off selector
            //   - done - add yourself back to the pool
            //   - error, close connection
            
            if ( doRead( true ) < 0 )
                return;
            
            WhatToDo next = null;
            if ( _current == null ){
                _logger.error( " _current is null in handleRead, should never happen" );
                next = WhatToDo.DONE_AND_CLOSE;
            }
            else {
                next = _current.handleRead( _fromServer , this );
            }
            
            switch ( next ){
            case CONTINUE: 
                _key.interestOps( _key.OP_READ );
                return;
            case PAUSE: 
                _key.interestOps( 0 );
                return;
            case ERROR:
                _userError( "unknown" );
                return;
            case DONE_AND_CLOSE:
                done( true );
                return;
            case DONE_AND_CONTINUE:
                done( false );
                return;
            }
        }
        
        public void done( boolean close ){

            if ( close )
                close();

            if ( _error == null ){
                _logger.debug( 2 , "putting connection back in pool" );
                _pool.done( this );
            }
            _current = null;
        }

        void handleWrite(){
            int wrote = 0;
            try {
                wrote = _sock.write( _toServer );
            }
            catch ( IOException ioe ){
                _error( ServerErrorType.SOCK_TIMEOUT , ioe );
                _key.interestOps( 0 );
            }
            
            if ( _toServer.position() == _toServer.limit() ){
		
		if ( _extraDataToServer != null && _extraDataToServer.hasMore() ){
		    _toServer.position(0);
		    _toServer.limit( _toServer.capacity() );
		    _extraDataToServer.write( _toServer );
		    _toServer.flip();
		    handleWrite();
		    return;
		}

		_extraDataToServer = null;
                _key.interestOps( _key.OP_READ );
                _logger.debug( 3 , "finished writing" );
                return;
            }
            
            if ( wrote < 0 ){
                _error( ServerErrorType.SOCK_TIMEOUT , new IOException( "wrote 0 bytes" ) );
                return;
            }

            // need to write more
            _key.interestOps( _key.OP_WRITE );
        }

        void start( Call c ){
            if ( c == null ){
                _userError( "shouldn't call start with a null Call" );
                return;
            }
            
            if ( _current != null ){
                _userError( "trying to start a Call but already have one" );
                return;
            }
            
            _current = c;
            
            _toServer.position( 0 );
            _toServer.limit( _toServer.capacity() );
            
            _extraDataToServer = _current.fillInRequest( _toServer );
            if ( _toServer.position() == 0 ){
                _userError( "fillInRequest didn't give me any data" );
                return;
            }
            
            _toServer.flip();

            handleWrite();
        }

        private void _userError( String msg ){
            _error( ServerErrorType.WEIRD , new IOException( "User Error : " + msg ) );
            throw new RuntimeException( msg );
        }

        private void _error( ServerErrorType type , IOException e ){
            _error = e;
            if ( _current != null )
                _current.error( type , e );
            if ( _ready ){
                close();
            }
        }

        public String toString(){
            return _addr.toString();
        }
        
        void close(){
            if ( _closed )
                return;
            
            _closed = true;
            try {
                _key.interestOps( 0 );
                _key.attach( null );
                _key.cancel();
                _sock.close();
            }
            catch ( IOException ioe ){
                // don't care
            }
        }

        final ConnectionPool _pool;
        final InetSocketAddress _addr;
        final long _opened = System.currentTimeMillis();

        final ByteBuffer _toServer = ByteBuffer.allocateDirect( 1024 * 32 );
        final ByteBuffer _fromServer = ByteBuffer.allocateDirect( 1024 * 32 );
	private ByteStream _extraDataToServer = null;

        final SocketChannel _sock;
        final SelectionKey _key;  
        
        private boolean _ready = false;
        private IOException _error = null;
        private boolean _closed = false;
        
        private Call _current = null;

        
    }
    
    class ConnectionPool extends SimplePool<Connection> {
        ConnectionPool( InetSocketAddress addr ){
            super( "ConnectionPool : " + addr , _connectionsPerHost , _connectionsPerHost );
            _addr = addr;
        }

        protected Connection createNew(){
            return new Connection( this , _addr );
        }
        
        public boolean ok( Connection c ){
            return c.ok();
        }
        
        final InetSocketAddress _addr;
    }
    
    
    public static abstract class Call {
        
        protected abstract InetSocketAddress where(); 
        protected abstract void error( ServerErrorType type , Exception e );
        
        protected abstract ByteStream fillInRequest( ByteBuffer buf );
        protected abstract WhatToDo handleRead( ByteBuffer buf , Connection conn );
        
        protected void cancel(){
            _cancelled = true;
        }

        protected void pause(){
            _paused = true;
        }
        
        protected void wakeup(){
            _paused = false;
        }

        public long getStartedTime(){
            return _started;
        }

        public long getTotalTime(){
            if ( _done )
                return _doneTime - _started;
            return -1;
        }

        public void done(){
            _done = true;
            _doneTime = System.currentTimeMillis();
        }

        public boolean isDone(){
            return _done;
        }

        private boolean _cancelled = false;
        private boolean _paused = false;
        private boolean _done = false;
        
        protected final long _started = System.currentTimeMillis();
        private long _doneTime = -1;
    }

    protected abstract class MyMonitor extends HttpMonitor {
        protected MyMonitor( String name ){
            super( _name + "-" + name );
        }
        
    }

    void _addMonitors(){
        HttpServer.addGlobalHandler( new MyMonitor( "serverConnPools" ){
                public void handle( MonitorRequest mr ){
		    
		    JxpWriter out = mr.getWriter();

                    for ( InetSocketAddress addr : getAllConnections() ){
                        out.print( "<b>"  );
                        out.print( addr.toString() );
                        out.print( "</b>   " );

                        ConnectionPool pool = getConnectionPool( addr );

                        out.print( "total: " );
                        out.print( pool.total() );
                        out.print( "   " );

                        out.print( "inUse: " );
                        out.print( pool.inUse() );
                        out.print( "   " );
                        
                        out.print( "everCreated: " );
                        out.print( pool.everCreated() );
                        out.print( "   " );

                        out.print( "<br>" );
                        
                    }
                }
            }
            );
        
    }

    final protected String _name;
    final protected int _connectionsPerHost;
    private boolean _shutdown = false;
    private long _shutdownTime = 0;

    final Logger _logger;
    final Logger _loggerOpen;
    final Logger _loggerDrop;

    private Selector _selector;
    private final BlockingQueue<Call> _newRequests = new ArrayBlockingQueue<Call>( 1000 );
    private final Map<InetSocketAddress,ConnectionPool> _connectionPools = new HashMap<InetSocketAddress,ConnectionPool>();
    
}
