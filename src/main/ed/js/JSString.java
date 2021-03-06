// JSString.java

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

package ed.js;

import java.util.*;
import java.util.regex.*;

import com.twmacinta.util.*;

import ed.util.*;
import ed.js.func.*;
import ed.js.engine.*;

/** @expose  */
public class JSString extends JSObjectBase implements Comparable {

    static { JS._debugSIStart( "JSString" ); }
    static { JS._debugSI( "JSString" , "0" ); }

    public static class JSStringCons extends JSFunctionCalls1{

        public JSObject newOne(){
            return new JSString("");
        }

        public Object call( Scope s , Object[] args ){
            return new JSString( "" );
        }

        public Object call( Scope s , Object fooO, Object[] args ){
            JSString foo = new JSString( fooO + "" );

            Object o = s.getThis();
            if ( o == null || ! ( o instanceof JSString ) )
                return foo;
            
            JSString str = (JSString)o;
            if ( foo != null )
                str._s = foo.toString();
            else 
                str._s = "";

            return str;
        }

        protected void init(){

            JS._debugSI( "JSString" , "JSStringCons init 0" );

            final JSObject myPrototype = _prototype;

            if ( ! JS.JNI ){
                final StringEncrypter encrypter = new StringEncrypter( "knsd8712@!98sad" );

                _prototype.set( "encrypt" , new JSFunctionCalls0(){
                        public Object call( Scope s , Object foo[] ){
                            synchronized ( encrypter ){
                                return new JSString( encrypter.encrypt( s.getThis().toString() ) );
                            }
                        }
                    } );

                _prototype.set( "decrypt" , new JSFunctionCalls0(){
                        public Object call( Scope s , Object foo[] ){
                            synchronized ( encrypter ){
                                return new JSString( encrypter.decrypt( s.getThis().toString() ) );
                            }
                        }
                    } );
            }


            JS._debugSI( "JSString" , "JSStringCons init 1" );

            _prototype.set( "trim" , new JSFunctionCalls0() {
                    public Object call( Scope s , Object foo[] ){
                        return new JSString( s.getThis().toString().trim() );
                    }
                } );


            _prototype.set( "md5" , new JSFunctionCalls0() {
                    public Object call( Scope s , Object extra[] ){
                        synchronized ( _myMd5 ){
                            _myMd5.Init();
                            _myMd5.Update( s.getThis().toString() );
                            return new JSString( _myMd5.asHex() );
                        }
                    }

                    private final MD5 _myMd5 = new MD5();
                } );

            _prototype.set( "to_sym" , new JSFunctionCalls0() {
                    public Object call( Scope s , Object extra[] ){
                        return s.getThis().toString();
                    }

                    private final MD5 _myMd5 = new MD5();
                } );

            _prototype.set( "toString" , new JSFunctionCalls0() {
                    public Object call( Scope s , Object extra[] ) {
                        Object o = s.getThis();
                        if( !( o instanceof JSString ) ) 
                            if ( o == myPrototype )
                                return new JSString( "" );
                            else
                                throw new JSException( "String.prototype.toString can only be called on Strings" );
                        return new JSString( ((JSString)o)._s );
                    }
                } );
            
            _prototype.set( "toLowerCase" , new JSFunctionCalls0() {
                    public Object call( Scope s , Object foo[] ){
                        return new JSString( s.getThis().toString().toLowerCase() );
                    }
                } );

            _prototype.set( "toUpperCase" , new JSFunctionCalls0() {
                    public Object call( Scope s , Object foo[] ){
                        return new JSString( s.getThis().toString().toUpperCase() );
                    }
                } );


            _prototype.set( "charCodeAt" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        String str = s.getThis().toString();
                        int idx = (int)JSNumber.getDouble( o );
                        if( idx >= str.length() || idx < 0 )
                            return Double.NaN;
                        return Integer.valueOf( str.charAt( idx ) );
                    }
                } );


            _prototype.set( "charAt" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        String str = s.getThis().toString();
                        int idx = (int)JSNumber.getDouble( o );
                        if ( idx >= str.length() || idx < 0 )
                            return EMPTY;
                        return new JSString( str.substring( idx , idx + 1 ) );
                    }
                } );

            _prototype.set( "indexOf" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        String str = s.getThis().toString();
                        if( o == null )
                            return -1;
                        String thing = o.toString();

                        int start = 0;
                        if ( foo != null && foo.length > 0 && foo[0] != null ) {
                            start = JSNumber.getNumber( foo[0] ).intValue();
                        }
                        return str.indexOf( thing , start );
                    }
                } );

            _prototype.set( "contains" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        String str = s.getThis().toString();
                        String thing = o.toString();

                        return str.contains( thing );
                    }
                } );

            _prototype.set( "__rshift" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        JSString me = (JSString)(s.getThis());
                        String thing = o.toString();
                        me._s += thing;
                        return me;
                    }
                } );

            _prototype.set( "lastIndexOf" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        String str = s.getThis().toString();
                        String thing = o.toString();

                        int end = str.length();
                        if ( foo != null && foo.length > 0 )
                            end = ((Number)foo[0]).intValue();

                        return str.lastIndexOf( thing , end );
                    }
                } );

            _prototype.set( "startsWith" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        String str = s.getThis().toString();
                        String thing = o.toString();

                        return str.startsWith( thing );
                    }
                } );

            _prototype.set( "endsWith" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        String str = s.getThis().toString();
                        String thing = o.toString();

                        return str.endsWith( thing );
                    }
                } );

            _prototype.set( "substring" , new JSFunctionCalls2() {
                    public Object call( Scope s , Object startO , Object endO , Object foo[] ){
                        String str = s.getThis().toString();
                        if( endO == null || endO == VOID )
                            endO = str.length();

                        int len = str.length();
                        int temp1 = (int)Math.min( Math.max( JSNumber.getDouble( startO ), 0 ), len );
                        int temp2 = (int)Math.min( Math.max( JSNumber.getDouble( endO ), 0 ), len );
                        int start = Math.min( temp1, temp2 );
                        int end = Math.max( temp1, temp2 );

                        return new JSString( str.substring( start , end ) );
                    }
                } );

            _prototype.set( "substr" , new JSFunctionCalls2() {
                    public Object call( Scope s , Object startO , Object lengthO , Object foo[] ){
                        String str = s.getThis().toString();

                        int start = ((Number)startO).intValue();
                        if ( start < 0 )
                            start = 0;
                        if ( start >= str.length() || start < 0 )
                            return EMPTY;

                        int length = -1;
                        if ( lengthO != null && lengthO instanceof Number )
                            length = ((Number)lengthO).intValue();

                        if ( start + length > str.length() )
                            length = str.length() - start;

                        if ( length < 0 )
                            return new JSString( str.substring( start) );
                        return new JSString( str.substring( start , start + length ) );
                    }
                } );


            _prototype.set( "match" , new JSFunctionCalls1() {
                    public Object call( Scope s , Object o , Object foo[] ){
                        String str = s.getThis().toString();

                        if ( o instanceof String || o instanceof JSString )
                            o = new JSRegex( o.toString() , "" );

                        if ( ! ( o instanceof JSRegex ) )
                            throw new RuntimeException( "not a regex : " + o.getClass() );

                        JSRegex r = (JSRegex)o;
                        Matcher m = r._patt.matcher( str );
                        if ( ! m.find() )
                            return null;
                        if ( r.getFlags().contains( "g" )){
                            JSArray a = new JSArray();
                            do {
                                a.add(new JSString(m.group()));
                            } while(m.find());
                            return a;
                        }
                        else{
                            return r.exec(str);
                        }
                    }
                } );


            _prototype.set( "split" , new JSFunctionCalls2(){
                    public Object call( Scope s , Object o , Object limit, Object extra[] ){

                        String str = s.getThis().toString();

                        JSArray a = new JSArray();

                        if ( limit == null ) {
                            limit = Integer.MAX_VALUE;
                        }
                        else {
                            limit = StringParseUtil.parseStrict( limit.toString() );
                            if ( ((Number)limit).intValue() == 0 ) {
                                return a;
                            }
                        }

                        if ( o == null || o == VOID ) {
                            a.add( s.getThis() );
                            return a;
                        }

                        if ( o instanceof String || o instanceof JSString )
                            o = new JSRegex( JSRegex.quote( o.toString() ) , "" );
                        if ( ! ( o instanceof JSRegex ) )
                            throw new RuntimeException( "not a regex : " + o.getClass() );
                        JSRegex r = (JSRegex)o;

                        String spacer = null;
                        if ( r.getPattern().contains( "(" ) )
                            spacer = r.getPattern().replaceAll( "[()]" , "" );

                        for ( String pc : r._patt.split( str , -1 ) ){
                            if ( a.size() > 0 && spacer != null )
                                a.add( spacer );
                            a.add( new JSString( pc ) );
                            if ( a.size() >= ((Number)limit).intValue() )
                                break;
                        }

                        if ( r.getPattern().length() == 0 ){
                            a.remove( 0 );
                            if( a.size() > 0 ) {
                                a.remove( a.size() - 1 );
                            }
                        }

                        return a;
                    }
                }
                );

            _prototype.set( "reverse" , new JSFunctionCalls0(){
                    public Object call(Scope s, Object [] args){
                        String str = s.getThis().toString();
                        StringBuffer buf = new StringBuffer( str.length() );
                        for ( int i=str.length()-1; i>=0; i--)
                            buf.append( str.charAt( i ) );
                        return new JSString( buf.toString() );
                    }
                } );

            _prototype.set("slice" , new JSFunctionCalls2(){
                public Object call(Scope s, Object o1, Object o2, Object [] args){
                    String str = s.getThis().toString();
                    
                    int start = 0;
                    
                    if (o1 != null && o1 instanceof Number) {
                        start = ((Number)o1).intValue();
                        
                        if (start < 0) { 
                            start = str.length() + start;  // add as it's negative
                        }
                    }

		    if ( start < 0 )
			start = 0;

                    if (start >= str.length())
                        return EMPTY;

                    int end = str.length();
                    
                    if (o2 != null && o2 instanceof Number) {
                        end = ((Number)o2).intValue();
                        
                        if (end < 0) { 
                            end = str.length() + end;  // add as it's negative
			    if ( end < 0 )
				end = 0;
                        }
                        
                        if (end > str.length()) {
                            end = str.length();
                        }
			
			if ( end < start )
			    end = start;
                    }

                    return new JSString(str.substring(start, end));
                }
            } );


            _prototype.set( "pluralize" , new JSFunctionCalls0(){
                    public Object call(Scope s, Object [] args){
                        String str = s.getThis().toString();
                        return str + "s";
                    }
                } );

            _prototype.set( "_eq__t_" , _prototype.get( "match" ) );

            _prototype.set( "__delete" , new JSFunctionCalls1(){
                    public Object call(Scope s, Object all , Object [] args){

                        String str = s.getThis().toString();
                        if ( all == null )
                            return str;

                        String bad = all.toString();

                        StringBuffer buf = new StringBuffer( str.length() );

                        outer:
                        for ( int i=0; i<str.length(); i++ ){
                            char c = str.charAt( i );
                            for ( int j=0; j<bad.length(); j++ )
                                if ( bad.charAt( j ) == c )
                                    continue outer;

                            buf.append( c );
                        }


                        return new JSString( buf.toString() );
                    }
                } );

            _prototype.set( "_lb__rb_" , new JSFunctionCalls1(){
                    public Object call( Scope s , Object thing , Object args[] ){
                        JSString str = (JSString)s.getThis();
                        if ( thing instanceof Number ){
                            return (int)(str._s.charAt( ((Number)thing).intValue() ));
                        }

                        if ( thing instanceof JSArray ){
                            JSArray a = (JSArray)thing;
                            int start = ((Number)(a.get("start"))).intValue();
                            int end = ((Number)(a.get("end"))).intValue();
                            String sub = str._s.substring( start , end + 1 );
                            return new JSString( sub );
                        }

                        return null;
                    }
                } );

            _prototype.set( "each_byte" , new JSFunctionCalls1(){
                    public Object call(Scope s, Object funcObject , Object [] args){

                        if ( funcObject == null )
                            throw new NullPointerException( "each_byte needs a function" );

                        JSFunction func = (JSFunction)funcObject;

                        String str = s.getThis().toString();
                        for ( int i=0; i<str.length(); i++ ){
                            func.call( s , (int)str.charAt( i ) );
                        }
                        return null;
                    }
                } );


            _prototype.set( "replace" , new JSFunctionCalls2() {
                    public Object call( Scope s , Object o , Object repl , Object crap[] ){
                        String str = s.getThis().toString();

                        if ( o instanceof String || o instanceof JSString )
                            o = new JSRegex( JSRegex.quote( o.toString() ) , "" );

                        if ( ! ( o instanceof JSRegex ) )
                            throw new RuntimeException( "not a regex : " + o.getClass() );

                        JSRegex r = (JSRegex)o;
                        Matcher m = r._patt.matcher( str );

                        StringBuffer buf = null;
                        int start = 0;

                        final JSObject options = ( crap != null && crap.length > 0  && crap[0] instanceof JSObject ) ? (JSObject)crap[0] : null;
                        final boolean replaceAll = r._replaceAll || ( options != null && options.get( "all" ) != null );

                        Object replArgs[] = null;

                        while ( m.find() ){
                            if ( buf == null )
                                buf = new StringBuffer( str.length() );

                            buf.append( str.substring( start , m.start() ) );

                            if ( repl == null )
                                repl = "null";

                            if ( repl instanceof JSString || repl instanceof String){
                                String foo = repl.toString();
                                for ( int i=0; i<foo.length(); i++ ){
                                    char c = foo.charAt( i );

                                    if ( c != '$' ){
                                        buf.append( c );
                                        continue;
                                    }

                                    if ( i + 1 >= foo.length() ||
                                         ! Character.isDigit( foo.charAt( i + 1 ) ) ){
                                        buf.append( c );
                                        continue;
                                    }

                                    i++;
                                    int end = i;
                                    while ( end < foo.length() && Character.isDigit( foo.charAt( end ) ) )
                                        end++;

                                    int num = Integer.parseInt( foo.substring( i , end ) );
                                    buf.append( m.group( num ) == null ? "" : m.group( num ) );

                                    i = end - 1;
                                }
                            }
                            else if ( repl instanceof JSFunction ){
                                if ( replArgs == null )
                                    replArgs = new Object[ m.groupCount() + 1 ];
                                for ( int i=0; i<replArgs.length; i++ )
                                    replArgs[i] = new JSString( m.group( i ) );
                                buf.append( ((JSFunction)repl).call( s , replArgs ) );
                            }
                            else {
                                throw new RuntimeException( "can't use replace with : " + repl.getClass() );
                            }

                            start = m.end();

                            if ( ! replaceAll )
                                break;
                        }

                        if ( buf == null )
                            return new JSString( str );

                        buf.append( str.substring( start ) );
                        return new JSString( buf.toString() );
                    }
                } );

            _prototype.set( "sub" , _prototype.get( "replace" ) );
            final JSObjectBase gsubOptions = new JSObjectBase();
            gsubOptions.set( "all" , "asd" );
            final Object gsubOptionsArray[] = new Object[]{ gsubOptions };

            _prototype.set( "gsub" , new JSFunctionCalls2() {
                    public Object call( Scope s , Object o , Object repl , Object crap[] ){
                        return ((JSFunction)myPrototype.get( "replace" )).call( s , o , repl , gsubOptionsArray );
                    }
                } );

            _prototype.set( "valueOf" , new JSFunctionCalls0() {
                    public Object call( Scope s , Object crap[] ){
                        Object o = s.getThis();
                        if( o == myPrototype ) 
                            return new JSString( "" );
                    
                        return new JSString( ((JSString)o).toString() );
                    }
                } );

            set("fromCharCode", new JSFunctionCalls0() {
                    public Object call(Scope s, Object [] args){
                        if(args == null) return new JSString("");
                        StringBuffer buf = new StringBuffer();
                        for(int i = 0; i < args.length; i++){
                            buf.append( (char)JSNumber.toUint16( args[i] ) );
                        }
                        return new JSString( buf.toString() );
                    }
                } );

            set( "isUpper" , new JSFunctionCalls1(){
                    public Object call( Scope s , Object thing , Object args[] ){
                        String str = thing.toString();
                        for ( int i=0; i<str.length(); i++ ){
                            char c = str.charAt( i );
                            if ( Character.isLetter( c ) &&
                                 Character.isUpperCase( c ) )
                                continue;
                            return false;
                        }
                        return true;
                    }
                    
                } );

            _prototype.dontEnumExisting();
        }
    };

    static { JS._debugSI( "JSString" , "1" ); }
    private static JSFunction _cons = new JSStringCons();
    static { JS._debugSI( "JSString" , "2" ); }

    /** The empty string: "" */
    static JSString EMPTY = new JSString("");

    /** Initializes a new string
     * @param The value for the string.
     */
    public JSString( String s ){
        super( Scope.getThreadLocalFunction( "String", _cons , true  ) );
        _s = s;
    }

    /** Initializes a new string from an array of characters.
     * @param The value for the string.
     */
    public JSString( char [] c ){
        this(new String(c));
    }

    /** Gets the property of this string with the key <tt>name</tt>.
     * @param name The name of the property
     */
    public Object get( Object name ){

        if ( name instanceof JSString )
            name = name.toString();

        if ( name instanceof String && name.toString().equals( "length" ) )
            return Integer.valueOf( _s.length() );

        return super.get( name );
    }

    /** Returns this string.
     * @return This string.
     */
    public String toString(){
        return _s;
    }

    public String toPrettyString(){
        return _s;
    }

    /** Compare this string with an object. The empty string matches null.
     * @param o The object with which to compare this string.
     * @return 1, -1, or 0 if this string is semantically greater than, less than, or equal to the given object.
     */
    public int compareTo( Object o ){
	if ( o == null ) o = "";
	return _s.compareTo( o.toString() );
    }

    /** Return the length of this string.
     * @return The length of this string.
     */
    public int length(){
        return _s.length();
    }

    /** The character at the given position in this string.
     * @param n Index of the character.
     * @return A single-character string
     */
    public Object getInt( int n ){
        if ( n >= _s.length() )
            return null;
        // Eliot said that we should map characters to objects in scope.java
        return new JSString(new char[]{_s.charAt( n )});
    }

    /** The hash code value of this array.
     * @return The hash code value of this array.
     */
    public int hashCode( IdentitySet seen ){
        return _s.hashCode();
    }

    /** Test equality between this string and an object.
     * @param Object to which to compare this string.
     * @return If object is equal to this string.
     */
    public boolean equals( Object o ){

        if ( o == null )
            return _s == null;

        if ( _s == null )
            return false;

        return _s.equals( o.toString() );
    }

    /** Returns this string as an array of bytes.
     * @return This string as an array of bytes.
     */
    public byte[] getBytes(){
        return _s.getBytes();
    }

    public Set<String> keySet( boolean includePrototype ){
        Set<String> keys = new OrderedSet<String>();
        for ( int i=0; i<_s.length(); i++ )
            keys.add( String.valueOf( i ) );

        keys.addAll( super.keySet( includePrototype ) );
        return keys;
    }

    private void setObj( boolean b ) {
        isObj = b;
    }

    public boolean isObj() {
        return isObj;
    }

    String _s;

    public static class Symbol extends JSString {
        public Symbol( String s ){
            super( s );
        }
    }

    static { JS._debugSIDone( "JSString" ); }
    private boolean isObj = false;
}
