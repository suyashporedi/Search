import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.PriorityQueue;
import java.util.Scanner;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;

public class searchTRECtopics {

	private static String SHORT_QUERY = "title";
	private static String LONG_QUERY = "description";

	// Function to extract Title(ShortQuery) from the given
	public static String extractTitle(String query) {
		String title = "";
		int titlePos = query.indexOf(":");
		if (titlePos != -1) {
			title = query.substring(titlePos + 1, query.length());
		}
		return title;
	}

	// Function to extract Title(ShortQuery) from the given
	public static String extractDescription(String query) {
		int descStart = query.indexOf("<desc>");
		int descEnd = query.indexOf("<", descStart + 1);
		String desc = query.substring(descStart + 6, descEnd);
		desc = desc.replace("Description:", " ");
		desc = desc.replace('?', ' ');
		desc = desc.replace('/', ' ');

		return desc;
	}

	public static void main(String[] args) throws IOException, ParseException {

		int queryChoice = 0;
		TrecTopicsReader trecTopicReader = new TrecTopicsReader();
		BufferedReader bufferedReader = new BufferedReader(new FileReader("./topics.51-100"));
		QualityQuery[] qualityQueries = trecTopicReader.readQueries(bufferedReader);
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("./index")));

		do {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println(" Choose Option(1-2) for Query Fetching\n 1)Short Query\n 2)Long Query\n 3)Exit\n");
			queryChoice = input.nextInt();
			switch (queryChoice) {
			case 1:
				executeResult(qualityQueries, reader, SHORT_QUERY, "ShortQuery");
				break;
			case 2:
				executeResult(qualityQueries, reader, LONG_QUERY, "LongQuery");
				break;

			case 3:

				break;
			}

		} while (queryChoice < 3);
	}

	// Calculate the score for each document using algorithm defined in
	// easySearch.java by calling 'calTFIDF' function
	public static void executeResult(QualityQuery[] qualityQueries, IndexReader reader, String queryTyp,
			String FileName) throws IOException, ParseException {
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
			// DocScoreData is a storage class which is used to store the scores and docid of the document 
			PriorityQueue<DocScoreData> pQueue = new PriorityQueue<DocScoreData>();
			pQueue = easySearch.calTFIDF(reader, extractedQuery);
			String fileName = "./" + FileName + ".txt";

			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true));

			int count = 0;
			while (!pQueue.isEmpty()) {
				DocScoreData dcd = pQueue.remove();
				++count;
				bw.append(queryID + "\t" + "Q0" + "\t" + dcd.getKey() + "\t" + count + "\t" + dcd.getValue() + "\t"
						+ "run-1 \n");

				if (count == 1000)
					break;
			}
			bw.flush();
			bw.close();

		}
		System.out.println("Queries from TREC 51-100 executed successfully for " + FileName);
	}

}
