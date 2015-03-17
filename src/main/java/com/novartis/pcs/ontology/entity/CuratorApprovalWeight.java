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
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * CuratorApprovalWeight entity
 */
@Entity
@Table(name = "CURATOR_APPROVAL_WEIGHT", 
		uniqueConstraints = @UniqueConstraint(columnNames = {"CURATOR_ID", "TABLE_NAME" }))
@AttributeOverride(name = "id", 
		column = @Column(name = "CURATOR_APPROVAL_ID", unique = true, nullable = false))
@NamedQueries({
		@NamedQuery(name=CuratorApprovalWeight.QUERY_BY_CURATOR_ID,
				query="select w from CuratorApprovalWeight as w where w.curator.id = :curatorId",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class CuratorApprovalWeight extends ModifiableEntity {
	private static final long serialVersionUID = 1L;

	public enum Entity {     
        ONTOLOGY,
        RELATIONSHIP_TYPE,
        TERM,
        TERM_RELATIONSHIP,
        TERM_SYNONYM;
        
        
        public static Entity valueOf(VersionedEntity entity) {
        	if(entity instanceof Synonym) {
        		return TERM_SYNONYM;
        	}
        	
        	if(entity instanceof Relationship) {
        		return TERM_RELATIONSHIP;
        	}
        	
        	if(entity instanceof Term) {
        		return TERM;
        	}
        	
        	if(entity instanceof RelationshipType) {
        		return RELATIONSHIP_TYPE;
        	}
        	
        	if(entity instanceof Ontology) {
        		return ONTOLOGY;
        	}
        	
        	throw new IllegalArgumentException("Invalid entity: " + entity.getClass().getName());
        }
    }
    
    public static final String QUERY_BY_CURATOR_ID = "CuratorApprovalWeight.loadByCuratorId";
	
    @NotNull
    @Valid
    @ManyToOne(optional=false)
    @JoinColumn(name = "CURATOR_ID", nullable = false)
	private Curator curator;
	
    @NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "TABLE_NAME", nullable = false)
	private Entity entity;
	
    @NotNull
    @Min(value=0)
    @Max(value=1)
	@Column(name = "APPROVAL_WEIGHT", nullable = false)
	private BigDecimal approvalWeight;
	
	protected CuratorApprovalWeight() {
	}

	public CuratorApprovalWeight(Curator curator,
			Entity entity, BigDecimal approvalWeight, Curator creator) {
		super(creator);
		setCurator(curator);
		setEntity(entity);
		setApprovalWeight(approvalWeight);
		
		curator.getApprovalWeights().add(this);
	}
	
	public Curator getCurator() {
		return curator;
	}

	public void setCurator(Curator curator) {
		this.curator = curator;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public BigDecimal getApprovalWeight() {
		return this.approvalWeight;
	}

	public void setApprovalWeight(BigDecimal approvalWeight) {
		this.approvalWeight = approvalWeight;
	}
}
