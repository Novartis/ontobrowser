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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Curator entity
 */
@Entity
@Table(name = "CURATOR", uniqueConstraints = @UniqueConstraint(columnNames = "USERNAME"))
@AttributeOverride(name = "id", 
		column = @Column(name = "CURATOR_ID", unique = true, nullable = false))
@NamedQueries({
		@NamedQuery(name=Curator.QUERY_BY_USERNAME,
				query="select c from Curator as c where c.username = :username",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class Curator extends ModifiableEntity {
	private static final long serialVersionUID = 1L;
	
	public static final String QUERY_BY_USERNAME = "Curator.loadByUsername";

	@NotNull
	@Column(name = "USERNAME", unique = true, nullable = false)
	private String username;
	
	@Column(name = "EMAIL_ADDRESS")
	private String emailAddress;
		
	@Column(name = "IS_ACTIVE", nullable = false)
	private boolean active;
	
	@Column(name = "PASSWORD")
	private String password;
	
	@Column(name = "PASSWORD_EXPIRED", nullable = false)
	private boolean passwordExpired;

	@Valid
	@OneToMany(fetch=FetchType.EAGER, mappedBy = "curator", 
			cascade={CascadeType.PERSIST, CascadeType.REMOVE})
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	private Set<CuratorApprovalWeight> approvalWeights = new HashSet<CuratorApprovalWeight>(0);
	
	protected Curator() {
	}

	public Curator(String username, Curator creator) {
		super(creator);
		setUsername(username);
		setActive(true);
	}

	public String getUsername() {
		return this.username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public boolean isPasswordExpired() {
		return passwordExpired;
	}

	public void setPasswordExpired(boolean passwordExpired) {
		this.passwordExpired = passwordExpired;
	}

	public Set<CuratorApprovalWeight> getApprovalWeights() {
		return approvalWeights;
	}

	public void setApprovalWeights(Set<CuratorApprovalWeight> approvalWeights) {
		this.approvalWeights = approvalWeights;
	}
	
	public BigDecimal getEntityApprovalWeight(CuratorApprovalWeight.Entity entity) {
		BigDecimal entityApprovalWeight = BigDecimal.ZERO;
		for(CuratorApprovalWeight approvalWeight : approvalWeights) {
			if(approvalWeight.getEntity().equals(entity)) {
				entityApprovalWeight = approvalWeight.getApprovalWeight();
				break;
			}
		}
		return entityApprovalWeight;
	}
	
	public boolean isAuthorised(CuratorApprovalWeight.Entity entity) {
		return getEntityApprovalWeight(entity).compareTo(BigDecimal.ZERO) > 0;
	}
		
	@Override
	public String toString() {
		return getUsername();
	}
}
