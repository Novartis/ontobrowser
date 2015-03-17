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

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.novartis.pcs.ontology.entity.AbstractEntity;
import com.novartis.pcs.ontology.entity.InvalidEntityException;

/**
 * Base class session bean implementation of all DAOs
 */

public abstract class AbstractDAO<T extends AbstractEntity> implements DAO<T> {	
	@PersistenceContext(unitName = "ontobrowser")
	protected EntityManager entityManager;
	protected Class<T> entityClass;
	protected Logger logger = Logger.getLogger(getClass().getName());
	
	@Resource(lookup="java:global/ontobrowser/database/oracle")
	private boolean oracle;
	
	@SuppressWarnings("unchecked")
	public AbstractDAO() {
		this.entityClass = (Class<T>) ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
		  
    @Override
	public T load(long id) {
		return load(id, false);
	}
    
    @Override
	public T load(long id, boolean loadLazyAssociations) {
		T entity = entityManager.find(entityClass, id);
		if(entity != null && loadLazyAssociations) {
			entity = loadLazyAssociations(entity);
		}
		return entity;
	}

	@Override
	public List<T> load(long[] ids) {
		List<T> entities = new ArrayList<T>();
        
        if(ids != null && ids.length > 0) {
	        for(int i = 0; i < ids.length; i++) {
	        	T entity = entityManager.find(entityClass, ids[i]);
	        	if(entity != null) {
	        		entities.add(entity);
	        	}
	        }
        }
        return entities;
	}
		
	@Override
	@SuppressWarnings("unchecked")
	public List<T> loadLastCreated(int max) {
		Query query = entityManager.createQuery("SELECT e FROM " 
				+ entityClass.getSimpleName() 
				+ " as e order by e.createdDate desc");
       	query.setHint("org.hibernate.cacheable", Boolean.TRUE);
       	query.setMaxResults(max);
		return query.getResultList();
	}

	@Override
    @SuppressWarnings("unchecked")
	public List<T> loadAll() {
       	Query query = entityManager.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " as e");
       	query.setHint("org.hibernate.cacheable", Boolean.TRUE);
		return query.getResultList();
    }
	
	public void save(T entity) throws InvalidEntityException {
		if(entity.getId() != 0L) {
			throw new EntityExistsException(entity.getClass().getSimpleName() 
					+ " already exsits with id: " + entity.getId());
    	}
    	
    	entityManager.persist(entity);
    }
    
	@Override
	public void saveAll(Iterable<T> entities) throws InvalidEntityException {
		for(T entity: entities) {
			save(entity);
		}
	}
		
	@Override
	public void delete(T entity) throws InvalidEntityException {	
		try {
    		entityManager.remove(entityManager.getReference(entityClass, entity.getId()));
    	} catch(EntityNotFoundException e) {
    		// Entity has already been deleted. Do nothing.
    	}		
	}

	protected T loadLazyAssociations(T entity) {
    	return entity;
    }
	
	protected boolean isOracle() {		
		return oracle;
	}
}
