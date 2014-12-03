************************************************
2009 i2b2 NLP Challenge Evaluation Code
README.TXT

Correspondence --
Challenge-related:
	Ozlem Uzuner, OUzuner@uamail.albany.edu
	Imre Solti, solti@u.washington.edu
Code-related:
	Eithon Cadag, ecadag@uw.edu 

v1.05
Document last updated on 10/16/2009
************************************************

I. ARCHIVE CONTENTS
The archive in which this README was found should contain the following files:
	README.TXT -- This file.
	i2b2eval.py -- The primary code for running ground the truth vs. system evaluation
	i2b2obj.py -- A support module for i2b2eval.py
If any of the above files is missing from the archive, please email Eithon for an updated archive.


II. DESCRIPTION
The file i2b2eval.py generates XML-format files consisting of record entries, and conducts evaluations of ground truth and system metrics, per the latest version of the evaluation metrics specification (see Google Groups page for the most recent copy).


III. SYSTEM REQUIREMENTS
i2b2eval.py requires either Python 2.4 or Python 2.5 to run. The software has not been tested with earlier versions of Python, or Python 3.0, and are not guaranteed to run properly on those interpreters.


IV. USAGE
i2b2eval.py has two modes: XML-generating, and evaluation-running (which requires the prerequisite XML). To see the built-in help, type:

	$> python i2b2eval.py -h

and the help will print onto the shell.

To run XML-generating mode, i2b2eval requires three inputs: a directory location containing the target (raw) medical notes, a properly-formatted XML file containing the ground truth, and a properly-formatted XML file containing the system (test) output. To generate either the ground truth or system XML, use the following command:

	$> python i2b2eval.py -x -r <record_directory> -z <entries_directory> -o <output_xml_file>

e.g.:

	$> python i2b2eval.py -x -r records/ -z gold_entries/ -o gold.xml

will create a file `gold.xml' containing the all the i2b2 mentions for all records present in both the `records/' and `gold_entries/' directories. The expected format of all i2b2 entry files containing mentions is `<record_name>.i2b2.entries', where `<record_name>' matches the record name of a medical note present in the record directory.

Once an XML file is available for both ground truth and testing, evaluation can be run using the following command:

	$> python i2b2eval.py -g <ground_truth_xml> -s <test_xml>

where `<ground_truth_xml>' and `<test_xml>' are the the ground truth and test XML files, respectively,  generated using the i2b2eval XML-generating mode, e.g.:

	$> python i2b2eval.py -g gold.xml -s test.xml

The output of i2b2eval for evaluation is written to standard out, and is a tab-delimited output of the following form:

	<inexact_or_exact_eval>	<vertical_or_horizontal> <system_or_patient_level> <tag_or_X_for_all_tags> <fmeasure>

with comments preceded by the `#' symbol.


V. KNOWN ISSUES
8/7/2009 -- Exact Fscores appear to be higher than inexact Fscores, on 
occasion (report by Adam Teichert); currently being investigated (OKAY)

VI. HISTORY
10/16/2009 -- Added Pr, Re flag (-p)
8/26/2009 -- Tabs replaced with spaces for vector information (bug report by Doan Son)
8/24/2009 -- Speed improvements (by Illes Solt)
8/7/2009 -- Structural data for record is now incorporated into the XML file (no longer any need to include the record directory during evaluation)
8/2/2009 -- Tags absent in the ground truth are now not included in the f-score calculations (bug report by Zuofeng Li)
7/27/2009 -- Released to i2b2 participants
