// GitDirTest.java

package ed.git;

import java.io.*;

import ed.*;
import ed.js.*;
import ed.io.*;
import org.testng.annotations.Test;

public class GitDirTest extends TestCase {

    @Test(groups = {"basic"})
    public void testBasic()
        throws IOException {
        GitDir gd = _create();
        try {
            assertTrue( gd.isValid() );
            assertEquals( "master" , gd.getBranchOrTagName() );
            assertClose( "master" , gd.getAllBranchAndTagNames() );
            assertTrue( gd.onLocalBranch() );
            gd.getCurrentHash();

            gd._exec( "git branch blah" );
            assertEquals( "master" , gd.getBranchOrTagName() );
            
            gd._exec( "git checkout blah" );
            assertTrue( gd.onLocalBranch() );
            assertEquals( "blah" , gd.getBranchOrTagName() );
            gd.getCurrentHash();

            assertTrue( gd.checkout( "master" ) );
            assertEquals( "master" , gd.getBranchOrTagName() );
            assertTrue( gd.checkout( "blah" ) );
            assertEquals( "blah" , gd.getBranchOrTagName() );

            assertFalse( gd.checkout( "blah2" ) );
            assertEquals( "blah" , gd.getBranchOrTagName() );

            assertClose( "blah,master" , gd.getAllBranchAndTagNames() );
            
        }
        finally {
            _destroy( gd );
        }
    }

    @Test(groups = {"basic"})
    public void testTagFinding()
        throws IOException {
        GitDir gd = _create();
        try {

            gd._exec( "git tag abc" );
            assertEquals( "master" , gd.getBranchOrTagName() );
            assertTrue( gd.onLocalBranch() );            

            gd._exec( "git checkout abc" );
            assertEquals( "abc" , gd.getBranchOrTagName() );
            assertFalse( gd.onLocalBranch() );            
            gd.getCurrentHash();
            
            assertTrue( gd.checkout( "master" ) );
            assertEquals( "master" , gd.getBranchOrTagName() );
            assertTrue( gd.onLocalBranch() );

            assertClose( "abc, master" , gd.getAllBranchAndTagNames() );
        }
        finally {
            _destroy( gd );
        }
    }

    @Test(groups = {"basic"})
    public void testReadConfig()
        throws IOException {
        GitDir gd = _create();
        try {
            JSObject config = gd.readConfig();
            assertClose( "[core]" , config.keySet() );
            assertEquals( "true" , ((JSObject)config.get( "core" )).get( "filemode" ) );
        }
        finally {
            _destroy( gd );
        }
        
    }

    GitDir _create()
        throws IOException {
        File root = new File( "/tmp/gittest/" + (int)(Math.random() * 1000000) + "/" );
        root.mkdirs();
        
        File readme = new File( root , "README" );
        FileUtil.touch( readme );

        GitDir g = new GitDir( root );
        g._exec( "git init" );

        g._exec( "git add README" );
        g._exec( "git commit -m foo -a" );
        return g;
    }

    void _destroy( GitDir gd ){
        FileUtil.deleteDirectory( gd._root );
    }

    public static void main( String args[] ){
        (new GitDirTest()).runConsole();
    }
}
