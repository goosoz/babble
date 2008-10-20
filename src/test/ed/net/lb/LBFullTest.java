// LBFullTest.java

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

package ed.net.lb;

import java.io.*;
import java.net.*;
import java.util.*;

import org.testng.annotations.Test;

import ed.*;
import ed.io.*;
import ed.util.*;
import ed.net.httpserver.*;

public class LBFullTest extends HttpServerTest {
    
    public LBFullTest()
        throws IOException {
        super();
        _lb = new LB( _lbPort , new MyMappingFactory() , 0 );
        _lb.start();
    }

    
    protected void finalize() throws Throwable {
        super.finalize();
        _lb.shutdown();
    }
    
    protected void checkResponse( Response r ){
        assert( r.headers.containsKey( "x-lb" ) );
    }

    protected Socket open()
        throws IOException {
        return new Socket("127.0.0.1", _lbPort);
    }
        
    
    class MyMappingFactory implements MappingFactory {
        
        MyMappingFactory()
            throws IOException {
            _pools.add( "prod1" );
            _addrs.add( new InetSocketAddress( "local.10gen.cc" , _port ) );
        }
        
        public long refreshRate(){
            return 1000 * 1000;
        }
        
        public Mapping getMapping(){
            return new Mapping(){

                public Environment getEnvironment( HttpRequest request ){
                    return new Environment( "shopwiki" , "www" );
                }

                public String getPool( Environment e ){
                    return _pools.get(0);
                }

                public String getPool( HttpRequest request ){
                    return _pools.get(0);
                }

                public List<InetSocketAddress> getAddressesForPool( String poolName ){
                    return _addrs;
                }
                
                public List<String> getPools(){
                    return _pools;
                }
                
                public String toFileConfig(){
                    throw new RuntimeException( "blah" );
                }
                
                public boolean reject( HttpRequest request ){
                    return false;
                }

            };
        }
        
        List<InetSocketAddress> _addrs = new ArrayList<InetSocketAddress>();
        List<String> _pools = new ArrayList<String>();
        
    }
    
    final LB _lb;
    
    final int _lbPort = 10002;

    public static void main(String args[])
            throws IOException {
        (new LBFullTest()).runConsole();
    }
    
}
