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

package ed.appserver.adapter.cgi;

import java.util.HashMap;

/**
 *  Simple extension of HashMap to prevent null values
 */
public class EnvMap extends HashMap<String, Object> {

    public EnvMap() {
    }

    public void set(String name, String value)
    {
        put(name, nonNull(value));
    }

    /**
     * Mainly for WSGI, which can pass pythong thingies (e.g. tupeles) for values
     * 
     * @param name name of var
     * @param value value of var
     */
    public void set(String name, Object value)
    {
        put(name, value);
    }

    public String nonNull(String s)
    {
        return s == null ? "" : s;
    }
}
