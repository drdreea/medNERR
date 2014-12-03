/**
 * 
 */
package postprocess.dailymed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author ab
 *
 */
public class ExtractMedIndications extends DailyMed{
    HashSet<String> indications ;

    /**
     * @param configsPath
     */
    public ExtractMedIndications(String configsPath) {
	super(configsPath);

	indications = new HashSet<String>();
    }

    public void execute() {
	String[] files = new File(configs.medsPath).list();

	for(int index = 0; index<files.length; index++) {
	    process(files[index]);
	}

	String outPath = this.configs.outputPath + "/indicationsDailyMed.txt";
	storeMeds(outPath, indications);

    }

    /**
     * @param string
     */
    private void process(String fileName) {
	HashMap<String, String> meds = 
		importer.importDailyMedIndicationsPerMed(
			this.configs.medsPath + "/" + fileName);
	if(meds!=null) {
	    for(String med : meds.keySet()) {
		String content = meds.get(med).trim();
		content = content.replaceAll("\\s+", " ");
		content = content.toLowerCase();
		indications.add(content);
	    }
	}

	if(meds == null)
	    System.out.println(fileName);

    }
    
    protected void storeMeds(String path, HashSet<String> content) {
  	File out = new File(path);

  	try {
  	    BufferedWriter bf = new BufferedWriter(new FileWriter(out));

  	    for(String med : content) {
  		bf.write(med + "\n");
  		bf.write("<->\n");
  		bf.flush();
  	    }

  	    bf.close();
  	}catch(Exception e) {
  	    e.printStackTrace();
  	}

      }

    /**
     * @param args
     */
    public static void main(String[] args) {
	checkParams(args);

	ExtractMedIndications med = new ExtractMedIndications(args[0]);

	med.execute();

    }

}
