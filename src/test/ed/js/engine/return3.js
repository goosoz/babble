
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

function foo1(){
    return 5;
}


function foo2(){
    return 
    5;
}

print( ! foo1() );
print( ! foo2() );

x = true;
y = true;
z = 3;

// implicit return
print( eval( 'if(x&&y) "PASS"; else "FAIL";' ) );
print( z == eval( "var a=3; { a }" ) );
