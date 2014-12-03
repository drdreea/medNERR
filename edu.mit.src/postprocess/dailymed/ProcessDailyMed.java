package postprocess.dailymed;

import java.io.File;
import java.util.HashSet;

public class ProcessDailyMed extends DailyMed{
    HashSet<String> medications;

    ProcessDailyMed(String configPath){
	super(configPath);
	medications = new HashSet<String>();
    }

    public void execute() {
	String[] files = new File(configs.medsPath).list();

	for(int index = 0; index<files.length; index++) {
	    process(files[index]);
	}
	
	String outPath = this.configs.outputPath + "/dailyMed.txt";
	storeMeds(outPath, medications);
    }

  

    private void process(String fileName) {
	HashSet<String> meds = importer.importDailyMed(this.configs.medsPath + "/" + fileName);
	if(meds!=null) {
	    for(String med : meds)
		if(med.contains(",")) {
		    String[] splitMed = med.split(",");
		    for(int index = 0; index<splitMed.length; index++) {
			if(!splitMed[index].trim().isEmpty())
			    medications.add(splitMed[index].toLowerCase().trim());
		    }
		}else
		    medications.add(med.toLowerCase().trim());

	}
	if(meds == null)
	    System.out.println(fileName);
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
	checkParams(args);

	ProcessDailyMed processor = new ProcessDailyMed(args[0]);
	processor.execute();

    }

}
