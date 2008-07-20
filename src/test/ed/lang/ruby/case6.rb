
=begin
    Copyright (C) 2008 10gen Inc.
  
    This program is free software: you can redistribute it and/or  modify
    it under the terms of the GNU Affero General Public License, version 3,
    as published by the Free Software Foundation.
  
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
  
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
=end

def foo(a,b)
  c = 
    case a
      when 5
      puts "Y"
      a = a + 1
      puts "Y#{a}"
      b = b + 2
      else
      puts "Z#{a}"
      a = a - 1
      puts "Z#{a}"
      b = b + 1
    end
  puts a
  puts b
  puts c 
  [ a , b , c ]
end

a = foo(2 , 3 )
puts( a[0] )
puts( a[1] )
puts( a[2] )
