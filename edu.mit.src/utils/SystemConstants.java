package utils;

import java.util.Arrays;
import java.util.List;

public class SystemConstants {

    public static final List<String> medDependencies = Arrays.asList("nn",
	    "dep", "amod");
    // temporarily taking down amod

    public static final List<String> reasonDependencies = Arrays.asList("poss",
	    "amod", "det", "nsubj");

    public static final List<String> preAttributes = Arrays.asList("multiple",
	    "therapeutic", "home", "this", "cream", "powder", "baby",
	    "nebulizer", "nebulizers", "regimen", "troche", "double strength",
	    "inhaler", "lotion", "oral liquid", "nasal spray", "spray",
	    "metered dose inhaler", "ointment", "all", "capsule", "strip",
	    "supplementation", "sliding scale", "single strength", "mdi", "regular");

    public static final List<String> postAttributes = Arrays.asList(
	    "therapeutic", "home", "cream", "topical powder", "powder", "gtt",
	    "baby", "slow release", "extended release", "immediate release",
	    "immediate rel","immed rel", "tablet", "tablets", "nebulizer", "nebulizers",
	    "patch", "regimen", "troche", "double strength", "inhaler",
	    "inhalers", "lotion", "oral liquid", "nasal spray", "spray",
	    "sprays", "metered dose inhaler", "ointment", "capsule", "strip",
	    "supplementation", "single strength", "pca", "suspension",
	    "diskus", "products", "product", "transfusions", "transfusion",
	    "drops", "regular");

    public static final List<String> mergeAttributes = Arrays.asList("plus");

    public static final List<String> excludeHeadings = Arrays.asList("entered",
	    "entered", "dictated_by",
	    "attending",
	    "entered_by",
	    // history
	    "social_history",
	    "family_history",
	    "past_surgical_history",
	    // labs data
	    "laboratory_data", "admission_labs",
	    "laboratory_data_on_admission", "data",
	    "laboratory_data_on_discharge",
// "studies",
	    "studies_at_the_time_of_admission", "labs",
	    // allergies
	    "allergies", "allergy",
	    // diet
	    "diet", "physical_examination", "for_pcp",
// "follow_up", "follow-up",
	    "medical_service", "potentially_serious_interaction",
	    "possible_allergy"
    // diagnosis
// "diagnoses", "admit_diagnosis", "principal_discharge_diagnosis",
// "principal_diagnosis", "principal_diagnoses",
// "secondary_diagnosis", "secondary_diagnoses",
// "discharge_diagnosis", "chief_complaint", "other_diagnosis",
// "other_diagnoses"
	    );

    public static final List<String> medications = Arrays.asList("medications",
	    "rx_on_admit", "home_meds", "meds", "current_medications ",
	    "discharge_medications ", "medications_on_discharge ",
	    "drug_history", "medications_at_rehab");

    public static final List<String> labs = Arrays.asList("laboratory_data",
	    "admission_labs", "laboratory_data_on_admission", "data",
	    "laboratory_data_on_discharge", "studies",
	    "studies_at_the_time_of_admission", "labs");

    public static final List<String> excludeHeadingsPartial = Arrays
	    .asList("diagnosis");

    public static final List<String> excludeWords = Arrays.asList("allerg");

    final static List<String> quantities = Arrays.asList("mg", "mEq", "ml",
	    "drops", "l");

    final static List<String> modal = Arrays.asList(
// "iv", "the", "prn", "oral",
// "b.i.d.", "b.i.d", "drip", "inh", "q.i.d", "q.i.d.",
// "taper", "subcutaneous", "one", "given",
// "sliding","therapy","regimen",
// "same", "clinic", "w/", "mh", "continue", "cp", "continued",
// "continue", "high", "q", "q.", "sats", "these", "with", "units"
	    );

    public final static List<String> conjuctions = Arrays.asList("and", "but",
	    "or", "nor", "yet", "so");

    public final static List<String> separatingConjuctions = Arrays.asList(
	    "but", "or", "nor", "yet", "so", "while");

    public final static List<String> falseReasons = Arrays.asList("bid");
}
