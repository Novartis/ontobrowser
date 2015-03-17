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
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;

import com.novartis.pcs.ontology.dao.ModifiableEntityValidator;

/**
 * Base class for all modifiable ontology entities
 *
 * @author Carlo Ravagli
 */

@MappedSuperclass
@EntityListeners(ModifiableEntityValidator.class)
public abstract class ModifiableEntity extends CreatableEntity {
	private static final long serialVersionUID = 1L;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "MODIFIED_BY")
	private Curator modifiedBy;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "MODIFIED_DATE")
	private Date modifiedDate;

    protected ModifiableEntity() {
    }
    
    protected ModifiableEntity(Curator creator) {
    	super(creator);
    }
    
	public Curator getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Curator modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Date getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	
	@PreUpdate
    public void setModifiedDate() {
		/*
		if(this.getModifiedBy() == null) {
			throw new InvalidEntityException(this, "The curator that modified the entity must be defined");
		}
		*/
		if(this.getModifiedDate() == null) {
			this.setModifiedDate(new Date());
		}
	}
}