// JSString.java

package ed.js;

import ed.js.func.*;
import ed.js.engine.*;

public class JSString extends JSObjectBase {

    static JSFunction _cons = new JSFunctionCalls0(){

            public Object call( Scope s , Object[] args ){
                throw new RuntimeException( "can't be here" );
            }

            protected void init(){

                _prototype.set( "charCodeAt" , new JSFunctionCalls1() {
                        public Object call( Scope s , Object o , Object foo[] ){
                            String str = s.getThis().toString();
                            int idx = ((Number)o).intValue();
                            return Integer.valueOf( str.charAt( idx ) );
                        }
                    } );

                
                _prototype.set( "charAt" , new JSFunctionCalls1() {
                        public Object call( Scope s , Object o , Object foo[] ){
                            String str = s.getThis().toString();
                            int idx = ((Number)o).intValue();
                            return str.substring( idx , idx + 1 );
                        }
                    } );

            }
        };
    
    public JSString( String s ){
        super( _cons );
        _s = s;
    }
    
    public Object get( Object name ){
        
        if ( name instanceof JSString )
            name = name.toString();
        
        if ( name instanceof String && name.toString().equals( "length" ) )
            return Integer.valueOf( _s.length() );

        return super.get( name );
    }
    
    public String toString(){
        return _s;
    }
    
    public int hashCode(){
        return _s.hashCode();
    }

    public boolean equals( Object o ){

        if ( o == null )
            return _s == null;
        
        if ( _s == null )
            return false;
        
        return _s.equals( o.toString() );
    }

    String _s;
}
