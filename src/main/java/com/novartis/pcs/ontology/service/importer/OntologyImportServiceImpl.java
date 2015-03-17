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
package com.novartis.pcs.ontology.service.importer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.coode.owlapi.obo.parser.OBOParser;
import org.coode.owlapi.obo.parser.ParseException;

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
import com.novartis.pcs.ontology.service.OntologyService;
import com.novartis.pcs.ontology.service.parser.InvalidFormatException;
import com.novartis.pcs.ontology.service.parser.obo.OBOParseContext;
import com.novartis.pcs.ontology.service.search.OntologySearchServiceLocal;

/**
 * Session Bean implementation class OntologyImportServiceImpl
 */
@Stateless
@Local(OntologyImportServiceLocal.class)
@Remote(OntologyImportServiceRemote.class)
public class OntologyImportServiceImpl extends OntologyService implements OntologyImportServiceRemote, OntologyImportServiceLocal {	
	private Logger logger = Logger.getLogger(getClass().getName());
	
	@EJB
	private OntologySearchServiceLocal searchService;
	
    /**
     * Default constructor. 
     */
    public OntologyImportServiceImpl() {
    }
    
    @Override
	public void importOntology(String ontologyName, InputStream is, Curator curator) 
    		throws DuplicateEntityException, InvalidEntityException {
    	// Lock ontology while we are importing/updating
    	// also need to lock ontology because we potentially
    	// update the current term reference id value
    	Ontology ontology = ontologyDAO.loadByName(ontologyName, true);
    	Collection<Term> terms = Collections.emptyList();
    	Collection<RelationshipType> relationshipTypes = relationshipTypeDAO.loadAll();
    	Collection<Datasource> datasources = datasourceDAO.loadAll();
    	Version version = lastUnpublishedVersion(curator);
    	
    	if(ontology == null) {
    		ontology = new Ontology(ontologyName, curator, version);
    		ontology.setStatus(Status.APPROVED);
    		ontology.setApprovedVersion(version);
    	} else {
    		terms = termDAO.loadAll(ontology);
    	}
    	    	
    	try {
    		// According to spec OBO files are UTF-8 encoded
			Reader reader = new InputStreamReader(is, "UTF-8");
			OBOParser parser = new OBOParser(reader);
			OBOParseContext context = new OBOParseContext(ontology, terms, 
					relationshipTypes, datasources, curator, version);
			parser.setHandler(context);
			parser.parse();
			terms = context.getTerms();
			
			String refIdPrefix = null;
			int refIdValue = 0;
			Set<String> names = new HashSet<String>(terms.size());
			for(Term term : terms) {
				String refId = term.getReferenceId();
				int colon = refId.indexOf(':');
				
				if(colon == -1) {
					throw new InvalidEntityException(term, 
							"No reference id prefix defined for term: " + refId);
				}
				
				if(refIdPrefix == null) {
					refIdPrefix = refId.substring(0,colon);
				} else if(!refIdPrefix.equals(refId.substring(0,colon))) {
					throw new InvalidEntityException(term, 
							"Invalid term reference id prefix: " + refId);
				}
				
				try {
					int value = Integer.parseInt(refId.substring(colon+1));
					refIdValue = Math.max(refIdValue, value);
				} catch(Exception e) {
					throw new InvalidEntityException(term, 
							"Invalid term reference id: " + refId, e);
				}
				
				if(!names.add(term.getName().toLowerCase())) {
					throw new DuplicateEntityException(term, "Duplicate term: " + term.getName());
				}
				
				if(term.getStatus() == Status.APPROVED) {
					boolean found = false;
					for(Relationship relationship : term.getRelationships()) {
						if(relationship.getStatus() == Status.APPROVED) {
							found = true;
							break;
						}
					}
					
					if(!found) {
						term.setRoot(true);
					}
				}
			}
			
			datasources = context.getDatasources();
			for(Datasource datasource : datasources) {
				if(datasource.getId() == 0L) {
					datasourceDAO.save(datasource);
				}
			}
			
			relationshipTypes = context.getRelationshipTypes();
			for(RelationshipType relationshipType : relationshipTypes) {
				if(relationshipType.getId() == 0L) {
					relationshipTypeDAO.save(relationshipType);
				}
			}
			
			ontology.setReferenceIdPrefix(refIdPrefix);
			ontology.setReferenceIdValue(refIdValue);
			if(ontology.getId() == 0L) {
				ontologyDAO.save(ontology);
			}	
			
			for(Term term : terms) {
				if(term.getId() == 0L) {
					termDAO.save(term);
				} else if(term.getStatus() == Status.OBSOLETE) {
					removePendingDependents(term, version);
				}
				searchService.update(term);
			}
		} catch (UnsupportedEncodingException e) {
			// Never happen because UTF-8 is built-in to JVM
		} catch (ParseException e) {
			logger.warning("Parse exception occuring during OBO file import: " + e.getMessage());
			throw new InvalidFormatException(e.getMessage());
		}   	
    }
    
    @SuppressWarnings("incomplete-switch")
	private void removePendingDependents(Term term, Version version) 
    		throws InvalidEntityException {
    	Collection<Synonym> synonyms = new ArrayList<Synonym>(term.getSynonyms());
		for(Synonym synonym : synonyms) {
			switch(synonym.getStatus()) {
			case PENDING:
				term.getSynonyms().remove(synonym);
				synonymDAO.delete(synonym);
				break;
			case APPROVED:
				synonym.setStatus(Status.OBSOLETE);
				synonym.setObsoleteVersion(version);
				break;
			}
		}
		
		Collection<Relationship> relationships = new ArrayList<Relationship>(term.getRelationships());
		for(Relationship relationship : relationships) {
			switch(relationship.getStatus()) {
			case PENDING:
				term.getRelationships().remove(relationship);
				relationshipDAO.delete(relationship);
				break;
			case APPROVED:
				relationship.setStatus(Status.OBSOLETE);
				relationship.setObsoleteVersion(version);
				break;
			}
		}
		
		Collection<Relationship> descendents = relationshipDAO.loadByRelatedTermId(term.getId());
		for (Relationship relationship : descendents) {
			switch(relationship.getStatus()) {
			case PENDING:
				Term childTerm = relationship.getTerm();
				childTerm.getRelationships().remove(relationship);
				relationshipDAO.delete(relationship);
				break;
			case APPROVED:
				relationship.setStatus(Status.OBSOLETE);
				relationship.setObsoleteVersion(version);
				break;
			}
		}
    }
}
