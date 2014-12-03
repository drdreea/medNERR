/**
 * 
 */
package postprocess.dailymed;

import io.importer.DailyMedConfigHandler;
import io.importer.DataImport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;

/**
 * @author ab
 *
 */
public abstract class DailyMed {
    final static int requiredArgs = 1;
    DailyMedConfigHandler configs ;
    DataImport importer;

    public DailyMed(String configsPath) {
	importer = new DataImport(null);
	configs = new DailyMedConfigHandler(configsPath);

    }
    
    /**
     * Check that the user gave the right number of params
     * @param params
     */
    public static void checkParams(String[] params) {
	if(params.length!= requiredArgs) {
	    System.out.println("Incorrect arguments");
	    System.exit(-1);
	}
    }
    
    protected void storeMeds(String path, HashSet<String> content) {
  	File out = new File(path);

  	try {
  	    BufferedWriter bf = new BufferedWriter(new FileWriter(out));

  	    for(String med : content) {
  		bf.write(med + "\n");
  		bf.flush();
  	    }

  	    bf.close();
  	}catch(Exception e) {
  	    e.printStackTrace();
  	}

      }

}
