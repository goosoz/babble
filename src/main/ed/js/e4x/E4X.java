// E4X.java

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

package ed.js.e4x;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import ed.js.*;
import ed.js.func.*;
import ed.js.engine.*;
import ed.util.*;

public class E4X {

    static Object _nodeGet( ENode start , String s ){
        if( start instanceof XMLList )
            return _nodeGet( (XMLList)start, s );
        return _nodeGet( new XMLList( start ), s );
    }

    static Object _nodeGet( XMLList start , String s ){
        final boolean search = s.startsWith( ".." );
        if ( search )
            s = s.substring(2);

        final boolean attr = s.startsWith( "@" );
        if ( attr )
            s = s.substring(1);

        final boolean qualified = s.contains( "::" );
        String uri = "";
        if( qualified ) {
            uri = s.substring( 0, s.indexOf("::") );
            s = s.substring( s.indexOf( "::" ) + 2 );
        }

        final boolean all = s.endsWith("*");
        if( all ) {
            if( s.length() > 1) return null;
            s = "";
        }

        List<ENode> traverse = new LinkedList<ENode>();
        List<ENode> res = new ArrayList<ENode>();
    
        for(int k=0; k< start.size(); k++) {
            traverse.add( start.get(k) );

            while ( ! traverse.isEmpty() ){
                ENode n = traverse.remove(0);

                if ( attr ){
                    ArrayList<ENode> nnm = n.getAttributes();
                    for( ENode enode : nnm ) {
                        if( all || ( ( ( qualified && enode.name().uri.equals( uri ) ) || !qualified ) && enode.localName().equals( s ) ) ) {
                            res.add( enode );
                        }
                    }
                }
            
                XMLList kids = n.children();
                if ( kids == null || kids.size() == 0 )
                    continue;

                for ( int i=0; i<kids.size(); i++ ){
                    ENode c = kids.get(i);
                    if ( !attr && c.node.getNodeType() != Node.ATTRIBUTE_NODE && 
                         ( all || 
                           ( ( c.node.getNodeType() == Node.TEXT_NODE && c.text().equals( s ) ) || c.node.getNodeType() != Node.TEXT_NODE ) &&
                           ( ( ( qualified && c.name().uri.equals( uri ) ) || !qualified ) && 
                             ( c.localName() != null && c.localName().equals( s ) ) ) ) ) {
                        res.add( c );
                    }

                    if ( search )
                        traverse.add( c );
                }
            }
        }
        return _handleListReturn( res );
    }

    static Object _handleListReturn( List<ENode> lst ){
        if ( lst.size() == 0 )
            return null;

        if ( lst.size() == 1 ){
            return lst.get(0);
        }
        return new XMLList(lst);
    }

    public static boolean isXMLName( String name ) {
        Pattern invalidChars = Pattern.compile("[@\\s\\{\\/\\']|(\\.\\.)|(\\:\\:)");
        Matcher m = invalidChars.matcher( name );
        if( m.find() ) {
            return false;
        }
        return true;
    }

    public static String escapeElementValue( String s ) {
        s = s.replaceAll( "<", "&lt;" );
        s = s.replaceAll( ">", "&gt;" );
        s = s.replaceAll( "&", "&amp;" );
        return s;
    }

    public static String escapeAttributeValue( String s ) {
        s = s.replaceAll( "\"", "&quot;" );
        s = s.replaceAll( ">", "&gt;" );
        s = s.replaceAll( "&", "&amp;" );

        s = s.replaceAll( "\\u000A", "&#xA;" );
        s = s.replaceAll( "\\u000D", "&#xD;" );
        s = s.replaceAll( "\u0009", "&#x9;" );
        return s;
    }

    public static XMLList addNodes(ENode a, ENode b) {
        if( a instanceof XMLList && b instanceof XMLList) {
            ((XMLList)a).addAll( (XMLList)b );
            return (XMLList)a;
        }
        else if ( a instanceof XMLList ) {
            ((XMLList)a).add(b);
            return (XMLList)a;
        }
        else if ( b instanceof XMLList ) {
            ((XMLList)b).add(0, a);
            return (XMLList)b;
        }
        else {
            XMLList list = new XMLList();
            list.add( a );
            list.add( b );
            return list;
        }
    }

    static abstract class Query {
        public Query( String what , JSString match ){
            _what = what;
            _match = match;
        }

        abstract boolean match( ENode n );

        final String _what;
        final JSString _match;
    }

    public static class Query_EQ extends Query {

        public Query_EQ( String what , JSString match ){
            super( what , match );
        }

        boolean match( ENode n ){
            ENode result = (ENode)n.get( _what );
            if( result.isDummy() )
                return false;
            if( result.node.getNodeType() == Node.ATTRIBUTE_NODE )
                return result.node.getNodeValue().equals( _match.toString() );
            else
                return JSInternalFunctions.JS_eq( E4X._nodeGet( n , _what ) , _match );
        }

        public String toString(){
            return " [[ " + _what + " == " + _match + " ]] ";
        }

    }
}