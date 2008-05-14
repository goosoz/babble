// Security.java

package ed.security;

import ed.js.engine.*;

public class Security {

    public final static boolean OFF = Boolean.getBoolean( "NO-SECURITY" );

    final static String SECURE[] = new String[]{ 
        Convert.DEFAULT_PACKAGE + "._data_corejs_" , 
        Convert.DEFAULT_PACKAGE + "._data_sites_admin_" , 
        Convert.DEFAULT_PACKAGE + "._data_sites_www_" , 
        Convert.DEFAULT_PACKAGE + ".lastline" ,
        Convert.DEFAULT_PACKAGE + "._home_yellow_code_for_hudson" 
    };
    
    public static boolean isCoreJS(){
        if ( OFF )
            return true;

        String topjs = getTopJS();
        if ( topjs == null ) {
            return false;
        }
        
        for ( int i=0; i<SECURE.length; i++ )
            if ( topjs.startsWith( SECURE[i] ) )
                return true;
        
        return false;
    }

    public static String getTopJS(){

        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        
        for ( int i=0; i<st.length; i++ ){
            StackTraceElement e = st[i];
            if ( e.getClassName().startsWith( Convert.DEFAULT_PACKAGE + "." ) )
                return e.getClassName();
        }

        return null;
    }
}
