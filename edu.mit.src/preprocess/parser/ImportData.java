package preprocess.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import utils.UtilMethods;

public class ImportData {
    enum Type {PARSE, TAG};

    public ArrayList<SentenceContent> importTaggedFile(byte[] byteArray, 
    		ArrayList<String> rawLines){
        // first check if the file path exists
        ByteArrayInputStream outputStream = 
                new ByteArrayInputStream( byteArray);

        return importFile(outputStream, Type.TAG, rawLines);

    }

    public ArrayList<SentenceContent> importParsedFile(byte[] byteArray, 
    		ArrayList<String> rawLines){
        // first check if the file path exists
        ByteArrayInputStream outputStream = 
                new ByteArrayInputStream( byteArray);

        return importFile(outputStream, Type.PARSE, rawLines);

    }

    public ArrayList<SentenceContent> importTaggedFile(String filePath,
    		ArrayList<String> rawLines){
        // first check if the file path exists
        File outputFileReader = new File(filePath);
        FileInputStream outputStream;
        try {
            outputStream = new FileInputStream(outputFileReader);
            if (!outputFileReader.exists() || 
                    (outputStream.read() == -1)){
                System.out.println("ERROR output stream empty " + 
                        filePath);
            }else{
                return importFile(outputStream, Type.PARSE, rawLines);
            }
        } catch ( Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public ArrayList<SentenceContent> importFile(InputStream outputFileReader, 
            Type importType, ArrayList<String> rawLines){
        ArrayList<SentenceContent> sentences = new ArrayList<SentenceContent>();

        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(outputFileReader);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("sentence");
            int documentOffset = 0;

            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElements = (Element) nNode;
                    NodeList content = eElements.getElementsByTagName("content");
                    NodeList value = eElements.getElementsByTagName("value");

                    SentenceContent newSentence = new SentenceContent(documentOffset);

                    if(content.getLength() == 1){
                        String contentValue = content.item(0).getTextContent();
                        switch (importType){
                        case PARSE :
                            newSentence.parseContent(contentValue);
                            break;
                        case TAG:
                            newSentence.parseTag(contentValue);
                            break;
                        }
                    }
                    if(value.getLength() == 1){
                        String contentValue = value.item(0).getTextContent();
                        newSentence.parseSentence(contentValue);
                    }

                    sentences.add(newSentence);
                    String processedSentence = UtilMethods.sentenceProcessTagger(
                            newSentence.sentence);
                    documentOffset += processedSentence.split(" ").length;
                }

            }
        }catch(Exception e){
            e.printStackTrace();
            System.exit(-1);
        }

        return sentences;

    }


}
