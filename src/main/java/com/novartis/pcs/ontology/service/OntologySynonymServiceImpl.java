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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import com.novartis.pcs.ontology.dao.ControlledVocabularyDAOLocal;
import com.novartis.pcs.ontology.dao.ControlledVocabularyTermDAOLocal;
import com.novartis.pcs.ontology.entity.ControlledVocabulary;
import com.novartis.pcs.ontology.entity.ControlledVocabularyContext;
import com.novartis.pcs.ontology.entity.ControlledVocabularyDomain;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTermLink;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.service.util.ControlledVocabularyTermUsageComparator;
import com.novartis.pcs.ontology.service.util.StatusChecker;

/**
 * Session Bean implementation class OntologySynonymService
 */
@Stateless
@Local(OntologySynonymServiceLocal.class)
@Remote(OntologySynonymServiceRemote.class)
public class OntologySynonymServiceImpl extends OntologyService implements OntologySynonymServiceRemote, OntologySynonymServiceLocal {

	@EJB
	private ControlledVocabularyDAOLocal vocabDAO;
	
	
	@EJB
	private ControlledVocabularyTermDAOLocal vocabTermDAO;
			
    /**
     * Default constructor. 
     */
    public OntologySynonymServiceImpl() {
    }
        
	@Override
	public Collection<Synonym> loadSynonyms(String termRefId) {
		Collection<Synonym> synonyms = synonymDAO.loadByTermRefId(termRefId);
		List<Synonym> validSynonyms = new ArrayList<Synonym>(synonyms.size());
		for(Synonym synonym : synonyms) {
			if(StatusChecker.isValid(synonym)) {
				validSynonyms.add(synonym);
			}
		}
		Collections.sort(validSynonyms);
		return validSynonyms;
	}
	
	@Override
	public Collection<Datasource> loadPublicDatasources() {
		Collection<Datasource> datasources = datasourceDAO.loadAll();
    	Collection<Datasource> publiclyAccessible = new ArrayList<Datasource>();
    	for(Datasource datasource : datasources) {
    		if(!datasource.isInternal() && datasource.isPubliclyAccessible()) {
    			publiclyAccessible.add(datasource);
    		}
    	}
	    return publiclyAccessible;
	}

	@Override
	public Collection<ControlledVocabulary> loadControlledVocabulariesWithUnmappedTerms() {
		return vocabDAO.loadByUnmappedTerms();
	}

	@Override
	public Collection<ControlledVocabularyTerm> loadUnmappedControlledVocabularyTerms(
			ControlledVocabularyDomain domain,
			ControlledVocabularyContext context, Datasource datasource) {
		return sort(vocabTermDAO.loadUnmapped(domain, context, datasource));
	}
	
	@Override
	public Collection<ControlledVocabularyTerm> loadUnmappedControlledVocabularyTerms(
			ControlledVocabularyDomain domain, Datasource datasource) {
		return sort(vocabTermDAO.loadUnmapped(domain, datasource));
	}

	@Override
	public Collection<ControlledVocabularyTerm> loadUnmappedControlledVocabularyTerms(
			ControlledVocabularyDomain domain,
			ControlledVocabularyContext context) {
		return sort(vocabTermDAO.loadUnmapped(domain, context));
	}

	@Override
	public Collection<ControlledVocabularyTerm> loadUnmappedControlledVocabularyTerms(
			ControlledVocabularyDomain domain) {
		return sort(vocabTermDAO.loadUnmapped(domain));
	}
	
	@Override
	public void excludeUnmappedControlledVocabularyTerms(Collection<ControlledVocabularyTerm> terms,
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		for(ControlledVocabularyTerm term : terms) {
			term = vocabTermDAO.load(term.getId());
			if(term != null && !term.isExcluded()) {
				Collection<Synonym> synonyms = synonymDAO.loadByCtrldVocabTermId(term);
				for(Synonym synonym : synonyms) {
					if(StatusChecker.isValid(synonym)) {
						throw new InvalidEntityException(term, "Cannot exclude previously mapped vocabulary term");
					}
				}
				term.setExcluded(true);
				term.setModifiedBy(curator);
			}
		}
	}
	
	@Override
	public Collection<ControlledVocabularyTermLink> loadControlledVocabularyTermLinks(
			ControlledVocabularyTerm term) {
		return vocabTermDAO.load(term.getId(), true).getLinks();
	}

	private Collection<ControlledVocabularyTerm> sort(Collection<ControlledVocabularyTerm> unmmapped) {
		List<ControlledVocabularyTerm> list = new ArrayList<ControlledVocabularyTerm>();
		
		for(ControlledVocabularyTerm term : unmmapped) {
			if(!term.isExcluded()) {
				list.add(term);
			}
		}
				
		Collections.sort(list, 
				new ControlledVocabularyTermUsageComparator());
		
		return list;
	}
	
}
