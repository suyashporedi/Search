/*
 * Author : Suyash Poredi 03 October 2019
 * Index Generator
 * 
 * References :
 * 1) XML Parser : https://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
 * 2) Remove Escape characters : https://stackoverflow.com/questions/12423071/how-to-remove-escape-characters-from-a-string-in-java
 * 3) XML String to Document : https://www.journaldev.com/1237/java-convert-string-to-xml-document-and-xml-document-to-string
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GenerateIndex {

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {


		String corpusPath="";
		// check for Corpus path in Command Line Argument
		if(args.length < 1){ 
			System.out.println("Invalid Command Line Arguments : Please specify Corpus path !");
			System.exit(1); 
		} 
		corpusPath = args[0]; 


		ArrayList<HashMap<String, String>> documents = new ArrayList<HashMap<String, String>>();
		//Read files one by one
		File f = new File(corpusPath);
		//Reading all files from Corpus
		File [] files = f.listFiles();
		System.out.println("Reading files from Corpus....");
		for (File file : files) {
			if (file.getName().endsWith("trectext")) {
				InputStream is;
				String fileString = "";
				try {
					is = new FileInputStream(file);
					@SuppressWarnings("resource")
					BufferedReader buffRead = new BufferedReader(new InputStreamReader(is)); 
					String line = buffRead.readLine(); 
					StringBuilder stringbuffer = new StringBuilder(); 

					while(line != null){
						stringbuffer.append(line).append("\n"); 
						line = buffRead.readLine(); 
					}
					fileString = stringbuffer.toString();
					fileString = fileString.replaceAll("& ", "&amp;").replaceAll("&$","&amp;").replaceAll("[^\\x00-\\x7F]", "").replaceAll("&\n","&amp;");
				} catch (IOException e1) {
					e1.printStackTrace();
				} 

				String str = "<DOCS>" + fileString +"</DOCS>"; 
				Document doc= DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(str))); 

				doc.getDocumentElement().normalize();
				NodeList children = doc.getElementsByTagName("DOC");

				for(int i=0; i<children.getLength(); i++){
					Node child = children.item(i);
					if(child.getNodeType()== Node.ELEMENT_NODE){
						NodeList list = child.getChildNodes();
						HashMap<String, String> document = new HashMap<String, String>();
						for(int j=0; j<list.getLength(); j++){
							Node childNodes=list.item(j);
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
								String s=childNodes.getTextContent();
								if(document.containsKey("BYLINE")){
									s = document.get("BYLINE") + ' ' + childNodes.getTextContent();
								}
								document.put("BYLINE", s);
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
			}
		}
		System.out.println("Finished reading files from Corpus....");
		String indexPath = "./index";

		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			Directory dir = FSDirectory.open(Paths.get(indexPath));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			iwc.setOpenMode(OpenMode.CREATE);

			IndexWriter writer = new IndexWriter(dir, iwc);

			for (HashMap<String, String> document : documents) {
				indexDoc(writer, document);
			}

			writer.close();
			System.out.println("Done ...");

			statsInfo(indexPath);

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass()
			+ "\n with message: " + e.getMessage());
		}
	}
	//Generate Statistics 
	static void statsInfo(String indexFilePath) throws IOException
	{
		String index = indexFilePath;
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths
				.get(index)));

		System.out.println("Total number of documents in the corpus: "
				+ reader.maxDoc());

		reader.close();
	}

	// Create Index doc using lucene
	static void indexDoc(IndexWriter writer, HashMap<String, String> document) throws IOException {
		org.apache.lucene.document.Document lDoc = new org.apache.lucene.document.Document();

		lDoc.add(new StringField("DOCNO", document.get("DOCNO"),
				Field.Store.YES));

		if(document.get("HEAD")!=null) {
			lDoc.add(new StringField("HEAD", document.get("HEAD"),
					Field.Store.YES));
		}

		if(document.get("BYLINE")!=null) {
			lDoc.add(new StringField("BYLINE", document.get("BYLINE"),
					Field.Store.YES));
		}

		if(document.get("DATELINE")!=null) {
			lDoc.add(new StringField("DATELINE", document.get("DATELINE"),
					Field.Store.YES));
		}
		lDoc.add(new TextField("TEXT", document.get("TEXT"), Field.Store.NO));
		writer.addDocument(lDoc);
	}

}