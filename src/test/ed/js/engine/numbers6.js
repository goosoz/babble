

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

for ( var i=1; i<50; i++ ){
    var n = 2;
    for ( var j=0; j<i; j++ )
        n *= 2;
    
    print( n );

    for ( var j=0; j<i; j++ )
        n = n / 2;
    
    print( n );
}


var x = 5;
x++;
print( "abc" + x );

print( 5 < 4 );
print( 5 < "4" );
print( 5 < "" );

print( 0 < "" );
print( 0 > "" );

print( 1 < "" );
print( 1 > "" );
