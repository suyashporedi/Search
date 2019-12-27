import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class easySearch {
	
	private static IndexReader reader;
	
	private static PriorityQueue<DocScoreData> pQueue;
	
	// Function to calculate TFIDF for each document from Corpus using Indexes generated in 'index' File
	public static PriorityQueue<DocScoreData> calTFIDF(IndexReader reader,String queryString) throws IOException, ParseException
	{
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		ClassicSimilarity similarity = new ClassicSimilarity();
		QueryParser parser = new QueryParser("TEXT", analyzer);
		HashMap<String,Double> docIDAndScore= new HashMap<String,Double>();
		Query query = parser.parse(QueryParserUtil.escape(queryString));
		Set<Term> queryTerms = new LinkedHashSet<Term>();
		searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
		searcher.setSimilarity(similarity);
		
		for(Term t : queryTerms )
		{
			int documentfrequency = reader.docFreq(new Term("TEXT", t.text()));
			List<LeafReaderContext> leafContexts = reader.getContext().reader().leaves();
			float docLeng=0;
			for (int i = 0; i < leafContexts.size(); i++) {
				// Get document length
				LeafReaderContext leafContext = leafContexts.get(i);
				
				int numberOfDoc = leafContext.reader().maxDoc();
				for (int docId = 0; docId < numberOfDoc; docId++) {
					// Get normalized length (1/sqrt(numOfTokens)) of the document
					float normDocLeng = similarity.decodeNormValue(leafContext.reader().getNormValues("TEXT").get(docId));
					docLeng = 1 / (normDocLeng * normDocLeng);
				}
				
				// Get frequency of the Query term from its postings
				PostingsEnum de = MultiFields.getTermDocsEnum(leafContext.reader(),"TEXT", new BytesRef(t.text()));

				int doc;
				if (de != null) {
					while ((doc = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
						int docID = de.docID() + leafContext.docBase;
						String docNO = searcher.doc(docID).get("DOCNO");
						
						// Formula for myAlgorithm
						
						double docScore =((de.freq()/docLeng) * Math.log((1 + reader.maxDoc())/(float)documentfrequency));
						
						if(!docIDAndScore.containsKey(docNO)) {
							docIDAndScore.put(docNO,docScore);
						}
						else {
							docIDAndScore.put(docNO,docScore+docIDAndScore.get(docNO));
						}
					}
				} 
			}
		}
		
		pQueue = new PriorityQueue<DocScoreData>();
		for (Map.Entry<String, Double> entry : docIDAndScore.entrySet())
		{			
			pQueue.add(new DocScoreData(entry.getKey(),entry.getValue()));
		}
		

		return pQueue;
	}
	
	public static void main(String[] args) throws ParseException, IOException
	{
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter Query String");
		String queryString = sc.nextLine();
		sc.close();
		reader = DirectoryReader.open(FSDirectory.open(Paths.get("./index")));
		pQueue= calTFIDF(reader,queryString);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("./easySearch.txt"));
        writer.write("Doc No		Score");
        writer.newLine();
        while (!pQueue.isEmpty()) {

        	DocScoreData dcd = pQueue.remove();
        	writer.append(dcd.getKey()+"  "+dcd.getValue());
            writer.newLine();
            System.out.println("Document ID = " + dcd.getKey() +" Score =" + dcd.getValue());
        }
        writer.close();
	}

	public IndexReader getReader() {
		return reader;
	}

	public void setReader(IndexReader reader) {
		this.reader = reader;
	}
	
	public PriorityQueue<DocScoreData> getpQueue() {
		return pQueue;
	}

	public void setpQueue(PriorityQueue<DocScoreData> pQueue) {
		this.pQueue = pQueue;
	}

}

// Class to store the data in the Queue using score as a comparator value
class DocScoreData implements Comparable<DocScoreData>{
    
	private String key;
    private Double value;
    public DocScoreData(String key, Double value) {
    	setKey(key);
    	setValue(value);
    }
    public Double getValue() {
		return value;
	}
	public void setValue(Double value) {
		this.value = value;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public int compareTo(DocScoreData o) {
		// Comparing w.r.t to Score value and not the String!
		return o.getValue().compareTo(this.getValue());
	}
}

/* References
 * 1) Priority Queue with Comparator :
 * https://www.geeksforgeeks.org/implement-priorityqueue-comparator-java/
*/
