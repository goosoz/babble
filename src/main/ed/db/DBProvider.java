// DBProvider.java

package ed.db;

import java.util.*;

public class DBProvider {

    public static DBApiLayer get( String root ){
        return get( root , null );
    }
    
    public static DBApiLayer get( String root , String ip ){

        if ( ip == null || ip.trim().length() == 0 ){
            ip = System.getenv( "db_ip" );
            if ( ip == null || ip.trim().length() == 0 ) {
                ip = "127.0.0.1";
            }
        }
        
        int port = DBPort.PORT;
        
        String s = System.getenv("db_port");
        
        if (s != null && ip.trim().length() > 0) { 
            // deliberately don't check this - the NumFormatExpn should drop the server
            port = Integer.valueOf(s);
        }
        
        return get(root, ip, port);
    }
    
    public static DBApiLayer get(String root , String ip, int port){

        final String key = root + ":" + ip + ":" + port;
        
        DBApiLayer db = _roots.get( key );
        
        if ( db != null ) {
            return db;
        }
        
        synchronized ( _roots ){
            db = _roots.get( key );
            if ( db != null ) {
                return db;       
            }
            
            db = create(root , ip, port);
            _roots.put( key , db );
        }

        return db;

    }

    private static DBApiLayer create(String root , String ip, int port){
        return new DBTCP( root , ip, port);
    }
    
    static final Map<String,DBApiLayer> _roots = new HashMap<String,DBApiLayer>();
}
