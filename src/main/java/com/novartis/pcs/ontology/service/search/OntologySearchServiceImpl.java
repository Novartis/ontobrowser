/* 

Copyright 2015 Novartis Institutes for Biomedical Research

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package com.novartis.pcs.ontology.service.search;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.TokenMgrError;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLEncoder;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.util.Version;

import com.novartis.pcs.ontology.dao.TermDAOLocal;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.search.result.HTMLSearchResult;
import com.novartis.pcs.ontology.service.search.result.InvalidQuerySyntaxException;
import com.novartis.pcs.ontology.service.util.StatusChecker;

// Lucene FAQ recommends only creating one InderSearcher:
// http://wiki.apache.org/lucene-java/LuceneFAQ#Is_the_IndexSearcher_thread-safe.3F

// Also note that although the lucene index searcher is thread-safe
// we need a read/write lock to reopen (i.e. refresh) the index
// reader after new data has been commited to the index. The rw lock
// ensures that the reader is not being used when it is closed.

@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
@Startup
@Local(OntologySearchServiceLocal.class)
@Remote(OntologySearchServiceRemote.class)
public class OntologySearchServiceImpl implements 
		OntologySearchServiceRemote, OntologySearchServiceLocal,
		LuceneTransactionListener {
	public static final String FIELD_ONTOLOGY = "ontology";
	public static final String FIELD_ID = "id";
	public static final String FIELD_TERM = "term";
	public static final String FIELD_SYNONYM = "synonym";
	
	private static final int MAX_CHARS = 1024;
				
	private Logger logger = Logger.getLogger(getClass().getName()); 
	
	// See https://java.net/jira/browse/JAVAEE_SPEC-8
	@Resource(mappedName="java:jboss/TransactionManager")
	private TransactionManager tm;
	
	@Resource(lookup="java:global/ontobrowser/index/path")
	private String path;
	
	@EJB
	protected TermDAOLocal termDAO;
	
	private Directory directory;
	private IndexWriter writer;
	private LuceneIndexWriterXAResource xar;
	private IndexReader reader;
	private IndexSearcher searcher;
	private ReadWriteLock rwlock;
	private Exception exception;
	private int numberOfDocuments;
	
    /**
     * Default constructor. 
     */
    public OntologySearchServiceImpl() {
        
    }
			
	@PostConstruct
	public void start() {
		try {
			Analyzer analyzer = new TermNameAnalyzer(true);
			File indexPath = new File(path);
			
			if(!indexPath.exists()) {
				indexPath.mkdirs();
			}
			
			directory = new MMapDirectory(indexPath, new NativeFSLockFactory());
			if(MMapDirectory.UNMAP_SUPPORTED) {
				((MMapDirectory)directory).setUseUnmap(true);
			}
			boolean indexExists = IndexReader.indexExists(directory);
			
			writer = new IndexWriter(directory, analyzer, 
					new IndexWriter.MaxFieldLength(MAX_CHARS));
			
			if(!indexExists) {
				logger.info("Building ontology search index.");
				Collection<Term> terms = termDAO.loadAll();
				for(Term term : terms) {
					if(StatusChecker.isValid(term)) {
						Collection<Document> docs = createDocuments(term);
						for(Document doc : docs) {
							writer.addDocument(doc);
						}
					}
				}
			}
			writer.optimize();
			writer.commit();
			numberOfDocuments = writer.numDocs();
			xar = new LuceneIndexWriterXAResource(writer, this);
			reader = IndexReader.open(directory, true);
			searcher = new IndexSearcher(reader);
			rwlock = new ReentrantReadWriteLock();
		} catch(Exception e) {
			logger.log(Level.WARNING, "Failed to start Lucene term searcher", e);
			stop();
			throw new RuntimeException("Failed to start Lucene term searcher", e);
		}
	}

	@PreDestroy
	public void stop() {
		close(searcher);
		close(reader); 
		close(writer);
		close(directory);
	}
	
	@Override
	public void delete(Term term) {
		org.apache.lucene.index.Term id = new org.apache.lucene.index.Term(FIELD_ID, term.getReferenceId());
				
		try {
			Transaction transaction = tm.getTransaction();
			if(!transaction.enlistResource(xar)) {
				String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
				logger.log(Level.WARNING, msg);
				throw new RuntimeException(msg);
			}
			
			logger.info("Deleting term from search index: " + term);
			writer.deleteDocuments(id);
		} catch (SystemException e) {
			String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg);
		} catch (RollbackException e) {
			String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		} catch( IOException e) {
			String msg = "Failed to write to Lucene due to IO error";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		}
	}
		
	@Override
	public void update(Term term) {
		org.apache.lucene.index.Term id = new org.apache.lucene.index.Term(FIELD_ID, term.getReferenceId());
				
		try {
			Transaction transaction = tm.getTransaction();
			if(!transaction.enlistResource(xar)) {
				String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
				logger.log(Level.WARNING, msg);
				throw new RuntimeException(msg);
			}
			
			logger.info("Deleting term from search index: " + term);
			writer.deleteDocuments(id);		
			if(StatusChecker.isValid(term)) {
				Collection<Document> docs = createDocuments(term);
				for(Document doc : docs) {
					writer.addDocument(doc);
				}
			}
		} catch (SystemException e) {
			String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg);
		} catch (RollbackException e) {
			String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		} catch( IOException e) {
			String msg = "Failed to write to Lucene due to IO error";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		}
	}
	
	@Override
	public void update(Collection<Term> terms) {
		org.apache.lucene.index.Term[] ids = new org.apache.lucene.index.Term[terms.size()];
		Collection<Document> docs = new ArrayList<Document>(terms.size());
		int i = 0;
		for(Term term : terms) {
			ids[i++] = new org.apache.lucene.index.Term(FIELD_ID, term.getReferenceId());
			if(StatusChecker.isValid(term)) {
				docs.addAll(createDocuments(term));
			}
		}
		
		try {
			Transaction transaction = tm.getTransaction();
			if(!transaction.enlistResource(xar)) {
				String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
				logger.log(Level.WARNING, msg);
				throw new RuntimeException(msg);
			}
			
			writer.deleteDocuments(ids);
			for(Document doc : docs) {
				writer.addDocument(doc);
			}
		} catch (SystemException e) {
			String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg);
		} catch (RollbackException e) {
			String msg = "Failed to enlist Lucene index writer XA resouce in transaction";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		} catch( IOException e) {
			String msg = "Failed to write to Lucene due to IO error";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		}
	}
	
	@Override
	public List<HTMLSearchResult> search(String pattern, boolean includeSynonyms) 
			throws InvalidQuerySyntaxException {		
		Analyzer analyzer = null;
		
		// default QueryParser.escape(pattern) method does not support phrase queries
		pattern = QuerySyntaxUtil.escapeQueryPattern(pattern);
		if(pattern.length() < EdgeNGramTokenFilter.DEFAULT_MIN_GRAM_SIZE) {
			return Collections.emptyList();
		}
		
		logger.log(Level.FINE, "Escaped search pattern: " + pattern);
		
		Lock lock = rwlock.readLock();
		lock.lock();
		if(exception != null) {
			lock.unlock();
			throw new RuntimeException(
					"Failed to refesh index reader after last commit", exception);
		}
		
		try {
			List<HTMLSearchResult> results = new ArrayList<HTMLSearchResult>();
			analyzer = new TermNameAnalyzer(false);
			
			QueryParser parser = new QueryParser(Version.LUCENE_30, FIELD_TERM, analyzer);
			Query query = parser.parse(pattern);
			
			logger.log(Level.FINE, "Query: " + query);
			
			// For highlighting words in query results
			QueryScorer scorer = new QueryScorer(query, reader, FIELD_TERM);
			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
			SimpleHTMLEncoder htmlEncoder = new SimpleHTMLEncoder();
			Highlighter highlighter = new Highlighter(htmlFormatter, htmlEncoder, scorer);
			highlighter.setMaxDocCharsToAnalyze(MAX_CHARS);		
			scorer.setExpandMultiTermQuery(true);
			
			// Perform search
			ScoreDoc[] hits = searcher.search(query, numberOfDocuments).scoreDocs;
			for (int i = 0; i < hits.length; i++) {
				int id = hits[i].doc;
				Document doc = searcher.doc(id);
				String ontology = doc.get(FIELD_ONTOLOGY);
				String referenceId = doc.get(FIELD_ID);
				String term = doc.get(FIELD_TERM);
				byte[] synonymBytes = doc.getBinaryValue(FIELD_SYNONYM);
				boolean isSynonym = synonymBytes != null 
					&& synonymBytes.length == 1 
					&& synonymBytes[0] == 1;
				
				if(!isSynonym || includeSynonyms) {
					Analyzer highlighterAnalyzer = new TermNameAnalyzer(true);
					TokenStream tokenStream = TokenSources.getTokenStream(reader, id, FIELD_TERM, highlighterAnalyzer);
					TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, term, true, 1);
					if(frag.length > 0 && frag[0] != null && frag[0].getScore() > 0) {
						results.add(new HTMLSearchResult(ontology, referenceId, term, 
								frag[0].toString(), frag[0].getScore(), isSynonym));
				    }
					highlighterAnalyzer.close();
				}
			}
			
			return results;
		} catch(ParseException e) {
			throw new InvalidQuerySyntaxException(e.getMessage(), e);
		} catch(TokenMgrError e) {
			throw new InvalidQuerySyntaxException(e.getMessage(), e);
		} catch (Throwable e) {
			String msg = "Failed to perform Lucene seach with pattern: " + pattern;
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		} finally {
			close(analyzer);
			lock.unlock();
		}
	}
	
	private Collection<Document> createDocuments(Term term) {
		Collection<Document> docs = new ArrayList<Document>(
				term.getSynonyms().size() + 1);
		docs.add(createDocument(term, term.getName(), false));
		
		// Same synonym from multiple datasources can exist so remove duplicates 
		Set<String> synonyms = new LinkedHashSet<String>(term.getSynonyms().size());
		for (Synonym synonym : term.getSynonyms()) {
			String trimmed = synonym.getSynonym().trim();
			if(StatusChecker.isValid(synonym)
					&& !term.getName().equalsIgnoreCase(trimmed)) {
				synonyms.add(trimmed);
			}
		}
		
		for (String synonym : synonyms) {
			docs.add(createDocument(term, synonym, true));
		}
		
		return docs;
	}
	
	private Document createDocument(Term term, String value, boolean synonym) {
		Document doc = new Document();
		
		Field ontologyField = new Field(FIELD_ONTOLOGY, 
	    		term.getOntology().getName(), 
	    		Field.Store.YES, 
	    		Field.Index.NO,
	    		TermVector.NO);
		ontologyField.setOmitNorms(true);
		ontologyField.setOmitTermFreqAndPositions(true);
		doc.add(ontologyField);
		
		Field idField = new Field(FIELD_ID, 
	    		term.getReferenceId(), 
	    		Field.Store.YES, 
	    		Field.Index.NOT_ANALYZED,
	    		TermVector.NO);
		idField.setOmitNorms(true);
		idField.setOmitTermFreqAndPositions(true);
		doc.add(idField);
		
		Field nameField = new Field(FIELD_TERM, 
		   		value, 
		   		Field.Store.YES, 
		   		Field.Index.ANALYZED,
		   		TermVector.WITH_POSITIONS_OFFSETS);
		//nameField.setOmitNorms(true);
		doc.add(nameField);
		
		doc.add(new Field(FIELD_SYNONYM,
				synonym ? new byte[] {1} : new byte[] {0},	
				Field.Store.YES));
		
		return doc;
	}
	
	@Override
	public void afterCommit() {
		logger.info("New data has been committed to index. Refreshing searcher");
		Lock lock = rwlock.writeLock();
		lock.lock();
		try {			
			IndexReader newReader = reader.reopen();
			if(newReader != reader) {
				// This won't close the reader but rather decrement
				// it's internal reference count. The reader will be
				// closed internally when the refCount == 0. Therefore
				// other threads can continue using the reader while
				// this thread reopens a new one with the latest index
				reader.close();
				reader = newReader;
				searcher = new IndexSearcher(reader);
				numberOfDocuments = reader.numDocs();
			}
		} catch(Exception e) { 
			logger.log(Level.SEVERE, "Failed to reopen/refresh index reader after commit", e);
			exception = e;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void afterRollback(IndexWriter newWriter) {
		logger.info("Lucene transaction rolled back. Assigning new index writer.");
		// Don't need to synchronize because it is handled within LuceneIndexWriterXAResource
		writer = newWriter;
	}
	
	private void close(Closeable closeable) {
		if(closeable != null) {
			try {
				closeable.close();
			} catch(Exception e) {
				logger.log(Level.WARNING, "Failed to close: " + closeable, e);
			}
		}
	}
}
