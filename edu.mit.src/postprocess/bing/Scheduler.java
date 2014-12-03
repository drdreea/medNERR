/**
 * 
 */
package postprocess.bing;

import java.util.HashMap;

/**
 * @author ab
 *
 */
public class Scheduler {
    HashMap<String, Boolean> idInUse;

    public Scheduler() {
	idInUse = new HashMap<String, Boolean>();
	idInUse.put("99B37C8EE16FB8CDFA5FFC7923A4A193347487BB", false);
	idInUse.put("99B37C8EE16FB8CDFA5FFC7923A4A1935B682C20", false);
    }
    
    public String getId() {
	boolean foundId = false;
	String appId = null;
	
	while(! foundId) {
	    for(String key : this.idInUse.keySet()) {
		if(this.idInUse.get(key)) continue;
		else {
		    foundId = true;
		    appId = key;
		    this.idInUse.put(key, true);
		}
	    }
	}
	
	return appId;
    }

}
