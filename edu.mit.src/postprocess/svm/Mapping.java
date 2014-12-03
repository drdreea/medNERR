package postprocess.svm;

import java.util.ArrayList;

public class Mapping {
    public String map;
    public double score;

    public Mapping(String str, double val) {
	map = str;
	score = val;
    }

    public static boolean listContain(ArrayList<Mapping> listMap, String concept) {
	boolean found = false;

	for(Mapping map : listMap) 
	    if(map.map.equals(concept)) {
		found =true;
		break;
	    }


	return found;
    }

}
