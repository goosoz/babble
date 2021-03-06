'''
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
'''

import datetime
import _10gen

assert isinstance(_10gen.jsDate, datetime.datetime)
_10gen.pyDate = datetime.datetime.now()
_10gen.pyDateFormatted = _10gen.pyDate.strftime("%Y%m%d %H%M%S")
_10gen.pyMicros = _10gen.pyDate.microsecond
