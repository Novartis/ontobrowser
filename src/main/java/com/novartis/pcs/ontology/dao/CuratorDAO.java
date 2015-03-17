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
import javax.persistence.Query;

import com.novartis.pcs.ontology.entity.Curator;

/**
 * Stateless session bean DAO for Curator entity
 */
@Stateless
@Local({CuratorDAOLocal.class})
@Remote({CuratorDAORemote.class})
public class CuratorDAO extends ModifiableEntityDAO<Curator> 
	implements CuratorDAOLocal, CuratorDAORemote {
       
    public CuratorDAO() {
        super();
    }

	@Override
	public Curator loadByUsername(String username) {
		try {
			Query query = entityManager.createNamedQuery(Curator.QUERY_BY_USERNAME);
			query.setParameter("username", username);
			return loadLazyAssociations((Curator)query.getSingleResult());
		} catch (javax.persistence.NoResultException e) {
			return null;
		}
	}
}
