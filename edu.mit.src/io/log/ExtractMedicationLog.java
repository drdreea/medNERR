/**
 * Created on Jan 10, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package io.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * @author ab
 *
 */
public class ExtractMedicationLog extends ExperimentLog{

	public ExtractMedicationLog(String outputPath) {
		FileWriter fstream = null;
		
		try {
			new File(outputPath + "/out.txt");
			fstream = new FileWriter(outputPath + "/out.txt");
			this.out = new BufferedWriter(fstream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
