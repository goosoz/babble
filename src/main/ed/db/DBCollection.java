// DBCollection.java

package ed.db;

import java.util.*;
import java.lang.reflect.*;

import ed.js.*;
import ed.js.func.*;
import ed.js.engine.*;

public abstract class DBCollection extends JSObjectLame {
    
    public abstract JSObject save( JSObject o );
    public abstract JSObject update( JSObject q , JSObject o , boolean upsert , boolean apply );

    protected abstract ObjectId doapply( JSObject o );
    public abstract int remove( JSObject id );
    
    public abstract JSObject find( ObjectId id );    
    public abstract Iterator<JSObject> find( JSObject ref , JSObject fields , int numToReturn );

    public abstract void ensureIndex( JSObject keys , String name );

    // ------

    public void ensureIndex( JSObject keys ){
        ensureIndex( keys , genIndexName( keys ) );
    }

    public String genIndexName( JSObject keys ){
        String name = "";
        for ( String s : keys.keySet() ){
            if ( name.length() > 0 )
                name += "_";
            name += s + "_" + keys.get( s ).toString().replace( ' ' , '_' );
        }
        return name;
    }

    public Iterator<JSObject> find( JSObject ref ){
        return find( ref , null , 0 );
    }

    public ObjectId apply( Object o ){

        if ( ! ( o instanceof JSObject ) )
            throw new RuntimeException( "can only apply JSObject" );
        
        JSObject jo = (JSObject)o;
        jo.set( "_save" , _save );
        
        return doapply( jo );
    }

    public void setConstructor( JSFunction cons ){
        _constructor = cons;
    }

    // ------

    protected DBCollection( DBBase base , String name ){
        _base = base;
        _name = name;

        _entries.put( "base" , _base.getName() );
        _entries.put( "name" , _name );

        _save = new JSFunctionCalls1() {
                public Object call( Scope s , Object o , Object fooasd[] ){
                    if ( o == null && s.getThis() != null )
                        o = s.getThis();
                    
                    if ( ! ( o instanceof JSObject ) )
                        throw new RuntimeException( "can only save JSObject" );
                    
                    JSObject jo = (JSObject)o;
                    
                    Object presaveObject = (JSFunction)jo.get( "presave" );
                    if ( presaveObject != null && presaveObject instanceof JSFunction ){
                        s.setThis( jo );
                        ((JSFunction)presaveObject).call( s );
                        s.clearThisNormal( null );
                    }

                    LinkedList<JSObject> toSearch = new LinkedList();
                    toSearch.add( jo );
                    while ( toSearch.size() > 0 ){
                        JSObject n = toSearch.remove(0);
                        for ( String name : n.keySet() ){
                            Object foo = n.get( name );
                            if ( foo == null )
                                continue;

                            if ( ! ( foo instanceof JSObject ) )
                                continue;
                            
                            JSObject e = (JSObject)foo;
                            if ( e instanceof JSFileChunk ){
                                _base.getCollection( "_chunks" ).apply( e );
                            }
                            
                            if ( e.get( "_save" ) == null ){
                                toSearch.add( e );
                                continue;
                            }
                            
                            JSFunction otherSave = (JSFunction)e.get( "_save" );
                            otherSave.call( s , e , null );
                            
                        }
                    }

                    ObjectId id = (ObjectId)jo.get( "_id" );

                    if ( id == null || id._new ){
                        if ( id != null )
                            id._new = false;
                        save( jo );
                        return jo;
                    }
                    
                    System.out.println( jo.get( "_id" ) );
                    JSObject q = new JSObjectBase();
                    q.set( "_id" , jo.get( "_id" ) );
                    return update( q , jo , true , true );
                }
            };
        _entries.put( "save" , _save );

        _update = new JSFunctionCalls2() {
                public Object call( Scope s , Object q , Object o , Object foo[] ){
                    
                    if ( o == null )
                        throw new RuntimeException( "object can't be null" );

                    if ( ! ( o instanceof JSObject ) )
                        throw new RuntimeException( "can only save JSObject not : " + o.getClass() );
                    
                    if ( ! ( q instanceof JSObject ) )
                        throw new RuntimeException( "can only save JSObject" );
                    
                    boolean upsert = false;
                    boolean apply = true;
                    
                    if ( foo != null && foo.length > 0 && foo[0] instanceof JSObject ){
                        JSObject params = (JSObject)foo[0];
                        
                        upsert = JSInternalFunctions.JS_evalToBool( params.get( "upsert" ) );
                        if ( params.get( "ids" ) != null )
                            apply = JSInternalFunctions.JS_evalToBool( params.get( "ids" ) );
                    }

                    return update( (JSObject)q , (JSObject)o , upsert , apply );
                }
            };
        _entries.put( "update" , _update );

        _entries.put( "remove" , 
                      new JSFunctionCalls1(){
                          public Object call( Scope s , Object o , Object foo[] ){
                              
                              if ( o == null && s.getThis() != null )
                                  o = s.getThis();
                              
                              if ( ! ( o instanceof JSObject ) )
                                  throw new RuntimeException( "can't only save JSObject" );
                              
                              return remove( (JSObject)o );
                              
                          }
                      } );

                          
        
        
        _apply = new JSFunctionCalls1() {
                public Object call( Scope s , Object o , Object foo[] ){
                    return apply( o );
                }
            };
        _entries.put( "apply" , _apply );

        _find = new JSFunctionCalls2() {
                public Object call( Scope s , Object o , Object fieldsWantedO , Object foo[] ){
                    
                    if ( o == null )
                        o = new JSObjectBase();
                    
                    if ( o instanceof ObjectId )
                        return find( (ObjectId)o );

                    if ( o instanceof JSObject ){
                        return new DBCursor( DBCollection.this , (JSObject)o , (JSObject)fieldsWantedO , _constructor );
                    }
                    
                    throw new RuntimeException( "wtf : " + o.getClass() );
                }
            };
        _entries.put( "find" , _find );

        _entries.put( "findOne" , 
                      new JSFunctionCalls1() {
                          public Object call( Scope s , Object o , Object foo[] ){
                              Object res = _find.call( s , o , foo );
                              if ( res == null )
                                  return null;
                              
                              if ( res instanceof JSArray ){
                                  JSArray a = (JSArray)res;
                                  if ( a.size() == 0 )
                                      return null;
                                  return a.getInt( 0 );
                              }
                              
                              if ( res instanceof Iterator ){
                                  Iterator<JSObject> it = (Iterator<JSObject>)res;
                                  if ( ! it.hasNext() )
                                      return null;
                                  return it.next();
                              }

                              if ( res instanceof JSObject )
                                  return res;
                              
                              throw new RuntimeException( "wtf : " + res.getClass() );
                          }
                      } );

        _entries.put( "tojson" , 
                      new JSFunctionCalls0() {
                          public Object call( Scope s , Object foo[] ){
                              return "{DBCollection:" + _name + "}";
                          }
                      } );
        
    }

    public Object get( Object n ){
        if ( n == null )
            return null;
        Object foo = _entries.get( n.toString() );
        if ( foo != null )
            return foo;
        
        if ( _methods.size() == 0 )
            for ( Method m : this.getClass().getMethods() )
                _methods.add( m.getName() );

        String s = n.toString();

        if ( _methods.contains( s ) )
            return null;

        return getCollection( s );
    }

    public DBCollection getCollection( String n ){
        return _base.getCollection( _name + "." + n );
    }

    public String getName(){
        return _name;
    }
    
    final DBBase _base;
    
    final JSFunction _save;
    final JSFunction _update;
    final JSFunction _apply;
    final JSFunction _find;

    final Set<String> _methods = new HashSet<String>();

    protected Map _entries = new TreeMap();
    final protected String _name;

    protected JSFunction _constructor;
}
