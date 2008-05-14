// Encoding.java

package ed.js;

import bak.pcj.map.*;
import bak.pcj.set.*;

import ed.js.func.*;
import ed.js.engine.*;

public class Encoding {

    public static JSFunction escape = new JSFunctionCalls1(){
            public Object call( Scope s , Object o , Object [] extra ){
                return _escape( o.toString() , _noEscaping );
            }
        };

    public static JSFunction encodeURI = new JSFunctionCalls1(){
            public Object call( Scope s , Object o , Object [] extra ){
                return _escape( o.toString() , _noEncoding );
            }
        };

    public static JSFunction encodeURIComponent = new JSFunctionCalls1(){
            public Object call( Scope s , Object o , Object [] extra ){
                return _escape( o.toString() , _noEncodingComonent );
            }
        };
    
    static final String _escape( final String str , final CharSet skip ){
        final StringBuilder buf = new StringBuilder( (int)(str.length() * 1.5) );
        final int len = str.length();
        
        for ( int i=0; i<len; i++ ){
            char c = str.charAt( i );
            int val = (int)c;
            
            if ( Character.isLetterOrDigit( c ) )
                buf.append( c );
            else if ( c == ' ' )
                buf.append( "%20" );
            else if ( skip.contains( c ) )
                buf.append( c );
            else if ( val < 255 )
                buf.append( "%" )
                    .append( _forDigit( ( val >> 4 ) & 0xF ) )
                    .append( _forDigit( val & 0xF ) );
        }
        
        return buf.toString();
    }
    
    final static char _forDigit( int val ){
        char c = Character.forDigit( val , 16 );
        if ( c >= 'a' && c <= 'z' )
            c += _upperDiff;
        return c;
    }

    public static JSFunction unescape = new JSFunctionCalls1(){
            public Object call( Scope s , Object o , Object [] extra ){
                return _unescape( o.toString() );
            }
        };

    static final String _unescape( final String str ){
        final StringBuilder buf = new StringBuilder( str.length() );

        final int len = str.length();
        final int max = len - 2;
        
        int i=0;
        for ( ; i<max; i++ ){
            char c = str.charAt( i );
            
            if ( c != '%' ){
                buf.append( c );
            }
            else {
                final String foo = str.substring( i + 1 , i + 3 );
                if ( ! _isHex( foo ) )
                    buf.append( c );
                else {
                    final int val = Integer.parseInt( foo , 16 );
                    buf.append( (char)val );
                    i += 2;
                }
            }
            
        }
        
        for ( ; i<len; i++ )
            buf.append( str.charAt( i ) );

        return buf.toString();
    }

    static final boolean _isHex( final String s ){
        final int len = s.length();
        for ( int i=0; i<len; i++ )
            if ( ! _isHex( s.charAt( i ) ) )
                return false;
        return true;
    }

    static final boolean _isHex( final char c ){
        if ( c >= '0' && c <= '9' )
            return true;

        if ( c >= 'a' && c <= 'f' )
            return true;

        if ( c >= 'A' && c <= 'F' )
            return true;
        
        return false;
    }

    static final int _upperDiff = 'A' - 'a';

    static final CharSet _noEscaping = new CharOpenHashSet();
    static final CharSet _noEncoding = new CharOpenHashSet();
    static final CharSet _noEncodingComonent = new CharOpenHashSet();

    static {
        _noEscaping.add( '@' );
        _noEscaping.add( '*' );
        _noEscaping.add( '-' );
        _noEscaping.add( '_' );
        _noEscaping.add( '+' );
        _noEscaping.add( '/' );
        _noEscaping.add( '.' );

        _noEncoding.add( '~' );
        _noEncoding.add( '!' );
        _noEncoding.add( '@' );
        _noEncoding.add( '#' );
        _noEncoding.add( '$' );
        _noEncoding.add( '&' );
        _noEncoding.add( '*' );
        _noEncoding.add( '(' );
        _noEncoding.add( ')' );
        _noEncoding.add( '-' );
        _noEncoding.add( '_' );
        _noEncoding.add( '+' );
        _noEncoding.add( '=' );
        _noEncoding.add( ';' );
        _noEncoding.add( ':' );
        _noEncoding.add( '\'' );
        _noEncoding.add( '/' );
        _noEncoding.add( '?' );
        _noEncoding.add( '.' );
        _noEncoding.add( ',' );


        _noEncodingComonent.add( '~' );
        _noEncodingComonent.add( '!' );
        _noEncodingComonent.add( '*' );
        _noEncodingComonent.add( '(' );
        _noEncodingComonent.add( ')' );
        _noEncodingComonent.add( '-' );
        _noEncodingComonent.add( '_' );
        _noEncodingComonent.add( '\'' );
        _noEncodingComonent.add( '.' );
    }
    
    public static JSFunction escapeHTML = new JSFunctionCalls1(){
            public Object call( Scope s , Object o , Object [] extra ){
                if ( o == null )
                    return null;
                return new JSString( _escapeHTML( o.toString() ) );
            }
        };

    public static String _escapeHTML( String html ){
        final StringBuilder buf = new StringBuilder( (int)(html.length() * 1.2) );
        final int max = html.length();
        for ( int i=0; i<max; i++ ){
            char c = html.charAt( i );

            switch( c ){
            case '<': buf.append( "&lt;" ); break;
            case '>': buf.append( "&gt;" ); break;
            case '&': buf.append( "&amp;" ); break;
            case '"': buf.append( "&quot;" ); break;
            case '\'': buf.append( "&apos;" ); break;
            default: buf.append( c ); break;
            }
        }
        return buf.toString();
    }
    

    public static void install( Scope s ){
        s.set( "escape" , escape );
        s.set( "encodeURI"  , encodeURI );
        s.set( "encodeURIComponent" , encodeURIComponent );
        
        s.set( "unescape" , unescape );
        s.set( "decodeURI" , unescape );
        s.set( "decodeURIComponent" , unescape );


        s.set( "escapeHTML" , escapeHTML );
    }
    
}
