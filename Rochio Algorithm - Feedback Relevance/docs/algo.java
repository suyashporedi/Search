import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

public class RocchioAlgortithm {

	public static Analyzer analyzer = new StandardAnalyzer();
    public static IndexWriterConfig config = new IndexWriterConfig(analyzer);
    public static RAMDirectory ramDirectory = new RAMDirectory();
    public static IndexWriter indexWriter;
    
	public static void main(String[] args) throws IOException, ParseException {
		List<ArrayList<String>> docList = getDocList();
		
		ArrayList<String> titleList = new ArrayList<String>();
		ArrayList<String> relevantCount = docList.get(4);
		ArrayList<String> nonRelevantCount = docList.get(5);
		
        IndexWriter writer = indexer();
        indexDoc(writer, docList);
		writer.close();
		
		String indexPath = "./RocchioIndex";
		IndexReader idxReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
		
        double alpha = 1, beta = 1, gamma = 0;
		
		for (int in = 0; in < 50; in++) {
			Set<String> myterms = new LinkedHashSet<String>(); 
		    Map<String, Integer> initQuery = getTermFrequencies(idxReader, in, "TITLE", myterms);
			Map<String, Integer> relDocs = getTermFrequencies(idxReader, in, "RELEVANT", myterms);
			Map<String, Integer> nonRelDocs = getTermFrequencies(idxReader, in, "NONRELEVANT", myterms);
			RealVector q0 = toRealVector(initQuery, myterms);
			RealVector Dr = toRealVector(relDocs, myterms);
			RealVector Dnr = toRealVector(nonRelDocs, myterms);
			//System.out.println(myterms);
			//System.out.println(initQuery);
			//System.out.println(relDocs);
			//System.out.println(nonRelDocs);
			//System.out.println(q0);
			//System.out.println(Dr);
			//System.out.println(Dnr);
			int rc = Integer.parseInt(relevantCount.get(in));
			int nrc = Integer.parseInt(nonRelevantCount.get(in));
			RealVector qM = rc != 0
					? (q0.mapMultiply(alpha)).add(Dr.mapMultiply(beta / rc)).subtract(Dnr.mapMultiply(gamma / nrc))
					: (q0.mapMultiply(alpha)).subtract(Dnr.mapMultiply(gamma / nrc));
			//System.out.println(qM);
			String newQuery = "";
			for (int i = 0; i < qM.getDimension(); i++) {
				//System.out.println(qM.getEntry(i));
				if (qM.getEntry(i) > 0) {
					newQuery += new ArrayList<>(myterms).get(i) + " ";
				}
			}
			//System.out.println(newQuery);
			titleList.add(newQuery);
		}
		int i = 51;
		for(String title:titleList)
		{
			//if(title.length() == 0)
				//System.out.println(titleList.indexOf(title));
			System.out.println(i+": "+title);
			i++;
		}
			
		//System.out.println(titleList);
		
		//Evaluation
		
		Similarity s1 = new ClassicSimilarity();	
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("./index")));
		IndexSearcher searcher1 = new IndexSearcher(reader); 
		String DefaultShortQuery = ".\\docs\\RocchioResultsBeta_"+alpha+"Gamma_"+gamma+".txt"; 
		FileWriter defaultShortWriter=new FileWriter(DefaultShortQuery);  
		String queryString;
		for(int in=0; in<50; in++) {
			//System.out.println(descList.get(in));
			queryString = titleList.get(in);
			compareAlgorithms.searchQueryUsingAlgo(in, "short",queryString, s1, searcher1, defaultShortWriter);
		}
		defaultShortWriter.close();


		
		
	}
	public static List<ArrayList<String>> getDocList() throws IOException {
		List<ArrayList<String>> docList = new ArrayList<ArrayList<String>>();
		File file = new File("./docs/topic judgement for feedback.txt");
		StringBuffer sb = new StringBuffer();
		ArrayList<String> numList = new ArrayList<String>(); 
		ArrayList<String> titleList = new ArrayList<String>(); 
		ArrayList<String> relevantList = new ArrayList<String>(); 
		ArrayList<String> nonRelevantList = new ArrayList<String>(); 
		ArrayList<String> relCounts = new ArrayList<String>(); 
		ArrayList<String> nonRelCounts = new ArrayList<String>(); 

		if (file.isFile()) {
			try (BufferedReader br  = new BufferedReader(new FileReader(file))) {
				String strCurrentLine;
				Boolean start = false;
				Boolean stop = false;
				int rc=0,nrc=0;
				while ((strCurrentLine = br.readLine()) != null) {
					if(strCurrentLine.startsWith("<num>")) {
						start = true;
						stop = false;
					}
					if(strCurrentLine.startsWith("</top>")) {
						sb.append("</top>");
						stop = true;
						start = false;
					}
					if(start == true && stop != true) {
						sb.append(" " + strCurrentLine);
					}			
					if(strCurrentLine.startsWith("<relevant>"))
						rc++;
					if(strCurrentLine.startsWith("<irrelevant>"))
						nrc++;
					if(strCurrentLine.startsWith("</top>")) 
					{
						Matcher m = Pattern.compile("(?<=<num>).+?(?=<)", Pattern.DOTALL)
								.matcher(sb.toString());
						while (m.find()) {
							numList.add(m.group());
						}
						m = Pattern.compile("(?<=<title>).+?(?=<)", Pattern.DOTALL)
								.matcher(sb.toString());
						while (m.find()) {
							titleList.add(m.group());
						}
						if(sb.toString().contains("<relevant>")) {
							m = Pattern.compile("(?<=<relevant>).+?(?=<irrelevant>)", Pattern.DOTALL)
									.matcher(sb.toString());
							while (m.find()) {
									relevantList.add(m.group().replace("<relevant>", ""));
								}
						}
						else {
							relevantList.add("");
						}
						m = Pattern.compile("(?<=<irrelevant>).+?(?=</top>)", Pattern.DOTALL)
								.matcher(sb.toString());
						while (m.find()) {
							nonRelevantList.add(m.group().replace("<irrelevant>", ""));
						}
						relCounts.add(Integer.toString(rc));
						nonRelCounts.add(Integer.toString(nrc));
						docList.add(numList);
						docList.add(titleList);
						docList.add(relevantList);
						docList.add(nonRelevantList);
						docList.add(relCounts);
						docList.add(nonRelCounts);
						sb = new StringBuffer();
						rc=0;
						nrc=0;
					}
				}
			}
		}
		return docList;
	}
	
	private static IndexWriter indexer() {
		String indexPath = "./RocchioIndex";
		
		try {
			
			System.out.println("Indexing to directory '" + indexPath + "'...");

			Directory dir = FSDirectory.open(Paths.get(indexPath));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			iwc.setOpenMode(OpenMode.CREATE);

			IndexWriter writer = new IndexWriter(dir, iwc);
			return writer;
		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			return null;
		}
		
	}
	
	private static void indexDoc(IndexWriter writer, List<ArrayList<String>> docList) throws IOException {
		// make a new, empty document
		for(int i=0; i<docList.get(0).size(); i++) {
			Document lDoc = new Document();
			//System.out.println(docList.get(1).get(i));
			lDoc.add(new StringField("NUM", docList.get(0).get(i),Field.Store.YES));
			FieldType myFieldType = new FieldType(TextField.TYPE_STORED);
			myFieldType.setStoreTermVectors(true);
			lDoc.add(new Field("TITLE", docList.get(1).get(i), myFieldType));
			lDoc.add(new Field("RELEVANT", docList.get(2).get(i), myFieldType));
			lDoc.add(new Field("NONRELEVANT", docList.get(3).get(i), myFieldType));
			writer.addDocument(lDoc);
		}
	}

	static Map<String, Integer> getTermFrequencies(IndexReader idxReader, int docId, String content, Set<String> myterms) throws IOException {       
        Map<String, Integer> frequencies = new HashMap<>();
		try {
			//IndexSearcher idxSearcher = new IndexSearcher(idxReader);
			Terms terms = idxReader.getTermVector(docId, content); 
			if (terms != null) {
				TermsEnum termsEnum = terms.iterator();
				BytesRef bytesRef = termsEnum.next();
				while (bytesRef != null) {
					if (bytesRef != null) {
						String term = bytesRef.utf8ToString();
						int freq = (int) termsEnum.totalTermFreq();
						frequencies.put(term, freq);
						myterms.add(term);
					}
					bytesRef = termsEnum.next();
				} 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return frequencies;

    }

    static RealVector toRealVector(Map<String, Integer> map, Set<String> myterms) {
        RealVector vector = new ArrayRealVector(myterms.size());
        int i = 0;
        for (String term : myterms) {
            int value = map.containsKey(term) ? map.get(term) : 0;
            vector.setEntry(i++, value);
        }
        return vector;
    }
}
