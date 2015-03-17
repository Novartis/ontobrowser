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
package com.novartis.pcs.ontology.entity;

import static javax.persistence.GenerationType.SEQUENCE;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;

/**
 * EJB3 entity bean base class for all ontology entities
 *
 * @author Carlo Ravagli
 */

@MappedSuperclass
public abstract class AbstractEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	// Oracle sequence implementation
	@SequenceGenerator(name="ONTOLOGY_ENTITY_ID_SEQ", sequenceName="PRIMARY_KEY_SEQ", allocationSize=1)
	@GeneratedValue(strategy=SEQUENCE, generator="ONTOLOGY_ENTITY_ID_SEQ")
	// Non-Oracle (e.g. MySQL, PostgreSQL) auto increment implementation
	// @GeneratedValue(strategy=IDENTITY)
	private long id;
		
    protected AbstractEntity() {
    }
    
    protected void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
    	
    @Override
    public int hashCode() {
        return id > 0L ? (int)id : super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        // Can't use getClass() == obj.getClass()
        // because of Hibernate proxies
        // Can't use this.getClass().isInstance(obj)
        // because GWT does not support isInstance method.
        // Below works because database sequence for PK
        // is used for all tables/entities
        if(obj != null && obj instanceof AbstractEntity) {
            AbstractEntity ae = (AbstractEntity)obj;
            return id > 0L ? id == ae.getId() : super.equals(obj);
        }
        return false;
    }
}