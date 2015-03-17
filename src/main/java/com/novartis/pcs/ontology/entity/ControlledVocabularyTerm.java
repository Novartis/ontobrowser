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

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "CTRLD_VOCAB_TERM", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"CTRLD_VOCAB_TERM", "CTRLD_VOCAB_ID"})})
@AttributeOverride(name = "id", 
		column = @Column(name = "CTRLD_VOCAB_TERM_ID", unique = true, nullable = false))
@NamedQueries({
		@NamedQuery(name=ControlledVocabularyTerm.QUERY_UNMAPPED,
				query="select t from ControlledVocabularyTerm as t" +
						" inner join t.controlledVocabulary as c" +
						" where t.id not in (select s.controlledVocabularyTerm.id" +
						" from Synonym as s" +
						" where s.controlledVocabularyTerm is not null" +
						" and s.status in ('PENDING','APPROVED'))",
				hints={ @QueryHint(name="javax.persistence.cache.storeMode", value="BYPASS"),
						@QueryHint(name="javax.persistence.cache.retrieveMode", value="BYPASS"),
						@QueryHint(name="org.hibernate.cacheable", value="false") }),
		@NamedQuery(name=ControlledVocabularyTerm.QUERY_UNMAPPED_BY_DOMAIN,
				query="select t from ControlledVocabularyTerm as t" +
						" inner join t.controlledVocabulary as c" +
						" where c.domain = :domain" +
						" and t.id not in (select s.controlledVocabularyTerm.id" +
						" from Synonym as s" +
						" where s.controlledVocabularyTerm is not null" +
						" and s.status in ('PENDING','APPROVED'))",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") }),
		@NamedQuery(name=ControlledVocabularyTerm.QUERY_UNMAPPED_BY_CONTEXT,
				query="select t from ControlledVocabularyTerm as t" +
						" inner join t.controlledVocabulary as c" +
						" where c.domain = :domain" +
						" and c.context = :context" +
						" and t.id not in (select s.controlledVocabularyTerm.id" +
						" from Synonym as s" +
						" where s.controlledVocabularyTerm is not null" +
						" and s.status in ('PENDING','APPROVED'))",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") }),
		@NamedQuery(name=ControlledVocabularyTerm.QUERY_UNMAPPED_BY_SOURCE,
				query="select t from ControlledVocabularyTerm as t" +
						" inner join t.controlledVocabulary as c" +
						" where c.domain = :domain" +
						" and c.datasource = :datasource" +
						" and t.id not in (select s.controlledVocabularyTerm.id" +
						" from Synonym as s" +
						" where s.controlledVocabularyTerm is not null" +
						" and s.status in ('PENDING','APPROVED'))",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") }),
		@NamedQuery(name=ControlledVocabularyTerm.QUERY_UNMAPPED_BY_CONTEXT_AND_SOURCE,
				query="select t from ControlledVocabularyTerm as t" +
						" inner join t.controlledVocabulary as c" +
						" where c.domain = :domain" +
						" and c.context = :context" +
						" and c.datasource = :datasource" +
						" and t.id not in (select s.controlledVocabularyTerm.id" +
						" from Synonym as s" +
						" where s.controlledVocabularyTerm is not null" +
						" and s.status in ('PENDING','APPROVED'))",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class ControlledVocabularyTerm extends ModifiableEntity
		implements Comparable<ControlledVocabularyTerm> {
	private static final long serialVersionUID = 1L;
	
	public static final String QUERY_UNMAPPED = "ControlledVocabularyTerm.unmapped";
	public static final String QUERY_UNMAPPED_BY_DOMAIN = "ControlledVocabularyTerm.unmappedByDomain";
	public static final String QUERY_UNMAPPED_BY_CONTEXT = "ControlledVocabularyTerm.unmappedByContext";
	public static final String QUERY_UNMAPPED_BY_SOURCE = "ControlledVocabularyTerm.unmappedBySource";
	public static final String QUERY_UNMAPPED_BY_CONTEXT_AND_SOURCE = "ControlledVocabularyTerm.unmappedByContextAndSource";
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "CTRLD_VOCAB_ID", nullable = false)
	private ControlledVocabulary controlledVocabulary;
	
	@NotNull
	@Column(name = "CTRLD_VOCAB_TERM", nullable = false)
	private String name;
	
	@Column(name = "REFERENCE_ID")
	private String referenceId;
	
	@Column(name = "USAGE_COUNT", nullable = false)
	private int usage;
	
	@Column(name = "IS_EXCLUDED", nullable = false)
	private boolean excluded;
	
	@Valid
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "controlledVocabularyTerm")
	@OrderBy("usage DESC")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	private List<ControlledVocabularyTermLink> links = new ArrayList<ControlledVocabularyTermLink>(0);
	
	protected ControlledVocabularyTerm() {
		
	}
	
	public ControlledVocabularyTerm(ControlledVocabulary controlledVocabulary, 
			String name, int usage, Curator creator) {
		super(creator);
		this.controlledVocabulary = controlledVocabulary;
		this.name = name;
		this.usage = usage;
	}
	
	public ControlledVocabulary getControlledVocabulary() {
		return controlledVocabulary;
	}
	
	public void setControlledVocabulary(ControlledVocabulary controlledVocabulary) {
		this.controlledVocabulary = controlledVocabulary;
	}

	public String getReferenceId() {
		return referenceId;
	}
	
	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getUsage() {
		return usage;
	}
	
	public void setUsage(int usgae) {
		this.usage = usgae;
	}
		
	public boolean isExcluded() {
		return excluded;
	}

	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}

	public List<ControlledVocabularyTermLink> getLinks() {
		return links;
	}

	public void setLinks(List<ControlledVocabularyTermLink> links) {
		this.links = links;
	}

	@Override
	public int compareTo(ControlledVocabularyTerm other) {
		return name.compareToIgnoreCase(other.name);
	}

	@Override
	public String toString() {
		return name;
	}
}
