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

package ed.lang.ruby;

import org.jruby.*;
import org.jruby.runtime.builtin.IRubyObject;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import ed.db.DBCollection;
import ed.js.JSObject;
import ed.js.Shell;
import static ed.lang.ruby.RubyObjectWrapper.toJS;

@Test(groups = {"ruby.db"})
public class RubyDBTest extends SourceRunner {
    
    static final String DB_NAME = "test";

    @BeforeMethod(groups={"ruby.db", "ruby.db.findone", "ruby.db.find"})
    public void setUp() {
	super.setUp();
	s.put("connect", new Shell.ConnectDB(), true);
	runJS("db = connect('" + DB_NAME + "');");
    }

    public void testCollectionWrapper() {
	Object jsobj = runRuby("$db.foobar");
	assertNotNull(jsobj);
	assertTrue(jsobj instanceof DBCollection, "jsobj is not a DBCollection; it's " + jsobj.getClass().getName());
    }
}
