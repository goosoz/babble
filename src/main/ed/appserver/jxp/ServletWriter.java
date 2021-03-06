// ServletWriter.java

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

package ed.appserver.jxp;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.Math;

import ed.js.*;
import ed.util.*;
import ed.js.engine.*;
import ed.js.func.*;
import ed.lang.*;

import ed.appserver.*;
import ed.net.httpserver.*;

/**
 * @expose
 * @docmodule system.HTTP.print
 */
public class ServletWriter extends JSFunctionCalls1 {

    public static final int MAX_WRITTEN_LENGTH = 1024 * 1024 * 15;

    public ServletWriter( JxpWriter writer , String cdnPrefix , String cdnSuffix , AppContext context ){
        this( writer , new URLFixer( cdnPrefix , cdnSuffix , context ) );
    }

    public ServletWriter( JxpWriter writer , URLFixer fixer ){
        _writer = writer;
        _fixer = fixer;

        if ( _writer == null )
            throw new NullPointerException( "writer can't be null" );

        set( "setFormObject" , new JSFunctionCalls1(){
                public Object call( Scope scope , Object o , Object extra[] ){
                    if ( o == null ){
                        _formInput = null;
                        return null;
                    }

                    if ( ! ( o instanceof JSObject ) )
                        throw new RuntimeException( "must be a JSObject" );

                    _formInput = (JSObject)o;
                    _formInputPrefix = null;

                    if ( extra != null && extra.length > 0 )
                        _formInputPrefix = extra[0].toString();

                    return o;
                }
            } );
    }

    public Writer asJavaWriter(){
        return new Writer(){
            public void close(){
                return;
            }

            public void flush(){
                return;
            }

            public void write(char[] cbuf, int off, int len){
                ServletWriter.this.print( new String( cbuf , off , len ) );
            }
        };
    }

    public void resetBuffer(){
        _writer.reset();
    }

    public Object get( Object n ){
        if ( "cdnPrefix".equals( n ) )
            return _fixer.getCDNPrefix();
        if ( "cdnSuffix".equals( n ) )
            return _fixer.getCDNSuffix();
        return super.get( n );
    }

    public Object set( Object n , Object v ){
        if ( "cdnPrefix".equals( n ) )
            return _fixer.setCDNPrefix( v.toString() );
        if ( "cdnSuffix".equals( n ) )
            return _fixer.setCDNSuffix( v.toString() );
        return super.set( n  , v );
    }

    public Object call( Scope scope , Object o , Object extra[] ){
        if ( o == null )
            print( "null" );
        else
            print( JSInternalFunctions.JS_toString( o ) );

        return null;
    }

    /**
     * tag handlers are called for every tag of that name that gets printed during your request
     * it is case insensitive
     * if the handler returns null, normall processing ensues
     * if it returns something non-null, it prints that instead
     */
    public void addTagHandler( String name , JSFunction handler ){
        if ( _tagHandlers == null )
            _tagHandlers = new StringMap<JSFunction>();
        _tagHandlers.put( name , handler );
    }

    public void print( String s ){
        print( s , true );
    }

    public void print( String s , boolean allowTagHandlers ){

        if ( ( _writtenLength += s.length() ) > MAX_WRITTEN_LENGTH )
            throw new RuntimeException( "trying to write a dynamic page more than " + MAX_WRITTEN_LENGTH + " chars long" );

        if ( _writer.closed() )
            throw new RuntimeException( "output closed.  are you using an old print function" );

        while ( s.length() > 0 ){
            if ( _extra.length() > 0 ){
                _extra.append( s );
                s = _extra.toString();
                _extra.setLength( 0 );
            }

            // if it's in a script tag just print it.
            // to find the end of the script tag we have to
            // ignore anything in quotes
            if (this._inScript) {
                s = readThroughScript( s );
                if ( s == null )
                    return;
                continue;
            }

            _matcher.reset( s );
            if ( ! _matcher.find() ){
                if ( s.endsWith( "<" ) ){
                    _extra.append( s );
                    return;
                }
                _writer.print( s );
                return;
            }

            _writer.print( s.substring( 0 , _matcher.start() ) );

            s = s.substring( _matcher.start() );
            int end = endOfTag( s );
            if ( end == -1 ){
                _extra.append( s );
                return;
            }

            String wholeTag = s.substring( 0 , end + 1 );

            boolean isClosed = (wholeTag.charAt(end - 1) == '/');

            if ( ! printTag( _matcher.group(1) , wholeTag , allowTagHandlers , isClosed ) )
                _writer.print( wholeTag );

            s = s.substring( end + 1 );
        }

    }

    /**
     * @return null if the entire thing is in script, or the rest
     */
    private String readThroughScript( String s ){
        assert( this._inScript );

        if (this._inDoubleQuote) {
            int dq = findDoubleQuote(s);
            if (dq == -1) {
                _writer.print(s);
                return null;
            }
            this._inDoubleQuote = false;
            _writer.print(s.substring(0, dq));
            s = s.substring(dq);
        } else if (this._inSingleQuote) {
            int sq = findSingleQuote(s);
            if (sq == -1) {
                _writer.print(s);
                return null;
            }
            this._inSingleQuote = false;
            _writer.print(s.substring(0, sq));
            s = s.substring(sq);
        } else if (this._inComment) {
            int c = findCloseComment(s);
            if (c == -1) {
                _writer.print(s);
                return null;
            }
            this._inComment = false;
            _writer.print(s.substring(0, c));
            s = s.substring(c);
        }

        int doubleQuote = s.indexOf('"');
        int singleQuote = s.indexOf('\'');
        int longComment = s.indexOf("/*");
        int shortComment = s.indexOf("//");

        int stateMin = singleQuote;
        if (doubleQuote != -1) {
            stateMin = (stateMin == -1 || doubleQuote < stateMin) ? doubleQuote : stateMin;
        }
        if (longComment != -1) {
            stateMin = (stateMin == -1 || longComment < stateMin) ? longComment : stateMin;
        }
        if (shortComment != -1) {
            stateMin = (stateMin == -1 || shortComment < stateMin) ? shortComment : stateMin;
        }

        _closeScriptMatcher.reset(s);

        if (_closeScriptMatcher.find() && (_closeScriptMatcher.start() < stateMin || stateMin == -1)) {
            this._inScript = false;

            _writer.print(s.substring(0, _closeScriptMatcher.start()));
            s = s.substring(_closeScriptMatcher.start());
            return s;
        }


        if (stateMin != -1 && stateMin != shortComment) {
            this.setState(doubleQuote, singleQuote, longComment);
            _writer.print(s.substring(0, stateMin + 1));
            s = s.substring(stateMin + 1);
            return s;
        } else if (stateMin != -1) {
            _writer.print(s.substring(0, stateMin + 1));
            s = s.substring(stateMin + 1);
            int nl = s.indexOf('\n');
            if (nl != -1) {
                _writer.print(s.substring(0, nl + 1));
                s = s.substring(nl + 1);
                return s;
            }
        }

        _writer.print(s);
        return null;
    }

    private void setState (int doubleQuote, int singleQuote, int comment) {
        doubleQuote = (doubleQuote != -1) ? doubleQuote : Integer.MAX_VALUE;
        singleQuote = (singleQuote != -1) ? singleQuote : Integer.MAX_VALUE;
        comment = (comment != -1) ? comment : Integer.MAX_VALUE;

        int min = Math.min(doubleQuote, Math.min(singleQuote, comment));
        if (min == Integer.MAX_VALUE) {
            return;
        }

        if (doubleQuote == min) {
            this._inDoubleQuote = true;
        } else if (singleQuote == min) {
            this._inSingleQuote = true;
        } else {
            this._inComment = true;
        }
    }

    private int findDoubleQuote (String s) {
        this._closeDoubleQuoteMatcher.reset(s);
        if (!_closeDoubleQuoteMatcher.find()) {
            return -1;
        }
        return _closeDoubleQuoteMatcher.start() + _closeDoubleQuoteMatcher.group(1).length();
    }

    private int findSingleQuote (String s) {
        this._closeSingleQuoteMatcher.reset(s);
        if (!_closeSingleQuoteMatcher.find()) {
            return -1;
        }
        return _closeSingleQuoteMatcher.start() + _closeSingleQuoteMatcher.group(1).length();
    }

    private int findCloseComment (String s) {
        return s.indexOf("*/");
    }

    /**
     * @return true if i printed tag so you should not
     */
    boolean printTag( String tag , String s , boolean allowTagHandlers , boolean isClosed ){

        if ( tag == null )
            throw new NullPointerException( "tag can't be null" );
        if ( s == null )
            throw new NullPointerException( "show tag can't be null" );

        if ( tag.equalsIgnoreCase( "/head" ) && ! _writer.hasSpot() ){
            _writer.saveSpot();
            return false;
        }

        if ( allowTagHandlers && _tagHandlers != null ){
            JSFunction func = _tagHandlers.get( tag );
            if ( func != null ){
                Object res = func.call( func.getScope() , new JSString( s ) );
                if ( res != null ){
                    print( res.toString() , false );
                    return true;
                }
            }
        }

        if (tag.equalsIgnoreCase("script") && !isClosed) {
            this._inScript = true;
        }

        { // CDN stuff
            String srcName = null;
            if ( tag.equalsIgnoreCase( "img" ) ||
                 tag.equalsIgnoreCase( "script" ) )
                srcName = "src";
            else if ( tag.equalsIgnoreCase( "link" ) ){
                srcName = "href";
                Matcher m = _attributeMatcher( "type" , s );
                if ( m.find() ){
                    String type = m.group(1);
                    if ( type.contains( "rss" ) || type.contains( "atom" ) )
                        srcName = null;
                }
            }

            if ( srcName != null ){

                s = s.substring( 2 + tag.length() );

                Matcher m = _attributeMatcher( srcName , s );
                if ( ! m.find() )
                    return false;

                _writer.print( "<" );
                _writer.print( tag );
                _writer.print( " " );

                _writer.print( s.substring( 0 , m.start(1) ) );
                String src = m.group(1);

                printSRC( src );

                _writer.print( s.substring( m.end(1) ) );

                return true;
            }

        }

        if ( _formInput != null && tag.equalsIgnoreCase( "input" ) ){
            Matcher m = Pattern.compile( "\\bname *= *['\"](.+?)[\"']" ).matcher( s );

            if ( ! m.find() )
                return false;

            String name = m.group(1);
            if ( name.length() == 0 )
                return false;

            if ( _formInputPrefix != null )
                name = name.substring( _formInputPrefix.length() );

            Object val = _formInput.get( name );
            if ( val == null )
                return false;

            if ( s.toString().matches( "value *=" ) )
                return false;

            _writer.print( s.substring( 0 , s.length() - 1 ) );
            _writer.print( " value=\"" );
            _writer.print( HtmlEscape.escape( val.toString() ) );
            _writer.print( "\" >" );

            return true;
        }

        return false;
    }

    /**
     * takes the actual src of the asset and fixes and prints
     * i.e. /foo -> static.com/foo
     */
    void printSRC( String src ){

        if ( src == null || src.length() == 0 )
            return;

        _fixer.fix( src , _writer );
    }

    int endOfTag( String s ){
        for ( int i=0; i<s.length(); i++ ){
            char c = s.charAt( i );
            if ( c == '>' )
                return i;

            if ( c == '"' || c == '\'' ){
                for ( ; i<s.length(); i++)
                    if ( c == s.charAt( i ) )
                        break;
            }
        }
        return -1;
    }

    static Matcher _attributeMatcher( String name , String tag ){
        Pattern p = _attPatternCache.get( name );
        if ( p == null ){
            p = Pattern.compile( name + " *= *['\"](.+?)['\"]" , Pattern.CASE_INSENSITIVE );
            _attPatternCache.put( name , p );
        }
        return p.matcher( tag );
    }

    static final Pattern _tagPattern = Pattern.compile( "<(/?\\w+)[ >]" );
    static final Map<String,Pattern> _attPatternCache = Collections.synchronizedMap( new HashMap<String,Pattern>() );
    final Matcher _matcher = _tagPattern.matcher("");
    final Matcher _closeScriptMatcher = Pattern.compile("</\\s*script\\s*>", Pattern.CASE_INSENSITIVE).matcher("");
    final Matcher _closeDoubleQuoteMatcher = Pattern.compile("([^\\\\]\"|^\")").matcher("");
    final Matcher _closeSingleQuoteMatcher = Pattern.compile("([^\\\\]'|^')").matcher("");

    final StringBuilder _extra = new StringBuilder();

    final JxpWriter _writer;
    final URLFixer _fixer;

    JSObject _formInput = null;
    String _formInputPrefix = null;

    int _writtenLength = 0;

    boolean _inScript = false;
    boolean _inDoubleQuote = false;
    boolean _inSingleQuote = false;
    boolean _inComment = false;

    Map<String,JSFunction> _tagHandlers;
}
