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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Ontology entity
 */
@Entity
@Table(name = "ONTOLOGY", uniqueConstraints = @UniqueConstraint(columnNames = "ONTOLOGY_NAME"))
@AttributeOverride(name = "id", 
		column = @Column(name = "ONTOLOGY_ID", unique = true, nullable = false))
@NamedQueries({
		@NamedQuery(name=Ontology.QUERY_BY_NAME,
				query="select o from Ontology as o where o.name = :name",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class Ontology extends VersionedEntity implements ReplaceableEntity<Ontology> {
	private static final long serialVersionUID = 1L;
	
	public static final String QUERY_BY_NAME = "Ontology.loadByName";
	
	@NotNull
	@Column(name = "ONTOLOGY_NAME", unique = true, nullable = false)
	private String name;
	
	@Column(name = "DESCRIPTION")
	private String description;
	
	@Column(name = "IS_INTERNAL", nullable = false)
	private boolean internal;
	
	@Column(name = "SOURCE_NAMESPACE")
	private String sourceNamespace;
	
	@Column(name = "SOURCE_URI")
	private String sourceUri;
	
	@Column(name = "SOURCE_RELEASE")
	private String sourceRelease;
	
	@Temporal(TemporalType.DATE)
	@Column(name = "SOURCE_DATE")
	private Date sourceDate;
	
	@Column(name = "SOURCE_FORMAT")
	private String sourceFormat;
	
	@Column(name = "REFERENCE_ID_PREFIX")
	private String referenceIdPrefix;
	
	@Column(name = "REFERENCE_ID_VALUE")
	private int referenceIdValue;
	
	@Column(name = "IS_CODELIST", nullable = false)
	private boolean codelist;
	
	@Valid
	@ManyToOne(cascade={CascadeType.PERSIST})
	@JoinColumn(name = "REPLACED_BY")
	private Ontology replacedBy;
	
	protected Ontology() {
	}

	public Ontology(String name, Curator creator, Version version) {
    	super(creator, version);
		setName(name);
	}
		
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public boolean isInternal() {
		return this.internal;
	}

	public void setInternal(boolean internal) {
		this.internal = internal;
	}

	public String getSourceNamespace() {
		return this.sourceNamespace;
	}

	public void setSourceNamespace(String sourceNamespace) {
		this.sourceNamespace = sourceNamespace;
	}
	
	public String getSourceUri() {
		return this.sourceUri;
	}

	public void setSourceUri(String sourceUri) {
		this.sourceUri = sourceUri;
	}

	public String getSourceRelease() {
		return this.sourceRelease;
	}

	public void setSourceRelease(String sourceRelease) {
		this.sourceRelease = sourceRelease;
	}
	
	public Date getSourceDate() {
		return this.sourceDate;
	}

	public void setSourceDate(Date sourceDate) {
		this.sourceDate = sourceDate;
	}
	
	public String getSourceFormat() {
		return this.sourceFormat;
	}

	public void setSourceFormat(String sourceFormat) {
		this.sourceFormat = sourceFormat;
	}
	
	public String getReferenceIdPrefix() {
		return referenceIdPrefix;
	}

	public void setReferenceIdPrefix(String referenceIdPrefix) {
		this.referenceIdPrefix = referenceIdPrefix;
	}
		
	public int getReferenceIdValue() {
		return referenceIdValue;
	}

	public void setReferenceIdValue(int referenceIdValue) {
		this.referenceIdValue = referenceIdValue;
	}

	public boolean isCodelist() {
		return codelist;
	}
	
	public void setCodelist(boolean codelist) {
		this.codelist = codelist;
	}

	@Override
	public Ontology getReplacedBy() {
		return replacedBy;
	}

	@Override
	public void setReplacedBy(Ontology replacedBy) {
		this.replacedBy = replacedBy;	
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
