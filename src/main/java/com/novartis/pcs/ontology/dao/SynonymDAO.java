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

import java.util.Collection;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.Query;

import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Synonym;

/**
 * Stateless session bean DAO for Synonym entity
 */
@Stateless
@Local(SynonymDAOLocal.class)
@Remote(SynonymDAORemote.class)
public class SynonymDAO extends VersionedEntityDAO<Synonym> 
	implements SynonymDAOLocal, SynonymDAORemote {
       
    public SynonymDAO() {
        super();
    }

	@Override
	@SuppressWarnings("unchecked")
	public Collection<Synonym> loadByTermRefId(String termRefId) {
		Query query = entityManager.createNamedQuery(Synonym.QUERY_BY_TERM_REF_ID);
		query.setParameter("termRefId", termRefId.toUpperCase());
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Collection<Synonym> loadByCtrldVocabTermId(ControlledVocabularyTerm ctrldVocabTerm) {
		Query query = entityManager.createNamedQuery(Synonym.QUERY_BY_CTRLD_VOCAB_TERM);
		query.setParameter("ctrldVocabTerm", ctrldVocabTerm);
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Collection<Synonym> loadBySynonym(String synonym) {
		Query query = entityManager.createNamedQuery(Synonym.QUERY_BY_SYNONYM);
		query.setParameter("synonym", synonym.trim().toLowerCase());
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Collection<Synonym> loadByDatasource(Datasource datasource) {
		Query query = entityManager.createNamedQuery(Synonym.QUERY_BY_DATASOURCE);
		query.setParameter("datasource", datasource);
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Collection<Synonym> loadByCtrldVocabRefId(Datasource datasource, String referenceId) {
		Query query = entityManager.createNamedQuery(Synonym.QUERY_BY_CTRLD_VOCAB_REF_ID);
		query.setParameter("datasource", datasource);
		query.setParameter("referenceId", referenceId);
		return query.getResultList();
	}
}
