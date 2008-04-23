// JxpConvertTest.java

package ed.appserver.templates;

import java.io.*;

import org.testng.annotations.Test;

import ed.*;
import ed.js.*;
import ed.js.func.*;
import ed.js.engine.*;
import ed.io.*;

public class JxpConvertTest extends ConvertTestBase {

    public JxpConvertTest(){
        super( ".jxp" );
    }

    TemplateConverter getConverter(){
        return new JxpConverter();
    }

    public static void main( String args[] ){
        JxpConvertTest t = new JxpConvertTest();
        t.runConsole();
    }
    
}
