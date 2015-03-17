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

import java.util.EnumSet;
import java.util.List;

import javax.persistence.Query;

import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;

/**
 * Base class session bean implementation of all DAOs
 */

public abstract class VersionedEntityDAO<T extends VersionedEntity> extends CreatableEntityDAO<T> implements VersionedDAO<T> {
	public VersionedEntityDAO() {
		super();
	}
	
	@Override
    @SuppressWarnings("unchecked")
	public List<T> loadByStatus(EnumSet<Status> status) {
       	Query query = entityManager.createQuery("SELECT e FROM " 
       			+ entityClass.getSimpleName()
       			+ " as e where e.status in (:status)");
       	query.setHint("org.hibernate.cacheable", Boolean.TRUE);
       	query.setParameter("status", status);
		return query.getResultList();
	}
    
	@Override
	public void delete(T entity) throws InvalidEntityException {
		if(!entity.getStatus().equals(Status.PENDING)) {
			throw new InvalidEntityException(entity, "Only PENDING entities can be deleted");
		}
				
		super.delete(entity);
	}

	@Override
	protected T loadLazyAssociations(T entity) {
		if(entity != null) {
			entity.getCuratorActions().size();
		}
		return entity;
	}
}
