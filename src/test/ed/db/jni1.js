

db = connect( "foo" , "127.0.0.1" );
t = db.t;

t.remove( {} );
t.save( { z : 1 } );
t.save( { z : 2 } );
assert( 2 == t.find( { $where : function(){ return 1; } } ).length() );
assert( 1 == t.find( { $where : function(){ return obj.z == 2; } } ).length() );

