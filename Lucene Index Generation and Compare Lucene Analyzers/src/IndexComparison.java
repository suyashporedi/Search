/*
 * Author : Suyash Poredi  03 October 2019
 * Index Comparison using Lucene
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
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class IndexComparison {
	public static ArrayList<HashMap<String, String>> documents = new ArrayList<HashMap<String, String>>();

	public static void main(String[] args) {

		String corpusPath="";
		// check for Corpus path in Command Line Argument
		if(args.length < 1){ 
			System.out.println("Invalid Command Line Arguments : Please specify Corpus path !");
			System.exit(1); 
		} 
		corpusPath = args[0]; 

		Analyzer analyzer = null;
		int analyzerChoice;
		do 
		{
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println(" Choose Option(1-4) for Index Comparison\n 1)Standard Analyzer\n 2)Keyword Analyzer\n 3)Simple Analyzer\n 4)Stop Analyzer\n 5)Exit");
			analyzerChoice = input.nextInt(); 
			String indexPath="./index";
			switch(analyzerChoice)
			{
			case 1: 
				indexPath=indexPath+"/Standard";
				analyzer = new StandardAnalyzer();
				indexCreater(indexPath,analyzer,corpusPath);
				break;

			case 2: 
				indexPath=indexPath+"/Keyword";
				analyzer = new KeywordAnalyzer();
				indexCreater(indexPath,analyzer,corpusPath);
				break;

			case 3: 
				indexPath=indexPath+"/Simple";
				analyzer = new SimpleAnalyzer();
				indexCreater(indexPath,analyzer,corpusPath);
				break;

			case 4: 
				indexPath=indexPath+"/Stop";
				analyzer = new StopAnalyzer();
				indexCreater(indexPath,analyzer,corpusPath);
				break;
			case 5:
				System.out.println("Exiting ........");
				break;
			}

		}while(analyzerChoice<5);
	}
	// Read one by one file and send documents for Indexing
	static void indexCreater(String indexFilePath, Analyzer analyzer, String corpusPath) {

		File f = new File(corpusPath);

		File [] files = f.listFiles();

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

				String str="<DOCS>" + fileString +"</DOCS>"; 

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

				DocumentBuilder builder = null;
				Document doc=null;
				try
				{
					builder = factory.newDocumentBuilder();
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
					if(child.getNodeType()== Node.ELEMENT_NODE){
						NodeList list = child.getChildNodes();
						HashMap<String, String> document = new HashMap<String, String>();
						for(int j=0; j<list.getLength(); j++){
							Node childNodes=list.item(j);
							if(childNodes.getNodeName().equals("TEXT"))
							{
								document.put("TEXT", childNodes.getTextContent());
							}
						}
						documents.add(document);
					}
				}
			}
		}
		try {
			System.out.println("Indexing to directory '" + indexFilePath + "'...");

			Directory dir = FSDirectory.open(Paths.get(indexFilePath));

			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			iwc.setOpenMode(OpenMode.CREATE);

			IndexWriter writer = new IndexWriter(dir, iwc);

			for (HashMap<String, String> document : documents) {
				indexDoc(writer, document);
			}

			writer.close();
			System.out.println("Done indexing ... Path :" +indexFilePath+" ");

			statsInfo(indexFilePath);

			documents.clear();

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass()
			+ "\n with message: " + e.getMessage());
		}
	}

	// Method to write index to file using lucene 
	static void indexDoc(IndexWriter writer, HashMap<String, String> document) throws IOException {
		org.apache.lucene.document.Document lDoc = new org.apache.lucene.document.Document();

		lDoc.add(new TextField("TEXT", document.get("TEXT"), Field.Store.NO));
		writer.addDocument(lDoc);
	}

	// Derive Stats using files generated by Lucene
	static void statsInfo(String indexFilePath) throws IOException
	{
		String index = indexFilePath;
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths
				.get(index)));

		System.out.println("Total number of documents in the corpus: " + reader.maxDoc());

		Terms vocabulary = MultiFields.getTerms(reader, "TEXT");		

		System.out.println("Number of tokens for this field: " + vocabulary.getSumTotalTermFreq());

		TermsEnum iterator = vocabulary.iterator();

		BytesRef byteReference = null;
		int count=0;
		while ((byteReference = iterator.next()) != null) {
			count++;
		}
		System.out.println("Number of Vocabulary : " + count);
		reader.close();
	}


}

