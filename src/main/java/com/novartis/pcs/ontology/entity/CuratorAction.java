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

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * CuratorAction entity
 */
@Entity
@Table(name = "CURATOR_ACTION")
@AttributeOverride(name = "id", 
		column = @Column(name = "CURATOR_ACTION_ID", unique = true, nullable = false))

@NamedQueries({
		@NamedQuery(name=CuratorAction.QUERY_BY_CURATOR_ID,
				query="select a from CuratorAction as a where a.curator.id = :curatorId",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class CuratorAction extends AbstractEntity {
	private static final long serialVersionUID = 1L;
	
	public enum Action {APPROVE, REJECT, REPLACE};
	
	public static final String QUERY_BY_CURATOR_ID = "CuratorAction.loadByCuratorId";
	
	@NotNull
	@Valid
	@ManyToOne(optional=false)
	@JoinColumn(name = "CURATOR_ID", nullable = false)
	private Curator curator;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "ACTION", nullable = false)
	private Action action;
	
	@ManyToOne
	@JoinColumn(name="TERM_ID", insertable = false, updatable = false)
	private Term term;
	
	@ManyToOne
	@JoinColumn(name="TERM_RELATIONSHIP_ID", insertable = false, updatable = false)
	private Relationship relationship;
	
	@ManyToOne
	@JoinColumn(name="TERM_SYNONYM_ID", insertable = false, updatable = false)
	private Synonym synonym;
	
	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "ACTION_DATE", nullable = false)
	private Date actionDate;
	
	@Column(name = "COMMENTS")
	private String comments;
		
	protected CuratorAction() {
	}

	public CuratorAction(Curator curator, Action action, 
			VersionedEntity entity, Date actionDate) {
		setCurator(curator);
		setAction(action);
		setActionDate(actionDate);
		setEntity(entity);
	}
	
	public CuratorAction(Curator curator, 
			Action action, VersionedEntity entity) {
		this(curator, action, entity, new Date());
	}
		
	public Curator getCurator() {
		return this.curator;
	}

	public void setCurator(Curator curator) {
		this.curator = curator;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}
	
	public VersionedEntity getEntity() {
		return term != null ? term :
			relationship != null ? relationship :
				synonym != null ? synonym : null;
	}
	
	public void setEntity(VersionedEntity entity) {
		if(entity instanceof Term) {
			term = (Term)entity;
		} else if(entity instanceof Relationship) {
			relationship = (Relationship)entity;
		} else if(entity instanceof Synonym) {
			synonym = (Synonym)entity;
		} else {
			throw new IllegalArgumentException("Curator action for " 
					+ entity.getClass().getName() + " is not supported");
		}
		entity.getCuratorActions().add(this);
	}
	
	public Date getActionDate() {
		return actionDate;
	}

	public void setActionDate(Date actionDate) {
		this.actionDate = actionDate;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
}
