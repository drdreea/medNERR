package corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import findstruct.StructFinder;

import utils.UtilMethods;

public class Sectionizer {
//	public static final String SECTION_FILENAME_EXTENSION = "xml";
//	public static final String TEMPLATE_FILENAME = "template.xml";
	StructFinder findStruct;
	String templatePath;
	
    public static final String fileEnding = ".xml";

    public Sectionizer(String givenTemplate) {
		templatePath = givenTemplate ;
		findStruct = new StructFinder();

    }
    
	public void sectionize(String inputPath, String outputPath){
		try{
			String outputFileName = outputPath ;

			// check if the directory exists, otherwise create it
			new File(new File(outputFileName).getParent()).mkdir();
			outputFileName = outputPath;

			if (!(new File(outputFileName).exists())){

				BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
				ArrayList<String> fileContent = UtilMethods.readFileLines(inputPath);
				String wholeFile = "";

				for(String line : fileContent)
					wholeFile = UtilMethods.mergeStrings(wholeFile, 
							StringEscapeUtils.escapeXml(line.toLowerCase()),
							"\n");
				
				ArrayList<findstruct.Section> result = findStruct.execute(
						wholeFile, new FileInputStream(templatePath));

				String output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<report>\n";

				for(findstruct.Section section : result){
					output =output +  "<" + section.getHeader() + ">";
					output =output +  section.getContent();
					output = output +  "</" + section.getHeader() + ">\n";
				}

				output += "</report>";
				
				out.write(output);
				out.close();
			}

		}catch(Exception e){
			e.printStackTrace();
		}
	}


    public ArrayList<Section> importSections(String rawFile, String filePath) {
    	
    	// now import the sections
        ArrayList<Section> sections = new ArrayList<Section>();

        try {
            File file = new File(filePath);
            
        	// first check whether the file was sectionized already
            if(!file.exists())
            	this.sectionize(rawFile, filePath);
            	
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            NodeList children = doc.getChildNodes();
            String prevHeader = null;

            for (int s = 0; s < children.getLength(); s++) {
                Node fstNode = children.item(s);

                NodeList subChildren = fstNode.getChildNodes();

                for (int s1 = 0; s1 < subChildren.getLength(); s1++) {
                    Node sndNode = subChildren.item(s1);

                    if (sndNode.hasChildNodes()) {
                        NodeList listChild = sndNode.getChildNodes();

                        for (int s2 = 0; s2 < listChild.getLength(); s2++) {
                            Node list = listChild.item(s2);
                            String text = list.getTextContent();

                            Section newSection = new Section(sndNode
                                    .getNodeName(), text);
                            if(newSection.getHeader().equals("list")
                            		&& prevHeader != null)
                            	newSection.setHeader(prevHeader);
                            else
                            	prevHeader = newSection.getHeader();
                            
                            sections.add(newSection);

                        }
                    }
                }

            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return sections;
    }
    
    /**
     * find the section of the given annotation
     * @param recordSections
     * @param currentLine
     * @param futureLine
     * @param med
     * 
     * @return the section name
     */
    public static String findSection(ArrayList<Section> recordSections,
            String currentLine, String futureLine, AnnotationDetail med) {
        String sectionName = "";

        futureLine = futureLine.toLowerCase();
        currentLine = currentLine.toLowerCase();

        String initialCurrentLine = currentLine.replace(":", "");

        if (currentLine.contains(":")) {
            currentLine = removeSubsection(currentLine, med);
        }

        for (Section sct : recordSections) {
            String text = sct.getContent();
            String header = sct.getHeader().replace("_", " ");

            if (text == null || text.isEmpty())
                continue;
            String[] splitContent = text.toLowerCase().split("\n");
            int count = 0;
            boolean found = true;

            if (UtilMethods.mergeStrings(header, splitContent[0]).toLowerCase()
                    .equals(initialCurrentLine))
                return sct.getHeader();

            for (int index = 0; index < splitContent.length; index++) {
                String currentContent = splitContent[index].trim();

                if (currentContent.contains(":")) {
                    currentContent = removeSubsection(currentContent, med);
                }
                if (currentContent.trim().length() <= 2)
                    continue;
                if (currentContent.trim().isEmpty())
                    continue;

                if (currentLine.equals(currentContent))
                    count++;

                if (currentLine.startsWith(currentContent)) {
                    String overlap = currentLine.substring(
                            currentLine.indexOf(currentContent)
                            + currentContent.length()).trim();
                    if (overlap.split(" ").length == 1)
                        count++;
                }

                if (currentLine.endsWith(currentContent)) {
                    String overlap = currentLine.substring(0,
                            currentLine.indexOf(currentContent)).trim();
                    if (overlap.split(" ").length == 1)
                        count++;
                }
                // if(futureLine.contains(splitContent[index])) found = true;
            }

            if (count > 0 && found){
                return sct.getHeader();
            }
        }
        
        if(sectionName.isEmpty())
            return "other";
            
        return sectionName.toLowerCase();
    }

    /**
     * @return
     */
    private static String removeSubsection(String currentLine, AnnotationDetail med) {
        String[] splitLine = currentLine.split("\\:");
        int wordCount = 0;
        for (int index = 0; index < splitLine.length; index++) {
            String subSection = splitLine[index];
            if (subSection.contains(med.content.toLowerCase())
                    && wordCount <= med.startOffset)
                if (med.startLine == med.endLine) {
                    if (med.endOffset <= wordCount
                            + subSection.split(" ").length)
                        currentLine = subSection;
                }
                else {
                    currentLine = subSection;
                }
            wordCount += subSection.split(" ").length;
        }

        return currentLine.trim();
    }

    /**
     * @param sections
     * @param annt1
     * @param annt2
     * @param emr
     * @return whether the two annotations are in the same section
     */
    public static boolean sameSection( AnnotationDetail annt1,
    		AnnotationDetail annt2, MedicalRecord emr) {

        String sect1 = findSection(emr.sections, 
                emr.rawFileLines.get(annt1.startLine-1),
                emr.rawFileLines.get(annt1.startLine), 
                annt1);

        String sect2 = findSection(emr.sections, 
                emr.rawFileLines.get(annt2.startLine-1),
                emr.rawFileLines.get(annt2.startLine),
                annt2);

        if(sect1 == null || sect2 == null)
            return false;
        if(sect1.equals(sect2))
            return true;


        return false;
    }

}
