/**
 * Created on Feb 4, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package conceptExtraction;

import java.util.ArrayList;

import corpus.AnnotationDetail;


/**
 * @author ab
 *
 */
public class NEInformation {

	private ArrayList<AnnotationDetail> medicationInformation;
	private ArrayList<AnnotationDetail> reasonInformation;
	
	NEInformation(){
		
	}
	
	NEInformation(ArrayList<AnnotationDetail> meds, ArrayList<AnnotationDetail> reasons){
		this.medicationInformation = meds;
		this.reasonInformation = reasons;
	}
	
	/**
	 * @return the medicationInformation
	 */
	public ArrayList<AnnotationDetail> getMedicationInformation() {
		return medicationInformation;
	}
	/**
	 * @param medicationInformation the medicationInformation to set
	 */
	public void setMedicationInformation(ArrayList<AnnotationDetail> medicationInformation) {
		this.medicationInformation = medicationInformation;
	}
	/**
	 * @return the reasonInformation
	 */
	public ArrayList<AnnotationDetail> getReasonInformation() {
		return reasonInformation;
	}
	/**
	 * @param reasonInformation the reasonInformation to set
	 */
	public void setReasonInformation(ArrayList<AnnotationDetail> reasonInformation) {
		this.reasonInformation = reasonInformation;
	}

}
