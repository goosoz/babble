

s = new Cloud.Site( "laksjdalsdj" );

s.environments.add( new Cloud.Environment( "dev" ) );
assert.eq( 1 , s.environments.length );
assert.eq( "dev" , s.environments[0].name );
assert.eq( s.environments[0] , s.findEnvironment( "dev" ) );
assert.eq( s.environments[0] , s.findEnvironment( 123 , "dev" ) );
assert.eq( s.environments[0] , s.findEnvironment( s.environments[0].id ) );
assert.eq( s.environments[0] , s.findEnvironmentById( s.environments[0].id ) );
assert.eq( s.environments[0].id.toString() , s.findEnvironmentById( s.environments[0].id.toString() ).id.toString() );
assert.eq( s.environments[0].id , s.findEnvironmentById( s.environments[0].id.toString() ).id );
assert.eq( s.environments[0].id.toString() , s.findEnvironmentById( s.environments[0].id.toString() ).id );
assert.eq( s.environments[0].id , s.findEnvironmentById( s.environments[0].id.toString() ).id.toString() );
assert.eq( s.environments[0] , s.findEnvironment( s.environments[0].name ) );
assert.eq( s.environments[0].id , s.environments[0].iid );

assert.eq( 1 , s.environments.length );
assert.eq( "dev" , s.environments[0].name );
assert( ! s.removeEnvironment( "asd" ) );
assert( s.removeEnvironment( "dev" ) );
assert.eq( 0 , s.environments.length );


s.environments.add( new Cloud.Environment( "dev" ) );
assert.eq( 1 , s.environments.length );
assert.eq( "dev" , s.environments[0].name );
assert( s.removeEnvironment( s.environments[0] ) );
assert.eq( 0 , s.environments.length );


s.environments.add( new Cloud.Environment( "dev" ) );
assert.eq( 1 , s.environments.length );
assert.eq( "dev" , s.environments[0].name );
assert( s.removeEnvironment( s.environments[0].id ) );
assert.eq( 0 , s.environments.length );

