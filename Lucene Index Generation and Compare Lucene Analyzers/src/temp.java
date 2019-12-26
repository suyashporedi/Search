import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class temp {

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {


		File fXmlFile = new File("/Users/suyashporedi/Desktop/SUYASH/Search/Assignment 1 Indexing/corpus/AP890520.trectext");

		if (fXmlFile.getName().endsWith("trectext")) {
			//DOMParser parser = new DOMParser();
			InputStream is;
			String fileAsString = "";
			try {
				is = new FileInputStream(fXmlFile);
				BufferedReader buf = new BufferedReader(new InputStreamReader(is)); 
				String line = buf.readLine(); 
				StringBuilder sb = new StringBuilder(); 

				while(line != null){
					sb.append(line).append("\n"); 
					line = buf.readLine(); 
				}
				fileAsString = sb.toString();
				fileAsString = fileAsString.replaceAll("& ", "&amp;").replaceAll("&$","&amp;").replaceAll("[^\\x00-\\x7F]", "").replaceAll("&\n","&amp;");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
			ArrayList<HashMap<String, String>> documents = new ArrayList<HashMap<String, String>>();
			String str="<DOCS>" + fileAsString +"</DOCS>"; 
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	         
	        //API to obtain DOM Document instance
	        DocumentBuilder builder = null;
	        Document doc=null;
	        try
	        {
	            //Create DocumentBuilder with default configuration
	            builder = factory.newDocumentBuilder();
	             
	            //Parse the content to Document object
	            doc = builder.parse(new InputSource(new StringReader(str)));
	           
	        }
	        catch (Exception e)
	        {
	            e.printStackTrace();
	        }
	      
			
			
			doc.getDocumentElement().normalize();
			NodeList children = doc.getElementsByTagName("DOC");
			for(int i=0; i<children.getLength(); i++){
				Node child = children.item(i);
				//System.out.println(child.toString());
				if(child.getNodeType()== Node.ELEMENT_NODE){
					NodeList list = child.getChildNodes();
					HashMap<String, String> document = new HashMap<String, String>();
					for(int j=0; j<list.getLength(); j++){
						Node childNodes=list.item(j);
						//System.out.println(childNodes.getNodeName() +" :  "+childNodes.getNodeValue());
						if(childNodes.getNodeName().equals("DOCNO"))
						{
							document.put("DOCNO", childNodes.getTextContent());
						}
						else if(childNodes.getNodeName().equals("HEAD"))
						{
							String s = childNodes.getTextContent();
							if(document.containsKey("HEAD")){
								s = document.get("HEAD") + ' ' + childNodes.getTextContent();
							}
							document.put("HEAD", s);
						}
						else if(childNodes.getNodeName().equals("BYLINE"))
						{
							document.put("BYLINE", childNodes.getTextContent());
						}
						else if(childNodes.getNodeName().equals("DATELINE"))
						{
							document.put("DATELINE", childNodes.getTextContent());
						}
						else if(childNodes.getNodeName().equals("TEXT"))
						{
							document.put("TEXT", childNodes.getTextContent());
						}
					}
					documents.add(document);
				}
			}
			System.out.println(documents.get(1));
		}
	}

}
