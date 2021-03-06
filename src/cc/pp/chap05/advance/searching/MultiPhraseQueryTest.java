package cc.pp.chap05.advance.searching;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import cc.pp.chap04.analysis.synonym.SynonymAnalyzer;
import cc.pp.chap04.analysis.synonym.SynonymEngine;

public class MultiPhraseQueryTest extends TestCase {

	private IndexSearcher searcher;

	@Override
	protected void setUp() throws Exception {

		Directory dir = new RAMDirectory();
		IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(),//
				IndexWriter.MaxFieldLength.UNLIMITED);

		Document doc1 = new Document();
		doc1.add(new Field("field", "the quick brown fox jumped over the lazy dog", //
				Field.Store.YES, Field.Index.ANALYZED));
		writer.addDocument(doc1);

		Document doc2 = new Document();
		doc2.add(new Field("field", "the fast fox hopped over the hound", //
				Field.Store.YES, Field.Index.ANALYZED));
		writer.addDocument(doc2);

		writer.close();
		searcher = new IndexSearcher(dir);
	}

	public void testBasic() throws Exception {

		MultiPhraseQuery query = new MultiPhraseQuery();
		/**
		 * 默认跨度为0，按顺序
		 */
		query.add(new Term[] { new Term("field", "quick"), //
				new Term("field", "fast") }); // 这些首先被检索
		query.add(new Term("field", "fox")); // 然后被检索

		//		System.out.println(query);

		TopDocs hits = searcher.search(query, 10);
		assertEquals("fast fox match", 1, hits.totalHits);

		query.setSlop(1);
		hits = searcher.search(query, 10);
		assertEquals("both match", 2, hits.totalHits);
	}

	public void testAgainstOR() throws Exception {

		PhraseQuery quickFox = new PhraseQuery();
		quickFox.setSlop(1);
		quickFox.add(new Term("field", "quick"));
		quickFox.add(new Term("field", "fox"));

		PhraseQuery fastFox = new PhraseQuery();
		fastFox.add(new Term("field", "fast"));
		fastFox.add(new Term("field", "fox"));

		BooleanQuery query = new BooleanQuery();
		query.add(quickFox, BooleanClause.Occur.SHOULD);
		query.add(fastFox, BooleanClause.Occur.SHOULD);

		//		System.out.println(query);

		TopDocs hits = searcher.search(query, 10);

		debug(hits);

		assertEquals(2, hits.totalHits);
	}

	public void testQueryParser() throws Exception {

		SynonymEngine engine = new SynonymEngine() {
			@Override
			public String[] getSynonyms(String str) {
				if (str.equals("quick"))
					return new String[] { "fast" };
				else
					return null;
			}
		};

		Query query = new QueryParser(Version.LUCENE_30, "field", //
				new SynonymAnalyzer(engine)).parse("\"quick fox\"");

		assertEquals("analyzed", "field:\"(quick fast) fox\"", query.toString());
		assertTrue("parsed as MultiPhraseQuery", query instanceof MultiPhraseQuery);
	}

	private void debug(TopDocs hits) throws Exception {

		for (ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);
			System.out.println(sd.score + ": " + doc.get("field"));
		}
	}

}
