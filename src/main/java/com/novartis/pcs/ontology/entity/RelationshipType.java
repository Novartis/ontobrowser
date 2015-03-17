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

import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * RelationshipType entity
 */
@Entity
@Table(name = "RELATIONSHIP_TYPE", uniqueConstraints = @UniqueConstraint(columnNames = "RELATIONSHIP_TYPE"))
@AttributeOverride(name = "id", 
		column = @Column(name = "RELATIONSHIP_TYPE_ID", unique = true, nullable = false))
@AssociationOverride(name = "curatorActions", joinColumns = @JoinColumn(name = "RELATIONSHIP_TYPE_ID"))
@NamedQueries({
		@NamedQuery(name=RelationshipType.QUERY_BY_RELSHIP,
				query="select rt from RelationshipType as rt where rt.relationship = :relationship",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class RelationshipType extends VersionedEntity implements ReplaceableEntity<RelationshipType> {
	private static final long serialVersionUID = 1L;
	
	public static final String QUERY_BY_RELSHIP = "RelationshipType.loadByRelationship";
	
	@NotNull
	@Column(name = "RELATIONSHIP_TYPE", unique = true, nullable = false)
	private String relationship;
	
	@NotNull
	@Column(name = "DEFINTION", nullable = false)
	private String defintion;
	
	@Column(name = "IS_CYCLIC", nullable = false)
	private boolean cyclic;
	
	@Column(name = "IS_SYMMETRIC", nullable = false)
	private boolean symmetric;
	
	@Column(name = "IS_TRANSITIVE", nullable = false)
	private boolean transitive;
	
	@Column(name = "EDGE_COLOUR")
	private String edgeColour;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "INVERSE_OF")
	private RelationshipType inverseOf;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "TRANSITIVE_OVER")
	private RelationshipType transitiveOver;		
	
	@Valid
	@ManyToOne(cascade={CascadeType.PERSIST})
	@JoinColumn(name = "REPLACED_BY")
	private RelationshipType replacedBy;

	protected RelationshipType() {
	}

	public RelationshipType(String relationship, String defintion,
			Curator creator, Version version) {
    	super(creator, version);
		setRelationship(relationship);
		setDefintion(defintion);
	}

	public String getRelationship() {
		return this.relationship;
	}

	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}
	
	public String getDefintion() {
		return this.defintion;
	}

	public void setDefintion(String defintion) {
		this.defintion = defintion;
	}

	public boolean isCyclic() {
		return cyclic;
	}

	public void setCyclic(boolean cyclic) {
		this.cyclic = cyclic;
	}

	public boolean isSymmetric() {
		return symmetric;
	}

	public void setSymmetric(boolean symmetric) {
		this.symmetric = symmetric;
	}

	public boolean isTransitive() {
		return transitive;
	}

	public void setTransitive(boolean transitive) {
		this.transitive = transitive;
	}
	
	public String getEdgeColour() {
		return edgeColour;
	}

	public void setEdgeColour(String edgeColour) {
		this.edgeColour = edgeColour;
	}

	public RelationshipType getInverseOf() {
		return inverseOf;
	}

	public void setInverseOf(RelationshipType inverseOf) {
		this.inverseOf = inverseOf;
	}

	public RelationshipType getTransitiveOver() {
		return transitiveOver;
	}

	public void setTransitiveOver(RelationshipType transitiveOver) {
		this.transitiveOver = transitiveOver;
	}

	@Override
	public RelationshipType getReplacedBy() {
		return this.replacedBy;
	}

	@Override
	public void setReplacedBy(RelationshipType replacedBy) {
		this.replacedBy = replacedBy;
	}
	
	@Override
	public String toString() {
		return getRelationship();
	}
}
