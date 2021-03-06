

var cleanProto = Object.extend( {} , Date.prototype );
for(var key in (new Date())){ print( "key: "+key); assert(0); }

Date.prototype.foo = 7;
found = 0;
for(var key in (new Date())){ found++; }
assert( found == 1 );

Date.prototype.dontEnum( "foo" );
for(var key in (new Date())){ assert(0); }


Date.prototype.bar = 7;
found = 0;
for(var key in (new Date())){ found++; }
assert( found == 1 );

Date.prototype._dontEnum = true;
for(var key in (new Date())){ assert(0); }

Date.prototype._dontEnum = false;
found = 0;
for(var key in (new Date())){ found++; }
assert( found == 1 );

Date.prototype = cleanProto;


