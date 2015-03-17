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

import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.ThreadInterruptedException;

/* This class provides transaction support for the Lucene writer
 * so changes to the index can be performed within the same JTA
 * transaction as the database changes.
 * 
 * Only one Lucene writer can be open for an index at one time and
 * therefore it makes sense to only create one instance and reuse
 * it for all transactions. The Lucene writer is thread safe but
 * we need ensure only one transaction is in progress at one time
 * i.e. to prevent the case where T1 has made changes to the writer
 * but has not yet committed and T2 starts to make changes. This
 * results with some or all of T2's changes being committed when
 * T1 commits.
 * 
 * Although this reduces concurrency, ontologies are not expected
 * to be updated frequently and/or concurrently by many users.  
 *  
 */

class LuceneIndexWriterXAResource implements XAResource {
	private enum TransactionState {NONE, ACTIVE, SUSPENDED, IDLE, PREPARED, ROLLBACK_ONLY}
	
	private Logger logger;
	private IndexWriter writer;
	private LuceneTransactionListener listener;
	private TransactionState state;
	private Xid currentXid;
		
	LuceneIndexWriterXAResource(IndexWriter writer, LuceneTransactionListener listener) {
		super();
		this.writer = writer;
		this.listener = listener;
		this.state = TransactionState.NONE;
		this.logger = Logger.getLogger(LuceneIndexWriterXAResource.class.getName());
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		logger.finest("Committing Lucene index writer. One phase: " + onePhase);
		
		changeState(xid, EnumSet.of(onePhase ? TransactionState.IDLE :
				TransactionState.PREPARED), null);
		
		try {
			writer.commit();
			clearXid();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to commit Lucene index writer", e);
			rollback(xid);
			throw newXAException(XAException.XAER_RMERR, e);
		}
		listener.afterCommit();
	}

	@Override
	public void end(Xid xid, int flags) throws XAException {
		logger.finest("Ending Lucene index writer. Flags: " + flags);
		switch(flags) {
		case TMSUCCESS:
			changeState(xid, 
					EnumSet.of(TransactionState.SUSPENDED, TransactionState.ACTIVE), 
					TransactionState.IDLE);
			break;
		case TMSUSPEND:
			changeState(xid, 
					EnumSet.of(TransactionState.ACTIVE), 
					TransactionState.SUSPENDED);
			break;
		case TMFAIL:
			changeState(xid, EnumSet.of(TransactionState.ACTIVE), 
					TransactionState.ROLLBACK_ONLY);
			break;
		default:
			throw new XAException(XAException.XAER_INVAL);
		
		}
	}

	@Override
	public void forget(Xid xid) throws XAException {
		logger.finest("Forgetting Lucene index writer");
		clearXid();
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return 0;
	}

	@Override
	public boolean isSameRM(XAResource resource) throws XAException {
		if(resource instanceof LuceneIndexWriterXAResource) {
			LuceneIndexWriterXAResource other = (LuceneIndexWriterXAResource)resource;
			return writer == other.writer;
		}
		
		return false;
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		logger.finest("Preparing Lucene index writer");
		
		changeState(xid, EnumSet.of(TransactionState.IDLE), null);
		
		try {
			writer.prepareCommit();
			changeState(xid, EnumSet.of(TransactionState.IDLE), TransactionState.PREPARED);
			return XAResource.XA_OK;
		} catch (Exception e) {
			logger.log(Level.WARNING,"Failed to prepare Lucene index writer", e);
			throw newXAException(XAException.XAER_RMERR, e);
		}
	}

	@Override
	public synchronized Xid[] recover(int flags) throws XAException {
		logger.finest("Recovering Lucene index writer");
		return currentXid == null || state != TransactionState.PREPARED ? new Xid[0] : new Xid[] {currentXid};
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		logger.finest("Rolling back Lucene index writer");
		
		changeState(xid, EnumSet.of(TransactionState.IDLE,
				TransactionState.PREPARED, TransactionState.ROLLBACK_ONLY), null);
		
		try {
			Directory directory = writer.getDirectory();
			Analyzer analyzer = writer.getAnalyzer();
			MaxFieldLength maxfieldLength = new MaxFieldLength(writer.getMaxFieldLength()); 
						
			writer.rollback();
			
			// IndexWriter.rollback() automatically closes the writer so reopen it.
			logger.finest("Opening new Lucene index writer after rollback");
			writer = new IndexWriter(directory, analyzer, maxfieldLength);
			listener.afterRollback(writer);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to rollback and reopen Lucene index writer", e);
			throw newXAException(XAException.XAER_RMERR, e);
		} finally {
			clearXid();
		}
	}

	@Override
	public boolean setTransactionTimeout(int timeout) throws XAException {
		if(timeout < 0) {
			throw new XAException(XAException.XAER_INVAL);
		}
		return false;
	}

	@Override
	public void start(Xid xid, int flags) throws XAException {
		logger.finest("Starting Lucene index writer. Flags: " + flags);

		switch(flags) {
			case TMJOIN:
				changeState(xid, EnumSet.of(TransactionState.IDLE), TransactionState.ACTIVE);
				break;
			case TMRESUME:
				changeState(xid, EnumSet.of(TransactionState.SUSPENDED), TransactionState.ACTIVE);
				break;
			case TMNOFLAGS:
				setXid(xid);
				break;
			default:
				throw new XAException(XAException.XAER_INVAL); 
		} 
	}
	
	private synchronized void setXid(Xid xid) throws XAException {
		// start method should have been called with TMJOIN
		// because isSameRM would have returned true
		if(currentXid != null && currentXid.equals(xid)) {
			throw new XAException(XAException.XAER_DUPID);
		}
		
		while(state != TransactionState.NONE && currentXid != null) {
			logger.finest("Blocking thread with transaction (id=" 
					+ xid 
					+ " ) because current transaction (id="
					+ currentXid
					+ ") is still in progress");
			try {
				wait();
			} catch (InterruptedException e) {
				if(Thread.interrupted()) { // clears interrupted status
					logger.log(Level.WARNING, "Thread waiting for transaction (id="
							+ currentXid
							+ ") to complete has been interrupted?", e);
					throw new ThreadInterruptedException(e);
				}
			}
		}
				
		currentXid = xid;
		state = TransactionState.ACTIVE;
	}
	
	private synchronized void changeState(Xid xid, EnumSet<TransactionState> from, TransactionState to) throws XAException {
		if(currentXid == null) {
			logger.log(Level.WARNING, "No transaction currently associated");
			throw new XAException(XAException.XAER_NOTA);
		}
		
		if(!currentXid.equals(xid)) {
			logger.log(Level.WARNING, "Transaction (id="
					+ xid + " does not match current transaction (id="
					+ currentXid + ")");
			throw new XAException(XAException.XAER_NOTA);
		}
						
		if(!from.contains(state)) {
			logger.log(Level.WARNING, "Transaction (id="
					+ currentXid + ") in illegal state " + state);
			throw new XAException(XAException.XAER_PROTO);
		}
		
		if(to != null) {
			this.state = to;
		}
	}

	private synchronized void clearXid() {
		currentXid = null;
		state = TransactionState.NONE;
		notify();
	}
	
	private XAException newXAException(int error, Exception cause) {
		XAException e = new XAException(error);
		e.initCause(cause);
		return e;
	}
	
}
