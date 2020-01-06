import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class RochioAlgo {
	private static Analyzer analyzer;

	public static void main (String args[]) throws ParseException, IOException {
		
		//inkedHashMap<String,String> topIdTitle = new LinkedHashMap<String, String>(); 
		HashMap<String, HashMap<String,HashMap<String, Double>>> topIdRelNonrelVectors = new HashMap<String, HashMap<String,HashMap<String, Double>>>();
		
		String filePath = "./docs/topic judgement for feedback.txt";
		File topicFile =  new File (filePath);
		FileInputStream fis = new FileInputStream(topicFile);
		BufferedReader bufreader = new BufferedReader(new InputStreamReader(fis));
        
		String line = bufreader.readLine();
		StringBuilder strbul = new StringBuilder();
		        
		while(line != null){
			strbul.append(line).append("\n");
		   line = bufreader.readLine();
		}
		bufreader.close();  
		String fileAsString = strbul.toString();
		
		Pattern topPattern = Pattern.compile("<top>(.+?)</top>", Pattern.DOTALL);
		Matcher matcher = topPattern.matcher(fileAsString);
	    List<String> topList = new ArrayList<String>();
	    while (matcher.find()) {
			topList.add(matcher.group(1).trim());
	    }
		
		Iterator<String> topIterator = topList.iterator();
		while (topIterator.hasNext()) {
			String tempString = topIterator.next();
			List<String> relevantList = new ArrayList<String>();
			List<String> irrelevantList = new ArrayList<String>();
			String topicNum = "";
			List<String> topicTitle=new ArrayList<String>();
			Pattern numPattern = Pattern.compile("<num>(.+?)<title>", Pattern.DOTALL);
			Matcher m = numPattern.matcher(tempString);

			while (m.find()) {
				topicNum=m.group(1).trim();
		    }
			m = Pattern.compile("<title>(.+?)\n", Pattern.DOTALL)
					.matcher(tempString);
			while (m.find()) {
				topicTitle.add(m.group(1).trim());
		    }
			
			m = Pattern.compile("<relevant>(.+?)\n", Pattern.DOTALL)
						.matcher(tempString);
				while (m.find()) {
						relevantList.add(m.group(1).trim());
			}
			
			m = Pattern.compile("<irrelevant>(.+?)\n", Pattern.DOTALL)
					.matcher(tempString);
			while (m.find()) {
				irrelevantList.add(m.group(1).trim());
			}
			
			
			int rc=relevantList.size();
			int nrc=irrelevantList.size();
			Map<String,Double> titleVector=FeatureVector(topicTitle);
			Map<String,Double> relevantVector = FeatureVector(relevantList);
			Map<String, Double> irrelVector = FeatureVector(irrelevantList);
			
			
			for (String relfeat: relevantVector.keySet()) {
				relevantVector.put(relfeat,relevantVector.get(relfeat)/rc);
			}
			
			
			for (String nonrelfeat: irrelVector.keySet()) {
				irrelVector.put(nonrelfeat,irrelVector.get(nonrelfeat)/nrc);
			}
			
			HashMap<String,HashMap<String, Double>> relNonrelVec=new HashMap<String, HashMap<String,Double>>();
			relNonrelVec.put("title", (HashMap<String, Double>) titleVector);
			relNonrelVec.put("relevant", (HashMap<String, Double>) relevantVector);
			relNonrelVec.put("irrelevant", (HashMap<String, Double>) irrelVector);

			topIdRelNonrelVectors.put(topicNum,relNonrelVec);
		}
		queryExpansion(topIdRelNonrelVectors);
	}
	

	
	public static void queryExpansion(HashMap<String,HashMap<String,HashMap<String, Double>>> fileData) throws IOException,ParseException{
		List<Double> betaValues = Arrays.asList(0.2,0.4,0.6,0.8,1.0);
		List<Double> gammaValues = Arrays.asList(0.0,0.2,0.4,0.6,0.8,1.0);
		String index = "./index";
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
		IndexSearcher indexSearcher = new IndexSearcher(reader);
		Analyzer analyzer = new SimpleAnalyzer();
		LinkedHashMap<Integer,String> expandedQueryMap = new LinkedHashMap<Integer,String>();
		
		for (Double beta: betaValues) {
			for (Double gamma: gammaValues) {
				for(int id=51;id<101;id++) {
					String s= Integer.toString(id);
					HashMap<String, Double> queryVector = fileData.get(s).get("title");
					HashMap<String, Double> relevantVector = new HashMap<String, Double>();
					HashMap<String, Double> nonrelevantVector = new HashMap<String, Double>();
					
					// relevant document vector * beta
					for (String rel: fileData.get(s).get("relevant").keySet()) {
						relevantVector.put(rel,fileData.get(s).get("relevant").get(rel)*beta);
						
					}
					// irrelevant document vector * gamma 
					
					for (String nonrel: fileData.get(s).get("irrelevant").keySet()) {
						nonrelevantVector.put(nonrel,fileData.get(s).get("irrelevant").get(nonrel)*gamma);
						
					}
					
					HashMap<String,Double> tempQueryVector = subVectors(addVectors(queryVector, relevantVector),nonrelevantVector);
					LinkedHashMap<String,Double> sortedQuery = sortingList(tempQueryVector);
					ArrayList<String> queryResult = new ArrayList<String>();
					
					for (String feat: sortedQuery.keySet()) 
					{
						if (sortedQuery.get(feat)>=0) {
							queryResult.add(feat);}
						if(queryResult.size()>500) {
							break;
						}
					}
					
					
					DecimalFormat decimal = new DecimalFormat("0.0");
					StringBuffer expandedQuery = new StringBuffer();
					
					// Form the query in form query_word ^ count
					for (String term : queryResult) {
						expandedQuery.append(term + "^" + decimal.format(tempQueryVector.get(term)) + " ");
					}
					//System.out.println(id+"  :  " + expandedQuery.toString());			
					expandedQueryMap.put(id,expandedQuery.toString());
				}
				String path = "./Beta"+beta+"Gamma"+gamma;
				executeResult(expandedQueryMap, reader, path,analyzer,indexSearcher,new ClassicSimilarity());
				System.out.println("Done writing in file"+path);
				
			}
		}
			
		}
	
	public static void executeResult(LinkedHashMap<Integer,String> queries,IndexReader reader,
			String FileName, Analyzer analyzer, IndexSearcher indexSearcher, Similarity similarity)
			throws IOException, ParseException {
		indexSearcher.setSimilarity(similarity);
		QueryParser queryParser = new QueryParser("TEXT", analyzer);
		for (Integer qid: queries.keySet()) {
			//System.out.println(queries.get(qid));
			Integer queryID = qid;
			Query query = queryParser.parse(queries.get(qid));
			String fileName = "./betaGamma/" + FileName + ".txt";
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
		
		System.out.println("Queries executed successfully for " + FileName);
	}
	
	
	//Referred to the code shared by the professor site for  https://github.com/gtsherman/lucene/blob/master/src/main/java/org/retrievable/lucene/searching/expansion/Rocchio.java 
private static List<String> analyze(List<String> list) {
	String text=""; 
	Iterator<String> listIterator = list.iterator();
	while (listIterator.hasNext()) {
	
		text = text+" "+ listIterator.next();
	}
	analyzer = new SimpleAnalyzer();
	List<String> result = new LinkedList<String>();
	try {
		TokenStream stream = null;
		stream = analyzer.tokenStream("text", new StringReader(text));

		CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);
		stream.reset();
		while(stream.incrementToken()) {
			String term = charTermAttribute.toString();
			result.add(term);
		}
	} catch (IOException e) {
		e.printStackTrace();
	}
	
	return result;
}

//Referred to the code shared by the professor site for  https://github.com/gtsherman/lucene/blob/master/src/main/java/org/retrievable/lucene/searching/expansion/Rocchio.java 

public static HashMap<String, Double> FeatureVector(List<String> text) {
	HashMap<String, Double> features = new HashMap<String, Double>();
	List<String> terms = analyze(text);
	Iterator<String> termsIt = terms.iterator();
	while(termsIt.hasNext()) {
		String term = termsIt.next();
		Double val = (Double)features.get(term);
		if(val == null) {
			features.put(term, new Double(1.0));
		} else {
			double v = val.doubleValue() + 1.0;
			features.put(term, new Double(v));
		}
	}
	return(features);
}

//Referred to the code shared by the professor site for  https://github.com/gtsherman/lucene/blob/master/src/main/java/org/retrievable/lucene/searching/expansion/Rocchio.java 

public static HashMap<String,Double> addVectors (HashMap<String,Double> vector1, HashMap<String,Double> vector2) {
	HashMap<String,Double> resultVector = new HashMap<String,Double>();
	Set<String> v2Features = vector2.keySet();
	for (String v1eachFeat: vector1.keySet()) {
		if (v2Features.contains(v1eachFeat)){
			resultVector.put(v1eachFeat,vector1.get(v1eachFeat)+vector2.get(v1eachFeat));}
		else {
			resultVector.put(v1eachFeat,vector1.get(v1eachFeat));
		}
	}
	
	v2Features.removeAll(resultVector.keySet());
	for (String v2eachFeat: v2Features) {
			resultVector.put(v2eachFeat,vector2.get(v2eachFeat));
		}
	return(resultVector);
}

//Referred to the code shared by the professor site for  https://github.com/gtsherman/lucene/blob/master/src/main/java/org/retrievable/lucene/searching/expansion/Rocchio.java 

 
public static HashMap<String,Double> subVectors (HashMap<String,Double> vector1, HashMap<String,Double> vector2) {
	HashMap<String,Double> resultVector = new HashMap<String,Double>();
	Set<String> v2Features = vector2.keySet();
	for (String v1eachFeat: vector1.keySet()) {
		if (v2Features.contains(v1eachFeat)){
			resultVector.put(v1eachFeat,vector1.get(v1eachFeat)-vector2.get(v1eachFeat));}
		else {
			resultVector.put(v1eachFeat,vector1.get(v1eachFeat));
		}
	}
	
	return(resultVector);
}

public static LinkedHashMap<String, Double> sortingList(HashMap<String, Double> hm) 
{ 
	//Referred https://www.baeldung.com/java-hashmap-sort
     
    List<Map.Entry<String, Double> > list = 
           new LinkedList<Map.Entry<String, Double> >(hm.entrySet()); 

    
    Collections.sort(list, new Comparator<Map.Entry<String, Double> >() { 
        public int compare(Map.Entry<String, Double> o1,  
                           Map.Entry<String, Double> o2) 
        { 
            return (o2.getValue()).compareTo(o1.getValue()); 
        } 
    }); 
      
      
    LinkedHashMap<String, Double> temp = new LinkedHashMap<String, Double>(); 
    for (Map.Entry<String, Double> aa : list) { 
        temp.put(aa.getKey(), aa.getValue()); 
    } 
    return temp; 
} 
}
