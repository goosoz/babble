// Server.java

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

package ed.net.lb;

import java.net.*;
import java.util.*;

import ed.net.*;
import ed.net.nioclient.*;
import ed.net.httpserver.*;
import static ed.net.lb.Mapping.*;
import ed.log.*;

public class Server implements Comparable<Server> {

    static final Logger _serverLogger = Logger.getLogger( "lb.server" );
    
    Server( InetSocketAddress addr ){
        this( addr , true );
    }
    
    Server( InetSocketAddress addr , boolean register ){
        if ( addr == null )
            throw new NullPointerException( "addr can't be null" );
        
        _logger = _serverLogger.getChild( addr.getHostName() );
	_addr = addr;
        
	_monitor = register ? ServerMonitor.register( this ) : null;
        
        _tracker = new HttpLoadTracker( "Server : " + addr );
	reset();
    }
    
    void reset(){
	_environmentsWithTraffic.clear();
	_serverStart = System.currentTimeMillis();
	_inErrorState = false;
    }
    
    void error( Environment env , NIOClient.ServerErrorType type , Exception what , HttpRequest request , HttpResponse response ){
        if ( what instanceof HttpExceptions.ClientError )
            return;
        _intoErrorState( "error state because of error" , what );
	_tracker.networkEvent();
        _tracker.hit( request , response );
    }
    
    void success( Environment env , HttpRequest request , HttpResponse response ){
	_environmentsWithTraffic.add( env );
        _tracker.hit( request , response );
    }
    
    /**
     * < 0 do not send traffic
     * 0 rather not have traffic
     * > 1 the higher the better
     */
    int rating( Environment e ){
	if ( _inErrorState )
	    return 0;
	
	if ( _environmentsWithTraffic.contains( e ) )
	    return 2;
	
	return 1;
    }
    
    void update( ServerMonitor.Status status ){
	if ( status == null ){
            _intoErrorState( "into error state because status was null" , null );
	    return;
	}
	
        if ( _inErrorState )
            _logger.alert( "out of error state beacuse status not null" );
        _inErrorState = false;

	// TODO
        // look at memory, num request, etc...
        
    }

    public int compareTo( Server s ){
	return this._addr.toString().compareTo( s._addr.toString() );
    }

    public int hashCode(){
	return _addr.hashCode();
    }

    public boolean equals( Object o ){
        
        if ( o == this )
            return true;
        
	if ( ! ( o instanceof Server ) )
	    return false;

	Server s = (Server)o;
	return _addr.equals( s._addr );
    }

    public String toString(){
	return _addr.toString();
    }

    public long timeSinceLastError(){
        return System.currentTimeMillis() - _lastError;
    }

    public boolean inErrorState(){
        return _inErrorState;
    }

    private void _intoErrorState( String msg , Exception e ){
        _inErrorState = true;
        _lastError = System.currentTimeMillis();
        _logger.error( msg , e );
    }
    
    final InetSocketAddress _addr;
    final ServerMonitor.Monitor _monitor;
    final HttpLoadTracker _tracker;
    final Logger _logger;

    final Set<Environment> _environmentsWithTraffic = Collections.synchronizedSet( new HashSet<Environment>() );
    long _serverStart;
    boolean _inErrorState = false;
    long _lastError = 0;
}
