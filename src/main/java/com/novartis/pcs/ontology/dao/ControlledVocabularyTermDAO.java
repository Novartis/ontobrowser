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
package com.novartis.pcs.ontology.dao;

import static com.novartis.pcs.ontology.entity.ControlledVocabularyTerm.QUERY_UNMAPPED;
import static com.novartis.pcs.ontology.entity.ControlledVocabularyTerm.QUERY_UNMAPPED_BY_CONTEXT;
import static com.novartis.pcs.ontology.entity.ControlledVocabularyTerm.QUERY_UNMAPPED_BY_CONTEXT_AND_SOURCE;
import static com.novartis.pcs.ontology.entity.ControlledVocabularyTerm.QUERY_UNMAPPED_BY_DOMAIN;
import static com.novartis.pcs.ontology.entity.ControlledVocabularyTerm.QUERY_UNMAPPED_BY_SOURCE;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.Query;

import com.novartis.pcs.ontology.entity.ControlledVocabularyContext;
import com.novartis.pcs.ontology.entity.ControlledVocabularyDomain;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Datasource;

/**
 * Stateless session bean DAO for ControlledVocabularyTerm entity
 */
@Stateless
@Local({ControlledVocabularyTermDAOLocal.class})
@Remote({ControlledVocabularyTermDAORemote.class})
public class ControlledVocabularyTermDAO extends ModifiableEntityDAO<ControlledVocabularyTerm> 
	implements ControlledVocabularyTermDAOLocal, ControlledVocabularyTermDAORemote {
       
    public ControlledVocabularyTermDAO() {
        super();
    }

	@Override
	@SuppressWarnings("unchecked")
	public List<ControlledVocabularyTerm> loadUnmapped(
			ControlledVocabularyDomain domain,
			ControlledVocabularyContext context, Datasource datasource) {
		Query query = entityManager.createNamedQuery(
				QUERY_UNMAPPED_BY_CONTEXT_AND_SOURCE);
		query.setParameter("domain", domain);
		query.setParameter("context", context);
		query.setParameter("datasource", datasource);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ControlledVocabularyTerm> loadUnmapped(
			ControlledVocabularyDomain domain,
			ControlledVocabularyContext context) {
		Query query = entityManager.createNamedQuery(
				QUERY_UNMAPPED_BY_CONTEXT);
		query.setParameter("domain", domain);
		query.setParameter("context", context);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ControlledVocabularyTerm> loadUnmapped(
			ControlledVocabularyDomain domain, Datasource datasource) {
		Query query = entityManager.createNamedQuery(
				QUERY_UNMAPPED_BY_SOURCE);
		query.setParameter("domain", domain);
		query.setParameter("datasource", datasource);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ControlledVocabularyTerm> loadUnmapped(
			ControlledVocabularyDomain domain) {
		Query query = entityManager.createNamedQuery(QUERY_UNMAPPED_BY_DOMAIN);
		query.setParameter("domain", domain);
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<ControlledVocabularyTerm> loadUnmapped() {
		Query query = entityManager.createNamedQuery(QUERY_UNMAPPED);
		return query.getResultList();
	}
	
	@Override
	protected ControlledVocabularyTerm loadLazyAssociations(ControlledVocabularyTerm term) {
		if(term != null) {
			term.getLinks().size();
		}
		return term;
	}
}
