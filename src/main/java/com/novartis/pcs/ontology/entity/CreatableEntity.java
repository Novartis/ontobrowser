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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.novartis.pcs.ontology.dao.CreatableEntityValidator;


/**
 * EJB3 entity bean base class for all creatable ontology entities
 *
 * @author Carlo Ravagli
 */

@MappedSuperclass
@EntityListeners(CreatableEntityValidator.class)
public abstract class CreatableEntity extends AbstractEntity {
	private static final long serialVersionUID = 1L;
	
	@NotNull
	@Valid
	@ManyToOne(optional=false)
	@JoinColumn(name = "CREATED_BY", nullable = false)
	private Curator createdBy;
	
	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATED_DATE", nullable = false)
	private Date createdDate;

    protected CreatableEntity() {
    }
    
    protected CreatableEntity(Curator creator) {
    	setCreatedBy(creator);
    	setCreatedDate(new Date());
    }
    
    public Curator getCreatedBy() {
		return this.createdBy;
	}

    public void setCreatedBy(Curator createdBy) {
		this.createdBy = createdBy;
	}
    
    public Date getCreatedDate() {
		return this.createdDate;
	}

    public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
    
    @PrePersist
    public void setCreatedDate() {
    	if(this.getCreatedDate() == null) {
    		this.setCreatedDate(new Date());
    	}
    }
}