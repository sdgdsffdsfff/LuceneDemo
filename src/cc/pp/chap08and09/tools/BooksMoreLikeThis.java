package cc.pp.chap08and09.tools;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class BooksMoreLikeThis {

	/**
	 * 测试函数
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		Directory dir = FSDirectory.open(new File("index/chap03index/"));
		IndexReader reader = IndexReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);

		int numDocs = reader.maxDoc();

		MoreLikeThis mlt = new MoreLikeThis(reader);
		mlt.setFieldNames(new String[] { "title", "author" });
		mlt.setMinTermFreq(1);
		mlt.setMinDocFreq(1);

		for (int docID = 0; docID < numDocs; docID++) {
			System.out.println();
			Document doc = reader.document(docID);
			System.out.println(doc.get("title"));

			Query query = mlt.like(docID);
			System.out.println(" query=" + query);

			TopDocs similarDocs = searcher.search(query, 10);
			if (similarDocs.totalHits == 0) {
				System.out.println(" None like this");
			}
			for (int i = 0; i < similarDocs.scoreDocs.length; i++) {
				if (similarDocs.scoreDocs[i].doc != docID) {
					doc = reader.document(similarDocs.scoreDocs[i].doc);
					System.out.println("  -> " + doc.getField("title").stringValue());
				}
			}
		}

		searcher.close();
		reader.clone();
		dir.close();

	}

}
