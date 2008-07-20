
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

function A(){
    this.z = 5;
}

A.foo = function(){
    print( "A.foo" );
}

A.prototype.foo = function(){
    print( "A.prototype.foo" );
}

A.foo();
a = new A();
a.foo();
print( a.z );

B = function(){
    this.constructor.__proto__.call( this );
};

B.__proto__ = A;
B.prototype.__proto__ = A.prototype;

B.foo();
b = new B();
b.foo();
print( b.z );
if ( 5 != b.z )
    throw "problem";
