// CGIGateway.java

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

package ed.lang.cgi;

import java.io.*;
import java.util.*;

import ed.js.*;
import ed.js.func.*;
import ed.js.engine.*;
import ed.net.httpserver.*;
import ed.appserver.jxp.*;
import ed.appserver.*;


/**
 *  Utility class that invokes a Babble-supported
 *  script in a CGI environment.
 */
public abstract class CGIGateway extends JxpSource {

    public CGIGateway(){
    }

    public abstract void handle( EnvMap env , InputStream stdin , OutputStream stdout , AppRequest ar );
    
    public JSFunction getFunction(){
        return _dontCall;
    }

    public JxpServlet getServlet( AppContext context ){
        if ( _servlet == null )
            _servlet = new MyServlet( context );
        return _servlet;
    }

    public void handle( AppRequest ar ){
        EnvMap env = makeCGIDict( ar.getRequest() );

        InputStream stdin = null;
        if ( ar.getRequest().getPostData() != null )
            stdin = ar.getRequest().getPostData().getInputStream();
        else 
            stdin = new ByteArrayInputStream( new byte[0] );
        
        OutputStream stdout = new CGIOutputStream( ar.getResponse() , ar.getResponse().getOutputStream() );

        handle( env , stdin , stdout , ar );
    }

    /**
     *  Parses the request into a map
     *
     *  Copied from Jetty's CGI servlet. Kudos to Greg and Jan!
     *  Following is under the Apache License
     *
     *  Lots of work needs to be done here
     *
     * @param req request object
     * @return map CGI map
     */
    public EnvMap makeCGIDict(HttpRequest req) {

        int len=req.getContentLength();

        if (len<0) {
            len=0;
        }

        EnvMap env=new EnvMap();

        // these ones are from "The WWW Common Gateway Interface Version 1.1"
        // look at :
        // http://Web.Golux.Com/coar/cgi/draft-coar-cgi-v11-03-clean.html#6.1.1

        env.set("AUTH_TYPE",req.getAuthType());
        env.set("CONTENT_LENGTH",Integer.toString(len));
        env.set("CONTENT_TYPE",req.getContentType());
        env.set("GATEWAY_INTERFACE","CGI/1.1");

        env.set("PATH_INFO", req.getPathInfo());

        env.set("PATH_TRANSLATED","/");   // TODO - fix
        env.set("QUERY_STRING",req.getQueryString());
        env.set("REMOTE_ADDR",req.getRemoteAddr());
        env.set("REMOTE_HOST",req.getRemoteHost());

        // The identity information reported about the connection by a
        // RFC 1413 [11] request to the remote agent, if
        // available. Servers MAY choose not to support this feature, or
        // not to request the data for efficiency reasons.
        // "REMOTE_IDENT" => "NYI"
        env.set("REMOTE_USER",req.getRemoteUser());
        env.set("REQUEST_METHOD",req.getMethod());
        env.set("SCRIPT_NAME","");       // TODO - fix

        env.set("SCRIPT_FILENAME","###FIXME2###");   // TODO -fix

        env.set("SERVER_NAME",req.getServerName());
        env.set("SERVER_PORT",Integer.toString(req.getServerPort()));
        env.set("SERVER_PROTOCOL",req.getProtocol());
        env.set("SERVER_SOFTWARE","Development/1.0");

        Enumeration enm=req.getHeaderNames();

        while (enm.hasMoreElements())
        {
            String name=(String)enm.nextElement();
            String value=req.getHeader(name);
            env.set("HTTP_"+name.toUpperCase().replace('-','_'),value);
        }

        // these extra ones were from printenv on www.dev.nomura.co.uk
        env.set("HTTPS",(req.isSecure()?"ON":"OFF"));

        // "DOCUMENT_ROOT" => root + "/docs",
        // "SERVER_URL" => "NYI - http://us0245",
        // "TZ" => System.getProperty("user.timezone"),

        // are we meant to decode args here ? or does the script get them
        // via PATH_INFO ? if we are, they should be decoded and passed
        // into exec here...

        return env;
    }


    public static class CGIOutputStream extends OutputStream {
        
        public CGIOutputStream( HttpResponse response ){
            this( response , response.getOutputStream() );
        }
        
        public CGIOutputStream( HttpResponse response , OutputStream body ){
            _response = response;
            _body = body;
        }
        
        public void write(byte[] b)
            throws IOException {
            if ( _inHeader )
                super.write( b );
            else 
                _body.write( b );
        }
        
        public void write(byte[] b, int off, int len)
            throws IOException {
            if ( _inHeader )
                super.write( b , off , len );
            else 
                _body.write( b , off , len );
        }

        public void write( int i )
            throws IOException {

            if ( ! _inHeader ){
                _body.write( i );
                return;
            }
            
            byte b = (byte)(i & 0xFF);
            if ( b == '\r' )
                return;
            
            if ( b != '\n' ){
                _line.append( (char)b );
                return;
            }
            
            if ( _line.length() == 0 ){
                _inHeader = false;
                return;
            }
            
            String s = _line.toString();
            int idx = s.indexOf( ":" );
            if ( idx < 0 )
                throw new RuntimeException( "invalid cgi header line [" + s + "]" );
            
            _response.addHeader( s.substring( 0 , idx ).trim() , s.substring( idx + 1 ).trim() );
            _line.setLength( 0 );
        }
        
        final OutputStream _body;
        final HttpResponse _response;

        private boolean _inHeader = true;
        private StringBuilder _line = new StringBuilder();
    }
    
    class MyServlet extends JxpServlet {
        MyServlet( AppContext context ){
            super( context , _dontCall );
        }

        public void handle( HttpRequest request , HttpResponse response , AppRequest ar ){
            CGIGateway.this.handle( ar );
        }
    }

    protected String getContent(){
        throw new RuntimeException( "not supported" );
    }
    
    protected InputStream getInputStream(){
        throw new RuntimeException( "not supported" );
    }

    private MyServlet _servlet = null;
    private static final JSFunction _dontCall = new JSFunctionCalls0(){
            public Object call( Scope s , Object extra[] ){
                throw new RuntimeException( "don't call me" );
            }
        };
}
