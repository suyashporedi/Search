import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


public class Stats {
	public static void main(String[] args) throws Exception {
		String index = "./index";
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths
				.get(index)));

		System.out.println("Total number of documents in the corpus: "
				+ reader.maxDoc());

		Terms vocabulary = MultiFields.getTerms(reader, "TEXT");		
		System.out.println("Size of the vocabulary for this field: "
				+ vocabulary.size());

		System.out
				.println("Number of documents that have at least one term for this field: "
						+ vocabulary.getDocCount());

		System.out.println("Number of tokens for this field: "
				+ vocabulary.getSumTotalTermFreq());

		System.out.println("Number of postings for this field: "
				+ vocabulary.getSumDocFreq());

//		TermsEnum iterator = vocabulary.iterator();
//		BytesRef byteRef = null;
//		System.out.println("\n*******Vocabulary-Start**********");
//		while ((byteRef = iterator.next()) != null) {
//			String term = byteRef.utf8ToString();
//			System.out.println(term);
//		}
//		System.out.println("\n*******Vocabulary-End**********");

		reader.close();
	}
}
