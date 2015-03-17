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

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import com.novartis.pcs.ontology.entity.Ontology;

/**
 * Stateless session bean DAO for Ontology entity
 */
@Stateless
@Local({OntologyDAOLocal.class})
@Remote({OntologyDAORemote.class})
public class OntologyDAO extends VersionedEntityDAO<Ontology> 
	implements OntologyDAOLocal, OntologyDAORemote {
       
    public OntologyDAO() {
        super();
    }

	@Override
	public Ontology loadByName(String ontologyName) {
		return loadByName(ontologyName, false);
	}
	
	@Override
	public Ontology loadByName(String ontologyName, boolean lock) {
		try {
			Query query = entityManager.createNamedQuery(Ontology.QUERY_BY_NAME);			
			query.setParameter("name", ontologyName);
			
			if(lock) {
				query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
			}
			
			return (Ontology)query.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
}
