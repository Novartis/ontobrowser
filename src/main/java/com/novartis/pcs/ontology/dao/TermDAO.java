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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;

/**
 * Stateless session bean DAO for Term entity
 */
@Stateless
@Local({TermDAOLocal.class})
@Remote({TermDAORemote.class})
public class TermDAO extends VersionedEntityDAO<Term> 
	implements TermDAOLocal, TermDAORemote {
	
	// Only required for non-Oracle implementation
	@EJB
	private RelationshipDAOLocal relationshipDAO;
	
    public TermDAO() {
        super();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Collection<Term> loadRoots() {
		Query query = entityManager.createNamedQuery(Term.QUERY_ROOTS);
		return query.getResultList();
	}
    
    @Override
    @SuppressWarnings("unchecked")
    public Collection<Term> loadAll(Ontology ontology) {
		Query query = entityManager.createNamedQuery(Term.QUERY_ALL);
		query.setParameter("ontology", ontology);
		return query.getResultList();
	}
    
    @Override
    @SuppressWarnings("unchecked")
    public Collection<Term> loadSubTermsByReferenceId(String referenceId, EnumSet<Status> status) {
    	if(isOracle()) {
	    	// Oracle database hierarchical query implementation. Comment out for non-Oracle databases.
	    	Query query = entityManager.createNamedQuery(Term.QUERY_SUBTERMS);
			// When using a native query with an Enum as a parameter
			// the Enum integer is used and not the name.
			Collection<String> statusNames = new ArrayList<String>();
			for(Status s : status) {
				statusNames.add(s.name());
			}
			// Setting cache hint causes exception due to hibernate bug (https://hibernate.atlassian.net/browse/HHH-9111)
	       	// query.setHint("org.hibernate.cacheable", Boolean.TRUE);
			query.setParameter("referenceId", referenceId.toUpperCase());				
			query.setParameter("status", statusNames);
			return query.getResultList();
    	} else {    	
	    	// non-Oracle implementation which loads all relationships (from second level cache)
	    	List<Relationship> all = relationshipDAO.loadAll();
			Map<String, List<Term>> map = new HashMap<String, List<Term>>(all.size());
			for(Relationship relationship : all) {
				if(status.contains(relationship.getStatus())) {
					String parentReferenceId = relationship.getRelatedTerm().getReferenceId();
					List<Term> list = map.get(parentReferenceId);
					if(list == null) {
						list = new ArrayList<Term>();
						map.put(parentReferenceId, list);
					}
					list.add(relationship.getTerm());
				}
			}
			
			Set<Term> subterms = new LinkedHashSet<Term>();
			addSubterms(subterms, map, referenceId);
			return new ArrayList<Term>(subterms);
    	}
	}

	@Override
	public Term loadByReferenceId(String referenceId) {
		return loadByReferenceId(referenceId, false);
	}
	
	@Override
	public Term loadByReferenceId(String referenceId, boolean loadLazyAssociations) {
		try {
			Query query = entityManager.createNamedQuery(Term.QUERY_BY_REF_ID);
			query.setParameter("referenceId", referenceId.toUpperCase());
			Term term = (Term)query.getSingleResult();
			return loadLazyAssociations ? loadLazyAssociations(term) : term;
		} catch (NoResultException e) {
			throw new EntityNotFoundException("Failed to load term with reference id: " + referenceId);
		}
	}
	
	@Override
	public Term loadByName(String name, Ontology ontology) {
		return loadByName(name, ontology, false);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Term loadByName(String name, Ontology ontology, boolean loadLazyAssociations) {
		Query query = entityManager.createNamedQuery(Term.QUERY_BY_NAME);
		query.setParameter("name", name.toLowerCase());
		query.setParameter("ontology", ontology);
		
		// Causes transaction to be rolled back if term does not exist.
		// This method is used to check in a term already exists so
		// this is not desired.
		//return (Term)query.getSingleResult();
		
		List<Term> list = query.getResultList();
		Term term = list.isEmpty() ? null : list.get(0);
		return loadLazyAssociations ? loadLazyAssociations(term) : term;
	}
	
	@Override
	protected Term loadLazyAssociations(Term term) {
		if(term != null) {
			term.getCrossReferences().size();
			term.getRelationships().size();
			term.getSynonyms().size();
		}
		return super.loadLazyAssociations(term);
	}
	
	private void addSubterms(Set<Term> subterms, Map<String, List<Term>> map, String refId) {
		List<Term> terms = map.get(refId);
		if(terms != null) {
			for(Term term : terms) {
				if(subterms.add(term)) {
					addSubterms(subterms, map, term.getReferenceId());
				}
			}
		}
	}
}
