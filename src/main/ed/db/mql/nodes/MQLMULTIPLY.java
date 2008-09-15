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

/* Generated By:JJTree: Do not edit this line. MQLMULTIPLY.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=false,TRACK_TOKENS=false,NODE_PREFIX=MQL,NODE_EXTENDS=,NODE_FACTORY= */
package ed.db.mql.nodes;

import ed.db.mql.SimpleNode;
import ed.db.mql.MQL;

public class MQLMULTIPLY extends SimpleNode {
    public MQLMULTIPLY(int id) {
        super(id);
    }

    public MQLMULTIPLY(MQL p, int id) {
        super(p, id);
    }

    public String stringJSForm() {
        return " * ";
    }

}
/* JavaCC - OriginalChecksum=df3478187eaaa14c31ec0470630c2610 (do not edit this line) */
