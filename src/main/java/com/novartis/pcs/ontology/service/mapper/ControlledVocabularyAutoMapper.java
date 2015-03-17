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
package com.novartis.pcs.ontology.service.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import com.novartis.pcs.ontology.dao.ControlledVocabularyTermDAOLocal;
import com.novartis.pcs.ontology.entity.ControlledVocabulary;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Synonym.Type;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.Version;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.service.OntologyService;
import com.novartis.pcs.ontology.service.notify.EmailSenderLocal;
import com.novartis.pcs.ontology.service.search.OntologySearchServiceLocal;
import com.novartis.pcs.ontology.service.util.ControlledVocabularyTermSourceComparator;
import com.novartis.pcs.ontology.service.util.StatusChecker;

/**
 * Message-Driven Bean implementation class for: ControlledVocabularyMapper
 *
 */
@Stateless
@Local(ControlledVocabularyAutoMapperLocal.class)
public class ControlledVocabularyAutoMapper 
		extends OntologyService 
		implements ControlledVocabularyAutoMapperLocal {
	private static Logger logger = Logger.getLogger(
			ControlledVocabularyAutoMapper.class.getName());
		
	@EJB
	private ControlledVocabularyTermDAOLocal ctrlVocabTermDAO;
	
	@EJB
	private OntologySearchServiceLocal searchService;
		
	@EJB
	private EmailSenderLocal emailSender;
	
	@Resource(lookup="java:global/ontobrowser/curator/system")
	private String systemCurator;
		
    /**
     * Default constructor. 
     */
    public ControlledVocabularyAutoMapper() {
        
    }

	/**
     * @see Job#execute(JobExecutionContext)
     */
    @Override
    public void execute(boolean sendNotification) {
    	logger.info("Starting auto mapping of controlled vocabulary terms. System username: " + systemCurator);
    	try {
    		List<ControlledVocabularyTerm> vocabTerms = ctrlVocabTermDAO.loadUnmapped();
    		if(!vocabTerms.isEmpty()) {
    			List<Term> terms = termDAO.loadByStatus(EnumSet.of(Status.PENDING,Status.APPROVED));
	    		Curator curator = curatorDAO.loadByUsername(systemCurator);
				Version version = lastUnpublishedVersion(curator);
				List<ControlledVocabularyTerm> unmapped = new ArrayList<ControlledVocabularyTerm>(vocabTerms.size());
	    		List<List<String>> stemmed = new ArrayList<List<String>>();
	    		PorterStemmer stemmer = new PorterStemmer();
	    		    			    		
	    		for(Term term : terms) {
	    			stemmed.add(stem(stemmer, term.getName()));
	    		}
	    		
	    		for(ControlledVocabularyTerm vocabTerm : vocabTerms) {
	    			if(!vocabTerm.isExcluded()) {
		    			List<String> vocabTermStemmed = stem(stemmer, vocabTerm.getName());
		    			Set<Ontology> ontologies = vocabTerm
		    					.getControlledVocabulary()
		    					.getDomain()
		    					.getOntologies(); 
		    			Term matchedTerm = null;
		    			Type synonymType = Type.EXACT;
		    			Status status = null;
		    			for(int i = 0; i < stemmed.size(); i++) {
		    				List<String> termStemmed = stemmed.get(i);
		    				Term term = terms.get(i);
		    				if(ontologies.contains(term.getOntology())
		    						&& termStemmed.size() == vocabTermStemmed.size() 
		    						&& termStemmed.containsAll(vocabTermStemmed)) {
		    					if(matchedTerm == null) {
		    						matchedTerm = term; 
		    					} else {
		    						StringBuilder msg = new StringBuilder("Multiple stemmed ontology terms matched");
		    						msg.append(" stemmed controlled vocabulary term ");
		    						append(msg, vocabTermStemmed);
		    						logger.info(msg.toString());
		    						matchedTerm = null;
		    						break;
		    					}	
	    					}
		    			}
		    			
		    			// Exact matching synonyms
		    			if(matchedTerm == null) {
		    				String vocabTermName = vocabTerm.getName().trim();
		    				termloop:
		    				for(int i = 0; i < terms.size(); i++) {
			    				Term term = terms.get(i);
			    				if(ontologies.contains(term.getOntology())) {
			    					for(Synonym termSynonym : term.getSynonyms()) {
			    						if(StatusChecker.isValid(termSynonym) 
			    								&& vocabTermName.equalsIgnoreCase(termSynonym.getSynonym().trim())) {
			    							matchedTerm = term;
			    							synonymType = termSynonym.getType();
				    						status = termSynonym.getStatus();
			    							break termloop;
			    						}
			    					}
		    					}
			    			}
		    			}
		    			
		    			// Matching synonyms after ignoring non-word characters
		    			if(matchedTerm == null) {
		    				String vocabTermName = vocabTerm.getName().replaceAll("\\W+", " ").trim();
		    				termloop:
		    				for(int i = 0; i < terms.size(); i++) {
			    				Term term = terms.get(i);
			    				if(ontologies.contains(term.getOntology())) {
			    					for(Synonym termSynonym : term.getSynonyms()) {
			    						if(StatusChecker.isValid(termSynonym)) {
			    							String synonym = termSynonym.getSynonym().replaceAll("\\W+", " ").trim();
			    							if(vocabTermName.equalsIgnoreCase(synonym)) {
				    							matchedTerm = term;
				    							synonymType = termSynonym.getType();
					    						status = Status.PENDING;
				    							break termloop;
			    							}
			    						}
			    					}
		    					}
			    			}
		    			}
		    			
		    			if(matchedTerm != null) {
		    				// Check if synonym has been previously rejected
		    				boolean previouslyRejected = false;
		    				for(Synonym synonym : matchedTerm.getSynonyms()) {
	    						if(vocabTerm.equals(synonym.getControlledVocabularyTerm())) {
	    							previouslyRejected = true;
	    							break;
	    						}
	    					}
		    				
		    				if(!previouslyRejected) {
			    				StringBuilder msg = new StringBuilder("Auto-mapping controlled vocab term \"")
			    					.append(vocabTerm.getName())
			    					.append("\" from ")
			    					.append(vocabTerm.getControlledVocabulary().getDatasource().getAcronym())
									.append(" controlled vocabulary ")
									.append(vocabTerm.getControlledVocabulary())
									.append(" to term \"")
									.append(matchedTerm.getName())
									.append("\" from ontology ")
									.append(matchedTerm.getOntology().getName());
														
								logger.info(msg.toString());
								
								Synonym synonym = new Synonym(matchedTerm, vocabTerm.getName(), 
										synonymType, curator, version);
		    					synonym.setControlledVocabularyTerm(vocabTerm);
		    					
		    					if(status == null) {
		    						status = matchedTerm.getStatus();
		    					}
		    					
		    					synonym.setStatus(status);
								
		    					if(status.equals(Status.APPROVED)) {
		    						synonym.setApprovedVersion(version);
		    					}
		    						    					
								searchService.update(matchedTerm);
		    				}
		    			} else {
		    				unmapped.add(vocabTerm);
		    			}
	    			}
	    		}
	    		
	    		if(sendNotification && !unmapped.isEmpty()) {
	    			sendUnmappedEmail(unmapped);
	    		}
    		}
    		logger.info("Completed auto mapping of controlled vocabulary terms");
    	} catch(Exception e) {
    		String msg = "Failed to auto map controlled vocabulary terms";
    		logger.log(Level.SEVERE, msg, e);
    		throw new RuntimeException(msg, e);
    	}
    }
    
    private void sendUnmappedEmail(List<ControlledVocabularyTerm> unmapped) {
    	try {
	    	StringBuilder msg = new StringBuilder();
	    	
	    	msg.append("This email alert lists the system vocabulary terms\n");
	    	msg.append("that have not yet been mapped to an ontology term.\n\n");
	    	
	    	Collections.sort(unmapped, new ControlledVocabularyTermSourceComparator());
	    	    	
	    	ControlledVocabulary last = null;
	    	for(ControlledVocabularyTerm vocabTerm : unmapped) {
	    		ControlledVocabulary controlledVocabulary = vocabTerm.getControlledVocabulary();
	    		if(last == null || !last.equals(controlledVocabulary)) {
	    			if(last != null) {
	    				msg.append("\n");
	    			}
	    			
	    			msg.append(controlledVocabulary.getDatasource().getAcronym())
	    				.append(" ").append(controlledVocabulary.getName())
	    				.append(" vocabulary:\n");
	    			last = controlledVocabulary;
	    		}
	    		    		
	    		msg.append("\t").append(vocabTerm.getName()).append("\n");
	    	}
	    	emailSender.send("Unmapped system vocabulary terms", msg.toString());
    	} catch(Exception e) {
    		// don't rollback entire transaction just because the email notification fails
    		String msg = "Failed to send email notification for unmmap controlled vocabulary terms";
    		logger.log(Level.WARNING, msg, e);
    	}
    }
    
    private List<String> stem(PorterStemmer stemmer, String s) {
    	List<String> stemmed = new ArrayList<String>();
		boolean reset = true;
		for(int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if(Character.isLetter(c)) {
				stemmer.add(Character.toLowerCase(c));
				reset = false;
			} else if(!reset) {
				stemmer.stem();
				stemmed.add(stemmer.toString());
				stemmer.reset();
				reset = true;
			}
		}
		if(!reset) {
			stemmer.stem();
			stemmed.add(stemmer.toString());
			stemmer.reset();
			reset = true;
		}
		return stemmed;
    }
    
    private void append(StringBuilder msg, List<String> list) {
    	boolean first = true;
    	msg.append("[");
    	for(String s : list) {
    		if(!first) {
    			msg.append(",");
    		}
    		msg.append(s);
    		first = false;
    	}
    	msg.append("]");
    }

}
