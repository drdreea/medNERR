package experiments;

import io.importer.TopicConfigHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import utils.SystemConstants;

public class ExportDataForTopicModel {
	TopicConfigHandler configs;
	enum Type{MEDICATION, LAB};

	public ExportDataForTopicModel(String configPath) {
		configs = new TopicConfigHandler(configPath);
	}

	private void execute() {
		String[] files = new File(this.configs.medsPath).list();

		for(int index=0; index<files.length; index++) {
			if(files[index].startsWith(".")) continue;
			process(files[index]);
		}

	}

	private void process(String path) {
		String filePath = this.configs.medsPath +"/"+path;

		try {
			File in = new File(filePath);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(in);
			doc.getDocumentElement().normalize();
			NodeList children = doc.getChildNodes();

			for (int s = 0; s < children.getLength(); s++) {
				Node fstNode = children.item(s);

				NodeList subChildren  = fstNode.getChildNodes();

				for (int s1 = 0; s1 < subChildren.getLength(); s1 ++) {
					Node sndNode = subChildren.item(s1);

					if (sndNode.hasChildNodes()) {
						NodeList listChild = sndNode.getChildNodes();

						for (int s2 = 0; s2 < listChild.getLength(); s2 ++) {
							Node list = listChild.item(s2); 
							String text = list.getTextContent();

							if (isLab(sndNode.getNodeName()))
								storeContent(text, path, Type.LAB);
							else if(isMedication(sndNode.getNodeName()))
								storeContent(text, path, Type.MEDICATION);
						}
					}
				}
			}

		}catch(Exception e) {

		}
	}

	public void storeContent(String content, String fileName, Type storeType) {
		String outputPath = "";

		switch(storeType) {
		case MEDICATION:
			outputPath = this.configs.outputPathMed+fileName;
			break;
		case LAB:
			outputPath = this.configs.outputPathLab+fileName;
			break;
		}

		File out = new File(outputPath);

		try {
			BufferedWriter bf = new BufferedWriter(new FileWriter(out, true));
			bf.write(content);
			bf.flush();
			bf.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isLab(String nodeName) {
		if(SystemConstants.labs.contains(nodeName))
			return true;

		return false;
	}

	public boolean isMedication(String nodeName) {
		if(SystemConstants.medications.contains(nodeName))
			return true;

		return false;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length !=1) {
			System.out.println("Incorrect params");
			System.exit(-1);
		}

		ExportDataForTopicModel exporter = new ExportDataForTopicModel(args[0]);
		exporter.execute();
	}

}
