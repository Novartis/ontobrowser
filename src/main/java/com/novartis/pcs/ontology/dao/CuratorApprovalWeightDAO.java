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

import com.novartis.pcs.ontology.entity.CuratorApprovalWeight;

/**
 * Stateless session bean DAO for CuratorApprovalWeight entity
 */
@Stateless
@Local({CuratorApprovalWeightDAOLocal.class})
@Remote({CuratorApprovalWeightDAORemote.class})
public class CuratorApprovalWeightDAO extends ModifiableEntityDAO<CuratorApprovalWeight> 
	implements CuratorApprovalWeightDAOLocal, CuratorApprovalWeightDAORemote {
       
    public CuratorApprovalWeightDAO() {
        super();
    }

	@SuppressWarnings("unchecked")
	@Override
	public Collection<CuratorApprovalWeight> loadByCuratorId(long curatorId) {
		Query query = entityManager.createNamedQuery(CuratorApprovalWeight.QUERY_BY_CURATOR_ID);
		query.setParameter("curatorId", curatorId);
		return query.getResultList();
	}
}
