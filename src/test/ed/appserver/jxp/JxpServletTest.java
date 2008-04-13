// JxpServletTest.java

package ed.appserver.jxp;

import java.io.*;

import org.testng.annotations.Test;

import ed.appserver.*;
import ed.net.httpserver.*;

public class JxpServletTest extends ed.TestCase {

    String STATIC = "SSSS";
    AppContext CONTEXT = new AppContext( "src/test/samplewww" );
    File one = new File( "src/test/samplewww/1.jpg" );
    
    @Test(groups = {"basic"})
    public void test0(){
        JxpWriter w = new JxpWriter.Basic();
        JxpServlet.MyWriter p = new JxpServlet.MyWriter( w , STATIC , CONTEXT , null  );
        p.print( "abc <img >zz"  );
        assertClose( "WOOGIE abc <img >zz" , w.getContent() );
    }

    @Test(groups = {"basic"})
    public void test1(){
        JxpWriter w = new JxpWriter.Basic();
        JxpServlet.MyWriter p = new JxpServlet.MyWriter( w , STATIC , CONTEXT , null  );
        p.print( "abc"  );
        assertClose( "abc" , w.getContent() );
    }

    @Test(groups = {"basic"})
    public void test2(){
        JxpWriter w = new JxpWriter.Basic();
        JxpServlet.MyWriter p = new JxpServlet.MyWriter( w , STATIC , CONTEXT , null  );
        p.print( "abc <img src='/1.jpg' > 123"  );
        assertClose( "abc <img src='" + STATIC + "/1.jpg?lm=" + one.lastModified() + "' > 123" , w.getContent() );
    }

    @Test(groups = {"basic"})
    public void test3(){
        JxpWriter w = new JxpWriter.Basic();
        JxpServlet.MyWriter p = new JxpServlet.MyWriter( w , STATIC , CONTEXT , null  );
        p.print( "abc <img " );
        p.print( " src='/1.jpg?a=b' >"  );
        assertClose( "abc <img src='" + STATIC + "/1.jpg?a=b&lm=" + one.lastModified() + "' > " , w.getContent() );
    }

    @Test(groups = {"basic"})
    public void test4(){
        JxpWriter w = new JxpWriter.Basic();
        JxpServlet.MyWriter p = new JxpServlet.MyWriter( w , STATIC , CONTEXT , null  );
        p.print( "abc <img "  );
        p.print( " src='"  );
        p.print( "/1.jpg"  );
        p.print( "' >"  );
        assertClose( "abc <img src='" + STATIC + "/1.jpg?lm=" + one.lastModified() + "' > " , w.getContent() );
    }

    @Test(groups = {"basic"})
    public void test5(){
        JxpWriter w = new JxpWriter.Basic();
        JxpServlet.MyWriter p = new JxpServlet.MyWriter( w , STATIC , CONTEXT , null  );
        p.print( "abc <img "  );
        p.print( " src='"  );
        p.print( "1.jpg"  );
        p.print( "' >"  );
        assertClose( "abc <img src='1.jpg' > " , w.getContent() );
    }

    public static void main( String args[] ){
        (new JxpServletTest()).runConsole();
    }
}
