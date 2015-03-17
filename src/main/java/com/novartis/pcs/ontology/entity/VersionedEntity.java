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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.novartis.pcs.ontology.dao.VersionedEntityValidator;

/**
 * Based class for all versioned ontology entities
 *
 * @author Carlo Ravagli
 */

@MappedSuperclass
@EntityListeners(VersionedEntityValidator.class)
public abstract class VersionedEntity extends CreatableEntity {
	private static final long serialVersionUID = 1L;
	
	public enum Status {PENDING, APPROVED, REJECTED, OBSOLETE};	
	
	@NotNull
	@Valid
	@ManyToOne(optional=false)
	@JoinColumn(name = "CREATED_VERSION_ID", nullable = false)
	private Version createdVersion;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "APPROVED_VERSION_ID")
	private Version approvedVersion;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "OBSOLETE_VERSION_ID")
	private Version obsoleteVersion;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", nullable = false)
	private Status status;
	
	@Valid
	@OneToMany(fetch = FetchType.LAZY, 
			cascade={CascadeType.PERSIST, CascadeType.REMOVE})
	@JoinColumn(name = "") // see https://hibernate.onjira.com/browse/HHH-4384
	@OrderBy("actionDate")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	private List<CuratorAction> curatorActions = new ArrayList<CuratorAction>(0);
		
    protected VersionedEntity() {
    }
    
    protected VersionedEntity(Curator creator, Version version) {
    	super(creator);
    	setCreatedVersion(version);
    	setStatus(Status.PENDING);
    }

	public Version getCreatedVersion() {
		return createdVersion;
	}

	public void setCreatedVersion(Version createdVersion) {
		this.createdVersion = createdVersion;
	}

	public Version getApprovedVersion() {
		return approvedVersion;
	}

	public void setApprovedVersion(Version approvedVersion) {
		this.approvedVersion = approvedVersion;
	}

	public Version getObsoleteVersion() {
		return obsoleteVersion;
	}

	public void setObsoleteVersion(Version obsoleteVersion) {
		this.obsoleteVersion = obsoleteVersion;
	}
	
	public Status getStatus() {
		return this.status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
	@PrePersist
	public void setStatus() {
		if(this.getStatus() == null) {
			this.setStatus(Status.PENDING);
		}
	}
	
	public List<CuratorAction> getCuratorActions() {
		return curatorActions;
	}
	
	public void setCuratorActions(List<CuratorAction> curatorActions) {
		this.curatorActions = curatorActions;
	}
}