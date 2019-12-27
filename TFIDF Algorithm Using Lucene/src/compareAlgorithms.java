import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class compareAlgorithms {
	private static String SHORT_QUERY = "title";
	private static String LONG_QUERY = "description";
	private static String trecTopics = "./topics.51-100";
	private static String index = "./index";

	// Function to extract Title(ShortQuery) from the given 
	public static String extractTitle(String query) {
		String title = "";
		int titlePosition = query.indexOf(":");
		if (titlePosition != -1) {
			title = query.substring(titlePosition + 1, query.length());
		}
		return title;
	}

	public static String extractDescription(String query) {
		int descBegin = query.indexOf("<desc>");
		int descEnd = query.indexOf("<", descBegin + 1);
		String desc = query.substring(descBegin + 6, descEnd);
		desc = desc.replace("Description:", " ");
		desc = desc.replace('?', ' ');
		desc = desc.replace('/', ' ');

		return desc;
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		int queryChoice = 0;
		TrecTopicsReader trecTopicReader = new TrecTopicsReader();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(trecTopics));
		QualityQuery[] qualityQueries = trecTopicReader.readQueries(bufferedReader);
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
		IndexSearcher indexSearcher = new IndexSearcher(reader);
		StandardAnalyzer analyzer = new StandardAnalyzer();

		do {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("Choose Option(1-2) for QueryType for(4 Models) \n1)Short Query\n2)Long Query\n3)Exit\n");
			queryChoice = input.nextInt();
			switch (queryChoice) {
			case 1: 
				executeResult(qualityQueries, reader, SHORT_QUERY, "ClassicSim_ShortQuery", analyzer, indexSearcher,
						new ClassicSimilarity());
				executeResult(qualityQueries, reader, SHORT_QUERY, "BM25Sim_ShortQuery", analyzer, indexSearcher,
						new BM25Similarity());
				executeResult(qualityQueries, reader, SHORT_QUERY, "LMDSim_ShortQuery", analyzer, indexSearcher,
						new LMDirichletSimilarity());
				executeResult(qualityQueries, reader, SHORT_QUERY, "LMJMSim_ShortQuery", analyzer, indexSearcher,
						new LMJelinekMercerSimilarity((float) 0.7));

				break;

			case 2: 
				executeResult(qualityQueries, reader, LONG_QUERY, "ClassicSim_LongQuery", analyzer, indexSearcher,
						new ClassicSimilarity());
				executeResult(qualityQueries, reader, LONG_QUERY, "BM25Sim_LongQuery", analyzer, indexSearcher,
						new BM25Similarity());
				executeResult(qualityQueries, reader, LONG_QUERY, "LMDSim_LongQuery", analyzer, indexSearcher,
						new LMDirichletSimilarity());
				executeResult(qualityQueries, reader, LONG_QUERY, "LMJMSim_LongQuery", analyzer, indexSearcher,
						new LMJelinekMercerSimilarity((float) 0.7));

				break;

			case 3:

				break;
			}

		} while (queryChoice < 3);
	}

	public static void executeResult(QualityQuery[] qualityQueries, IndexReader reader, String queryTyp,
			String FileName, StandardAnalyzer analyzer, IndexSearcher indexSearcher, Similarity similarity)
			throws IOException, ParseException {
		indexSearcher.setSimilarity(similarity);
		QueryParser queryParser = new QueryParser("TEXT", analyzer);

		for (int i = 0; i < qualityQueries.length; i++) {
			QualityQuery qualityQuery = qualityQueries[i];
			String queryID = qualityQuery.getQueryID();

			String queryType = "";
			String extractedQuery = "";

			if (queryTyp.equals(SHORT_QUERY)) {
				queryType = qualityQuery.getValue(queryTyp);
				extractedQuery = extractTitle(queryType);
			} else {
				queryType = qualityQuery.getValue("description");
				extractedQuery = extractDescription(queryType);
			}

			Query query = queryParser.parse(QueryParserUtil.escape(extractedQuery));

			String fileName = "./" + FileName + ".txt";

			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true));

			TopDocs results = indexSearcher.search(query, 1000);

			ScoreDoc[] hits = results.scoreDocs;

			for (int j = 0; j < hits.length; j++) {
				Document doc = indexSearcher.doc(hits[j].doc);

				try {
					bw.append(queryID + "\t" + "Q0" + "\t" + doc.get("DOCNO") + "\t" + (j + 1) + "\t" + hits[j].score
							+ "\t" + "run-1 \n");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			bw.flush();
			bw.close();

		}
		System.out.println("Queries from TREC 51-100 executed successfully for " + FileName);
	}

}

// References 
/*
 * 1) For TrecTopicsReader :
 * https://lucene.apache.org/core/2_9_4/api/contrib-benchmark/org/apache/lucene/benchmark/quality/trec/TrecTopicsReader.html
 * 2) For Fetching Query from string
 * https://stackoverflow.com/questions/50958593/substring-extract-between-an-xml-string-java/50960268
 * 
 */
