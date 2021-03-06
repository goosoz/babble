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

jsDate = new Date();

local.src.test.ed.lang.python.date1_helper();

assert( pyDate instanceof Date );
assert( Math.abs(jsDate.getTime() - pyDate.getTime()) < 2000 ,
        'time zone changed' );

assert.eq( pyDate.getMilliseconds() , Math.floor ( pyMicros / 1000 ) );
assert.eq( pyDate.format("yyyyMMdd HHmmss") , pyDateFormatted );


