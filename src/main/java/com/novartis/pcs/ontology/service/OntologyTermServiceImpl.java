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
package com.novartis.pcs.ontology.service;


import java.util.Collection;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.novartis.pcs.ontology.dao.ControlledVocabularyTermDAOLocal;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.DuplicateEntityException;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.Version;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.service.search.OntologySearchServiceListener;
import com.novartis.pcs.ontology.service.search.OntologySearchServiceLocal;
import com.novartis.pcs.ontology.service.util.StatusChecker;


/**
 * Session Bean implementation class OntologyTermService
 */
@Stateless
@Local({OntologyTermServiceLocal.class})
@Remote({OntologyTermServiceRemote.class})
public class OntologyTermServiceImpl extends OntologyService implements OntologyTermServiceRemote, OntologyTermServiceLocal {	
	@EJB
	private OntologySearchServiceLocal serachService;
	
	@EJB
	private ControlledVocabularyTermDAOLocal vocabTermDAO;
	
    /**
     * Default constructor. 
     */
    public OntologyTermServiceImpl() {
        
    }
    
    @Override
	public Collection<Term> loadAll(String ontologyName) {
		Ontology ontology = ontologyDAO.loadByName(ontologyName);
		Collection<Term> terms = termDAO.loadAll(ontology);
		StatusChecker.removeInvalid(terms);
    	return terms;
	}

	@Override
    public Collection<Term> loadRoots() {
    	Collection<Term> terms = termDAO.loadRoots();
    	StatusChecker.removeInvalid(terms);
    	return terms;
    }
        
    @Override
	public Collection<Term> loadLastCreated(int max) {
    	Collection<Term> terms = termDAO.loadLastCreated(max);
    	StatusChecker.removeInvalid(terms);
    	return terms;
	}

	@Override
    public Term loadByReferenceId(String referenceId) {
    	return termDAO.loadByReferenceId(referenceId.trim(), true);
    }
    
    @Override
    public Collection<RelationshipType> loadAllRelationshipTypes() {
    	Collection<RelationshipType> types = relationshipTypeDAO.loadAll();
    	StatusChecker.removeInvalid(types);
    	return types;
    }
	
	@Override
	@SuppressWarnings({ "unchecked", "unused" })
	@Interceptors({OntologySearchServiceListener.class})
	public Term addRelationship(String termRefId,
			String relatedTermRefId, String relationshipType,
			String curatorUsername) throws DuplicateEntityException, InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Version version = lastUnpublishedVersion(curator);
		RelationshipType type = relationshipTypeDAO.loadByRelationship(relationshipType);
		Term term = termDAO.loadByReferenceId(termRefId, true);
		Term relatedTerm = termDAO.loadByReferenceId(relatedTermRefId);
				
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		StatusChecker.validate(type, term, term.getOntology(), relatedTerm);
		
		for(Relationship duplicate : term.getRelationships()) {
			if(duplicate.getRelatedTerm().equals(relatedTerm)
					&& duplicate.getType().equals(type)) {
				throw new DuplicateEntityException(duplicate, "Relationship already exists");
			}
		}
		
		Relationship relationship = new Relationship(term, relatedTerm, type, curator, version);
		
		return term;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Interceptors({OntologySearchServiceListener.class})
	public Term addSynonym(String termRefId, 
			ControlledVocabularyTerm vocabTerm,
			Synonym.Type type,
			String curatorUsername) throws DuplicateEntityException, InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Version version = lastUnpublishedVersion(curator);
		Term term = termDAO.loadByReferenceId(termRefId, true);
		vocabTerm = vocabTermDAO.load(vocabTerm.getId());
						
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		StatusChecker.validate(term, term.getOntology());
		
		Set<Ontology> ontologies = vocabTerm.getControlledVocabulary().getDomain().getOntologies();
		if(!ontologies.contains(term.getOntology())) {
			throw new InvalidEntityException(term, 
					"Cannot map controlled vocabulary term from " 
					+ vocabTerm.getControlledVocabulary().getDomain().getName()
					+ " domain to " 
					+ term.getOntology().getName()
					+ " ontology/codelist");
		}
		
		Collection<Synonym> duplicates = synonymDAO.loadByCtrldVocabTermId(vocabTerm);
		for(Synonym duplicate : duplicates) {
			if(StatusChecker.isValid(duplicate)) {
				throw new DuplicateEntityException(duplicate, 
							"Synonym already exists for controlled vocabulary term: " + vocabTerm.getName());
			}
		}
		
		duplicates = synonymDAO.loadBySynonym(vocabTerm.getName());
		for(Synonym duplicate : duplicates) {
			if(StatusChecker.isValid(duplicate) 
					&& ontologies.contains(duplicate.getTerm().getOntology())
					&& !duplicate.getTerm().equals(term)) {
				throw new DuplicateEntityException(duplicate, 
							"Similar synonym has been mapped to a different "
						        + duplicate.getTerm().getOntology().getName()
						        + " ontology term: "
								+ duplicate.getTerm().getName());
			}
		}
		
		Synonym newSynonym = new Synonym(term, vocabTerm.getName(), 
				type, curator, version);
		newSynonym.setControlledVocabularyTerm(vocabTerm);
		
		return term;
	}

	@Override
	@Interceptors({OntologySearchServiceListener.class})
	@SuppressWarnings("unchecked")
	public Term addSynonym(String termRefId, String synonym, Synonym.Type type,
			String datasourceAcronym, String referenceId, String curatorUsername)
			throws DuplicateEntityException, InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Version version = lastUnpublishedVersion(curator);
		Term term = termDAO.loadByReferenceId(termRefId, true);
		Collection<Synonym> duplicates = synonymDAO.loadBySynonym(synonym);
		Datasource datasource = null;
				
		if(datasourceAcronym != null) {
			datasource = datasourceDAO.loadByAcronym(datasourceAcronym);
			if(datasource == null) {
				throw new InvalidEntityException(datasource, "Datasource does not exist: " + datasourceAcronym);
			}
		}
		
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		StatusChecker.validate(term, term.getOntology());
		
		for(Synonym duplicate : duplicates) {
			if(StatusChecker.isValid(duplicate)
					&& duplicate.getControlledVocabularyTerm() == null
					&& ObjectUtils.equals(duplicate.getDatasource(), datasource)) {
				throw new DuplicateEntityException(duplicate, 
						"Synonym already exists for: " + synonym);
			}
		}
		
		Synonym newSynonym = new Synonym(term, synonym, type, curator, version);
		if(datasource != null) {
			newSynonym.setDatasource(datasource);
			newSynonym.setReferenceId(referenceId);
		}
		    	
		return term;
	}
	
	@Override
	@Interceptors({OntologySearchServiceListener.class})
	public Term addSynonyms(String termRefId, 
			Collection<ControlledVocabularyTerm> terms,
			Synonym.Type synonymType,
			String curatorUsername) throws DuplicateEntityException, InvalidEntityException {
		Term term = null;
		for(ControlledVocabularyTerm vocabTerm : terms) {
			term = addSynonym(termRefId, 
					vocabTerm, synonymType, curatorUsername);
		}
		return term;
	}
	
	@Override
	@Interceptors({OntologySearchServiceListener.class})
	public Term createTerm(String ontologyName, String termName,
			String definition, String url, String comments,
			String relatedTermRefId, String relationshipType,
			String curatorUsername) throws DuplicateEntityException, InvalidEntityException {
		return createTerm(ontologyName, termName, 
				definition, url, comments, 
				relatedTermRefId, relationshipType,
				null, null,
				null, null, curatorUsername);
	}

	@Override
	@SuppressWarnings({ "unchecked", "unused" })
	@Interceptors({OntologySearchServiceListener.class})
	public Term createTerm(String ontologyName, String termName,
			String definition, String url, String comments,
			String relatedTermRefId, String relationshipType,
			String datasourceAcronym, String sourceReferenceId,
			Collection<ControlledVocabularyTerm> synonyms,
			Synonym.Type synonymType,
			String curatorUsername) throws DuplicateEntityException, InvalidEntityException {
		// Lock ontology because of update of term reference id value
		Ontology ontology = ontologyDAO.loadByName(ontologyName, true);
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		
		StatusChecker.validate(ontology);
		
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		Term duplicate = termDAO.loadByName(termName, ontology);
		if(duplicate != null) {
			throw new DuplicateEntityException(duplicate, "Term already exists");
		}
		
		Version version = lastUnpublishedVersion(curator);
		String referenceId = nextReferenceId(ontology);
		
		Term newTerm = new Term(ontology, termName, referenceId, curator, version);
		
		if(definition != null && definition.trim().length() > 0) {
			newTerm.setDefinition(definition.trim());
		}
		
		if(url != null && url.trim().length() > 0) {
			newTerm.setUrl(url.trim());
		}
		
		if(comments != null && comments.trim().length() > 0) {
			newTerm.setComments(comments.trim());
		}
		
		if(relatedTermRefId != null) {
			Term relatedTerm = termDAO.loadByReferenceId(relatedTermRefId);
			RelationshipType type = relationshipTypeDAO.loadByRelationship(relationshipType);
			
			StatusChecker.validate(type, relatedTerm);
						
			Relationship relationship = new Relationship(newTerm, relatedTerm, type, curator, version);
		}
		
		if(datasourceAcronym != null) {
			Datasource datasource = datasourceDAO.loadByAcronym(datasourceAcronym);
			if(datasource == null) {
				throw new InvalidEntityException(datasource, "Datasource does not exist: " + datasourceAcronym);
			}
			CrossReference xref = new CrossReference(newTerm, datasource, sourceReferenceId, curator);
		}
		
		if(synonyms != null) {
			for(ControlledVocabularyTerm vocabTerm : synonyms) {
				Collection<Synonym> existingSynonyms = synonymDAO.loadByCtrldVocabTermId(vocabTerm);				
				for(Synonym existingSynonym : existingSynonyms) {
					if(StatusChecker.isValid(existingSynonym)) {
						throw new DuplicateEntityException(existingSynonym, 
								"Synonym already exists for controlled vocabulary term: " + vocabTerm);
					}
				}
				
				Collection<Synonym> duplicateSynonyms = synonymDAO.loadBySynonym(vocabTerm.getName());
				for(Synonym duplicateSynonym : duplicateSynonyms) {
					if(StatusChecker.isValid(duplicateSynonym)) {
						throw new DuplicateEntityException(duplicate, 
									"Similar synonym has been mapped to a different ontology term: "
										+ duplicateSynonym.getTerm().getName());
					}
				}
				
				Synonym synonym = new Synonym(newTerm, vocabTerm.getName(), 
						synonymType, curator, version);
				synonym.setControlledVocabularyTerm(vocabTerm);
			}
		}
				
		termDAO.save(newTerm);
		return newTerm; 
	}
	
	@Override
	public Term updateTerm(long termId, String definition, String url,
			String comments, String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Term term = termDAO.load(termId, true);
				
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		StatusChecker.validate(term);
		
		if(definition != null && definition.trim().length() > 0) {
			term.setDefinition(definition.trim());
		} else {
			term.setDefinition(null);
		}
		
		if(url != null && url.trim().length() > 0) {
			term.setUrl(url.trim());
		} else {
			term.setUrl(null);
		}
		
		if(comments != null && comments.trim().length() > 0) {
			term.setComments(comments.trim());
		} else {
			term.setComments(null);
		}	
		
		return term;
	}

	@Override
	public Synonym updateSynonym(long synonymId, Synonym.Type type, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Synonym synonym = synonymDAO.load(synonymId);
		
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		StatusChecker.validate(synonym);
		
		synonym.setType(type);
		
		return synonym;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Relationship updateRelationship(long relationshipId,
			String relationshipType, String curatorUsername) 
			throws InvalidEntityException, DuplicateEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Relationship relationship = relationshipDAO.load(relationshipId);
		RelationshipType type = relationshipTypeDAO.loadByRelationship(relationshipType);
		
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
			
		StatusChecker.validate(type, relationship);
		
		Term term = relationship.getTerm();
		Term relatedTerm = relationship.getRelatedTerm();
		for(Relationship duplicate : term.getRelationships()) {
			if(duplicate.getRelatedTerm().equals(relatedTerm)
					&& duplicate.getType().equals(type)) {
				throw new DuplicateEntityException(duplicate, "Relationship already exists");
			}
		}
		
		relationship.setType(type);
		
		return relationship;
	}
	
	@Override
	public void deleteTerm(long termId, String curatorUsername) 
			throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Term term = termDAO.load(termId, true);
		if(term != null) {		
			if(curator == null || !curator.isActive()) {
				throw new InvalidEntityException(curator, "Curator is invalid/inactive");
			}
			
			if(!term.getStatus().equals(Status.PENDING)) {
				throw new InvalidEntityException(term, "Cannot delete non-pending term");
			}
			
			if(!term.getCreatedBy().equals(curator)) {
				throw new InvalidEntityException(term, "Cannot delete term created by another curator");
			}
					
			Collection<Relationship> descendents = relationshipDAO.loadByRelatedTermId(termId);
			for (Relationship relationship : descendents) {
				Term childTerm = relationship.getTerm(); 
				childTerm.getRelationships().remove(relationship);
				relationshipDAO.delete(relationship);
			}
								
			termDAO.delete(term);
			serachService.delete(term);
		}
	}

	@Override
	public void deleteSynonym(long synonymId, String curatorUsername)
			throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Synonym synonym = synonymDAO.load(synonymId);
		if(synonym != null) {
			Term term = synonym.getTerm(); 
			
			if(curator == null || !curator.isActive()) {
				throw new InvalidEntityException(curator, "Curator is invalid/inactive");
			}
			
			if(!synonym.getStatus().equals(Status.PENDING)) {
				throw new InvalidEntityException(synonym, "Cannot delete non-pending synonym");
			}
			
			if(!synonym.getCreatedBy().equals(curator)) {
				throw new InvalidEntityException(synonym, "Cannot delete synonym created by another curator");
			}
			
			term.getSynonyms().remove(synonym);
			synonymDAO.delete(synonym);
			serachService.update(term);
		}
	}

	@Override
	public void deleteRelationship(long relationshipId, String curatorUsername) 
			throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Relationship relationship = relationshipDAO.load(relationshipId);
		if(relationship != null) {
			Term term = relationship.getTerm();
			
			if(curator == null || !curator.isActive()) {
				throw new InvalidEntityException(curator, "Curator is invalid/inactive");
			}
				
			if(!relationship.getStatus().equals(Status.PENDING)) {
				throw new InvalidEntityException(relationship, "Cannot delete non-pending relationship");
			}
			
			if(!relationship.getCreatedBy().equals(curator)) {
				throw new InvalidEntityException(relationship, "Cannot delete relationship created by another curator");
			}
			
			term.getRelationships().remove(relationship);
			relationshipDAO.delete(relationship);
		}
	}
	
	// Ontology must be locked before calling this
	// i.e. ontologyDao.loadByName(name, true);
	private String nextReferenceId(Ontology ontology) {
		StringBuilder referenceId = new StringBuilder();
		String prefix = ontology.getReferenceIdPrefix();
		if(prefix != null && prefix.length() > 0) {
			referenceId.append(prefix).append(':');
		}
		
		int value = ontology.getReferenceIdValue() + 1;
		ontology.setReferenceIdValue(value);
				
		referenceId.append(StringUtils.leftPad(
				Integer.toString(value), 7, "0"));
		
		return referenceId.toString();
	}
}
