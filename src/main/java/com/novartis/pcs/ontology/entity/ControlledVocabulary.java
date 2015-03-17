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

import javax.persistence.AttributeOverride;
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

@Entity
@Table(name = "CTRLD_VOCAB", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"CTRLD_VOCAB_NAME", "DATASOURCE_ID"}),
		@UniqueConstraint(columnNames = { "CTRLD_VOCAB_CONTEXT_ID", 
                "CTRLD_VOCAB_DOMAIN_ID", "DATASOURCE_ID" }) })
@AttributeOverride(name = "id", 
		column = @Column(name = "CTRLD_VOCAB_ID", unique = true, nullable = false))
@NamedQueries({
		@NamedQuery(name=ControlledVocabulary.QUERY_UNMAPPED_TERMS,
				query="select distinct c from ControlledVocabularyTerm as t" +
						" inner join t.controlledVocabulary as c" +
						" where t.id not in (select s.controlledVocabularyTerm.id" +
						" from Synonym as s" +
						" where s.controlledVocabularyTerm is not null" +
						" and s.status in ('PENDING','APPROVED'))",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class ControlledVocabulary extends ModifiableEntity {
	private static final long serialVersionUID = 1L;
	
	public static final String QUERY_UNMAPPED_TERMS = "ControlledVocabulary.unmappedTerms";
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "DATASOURCE_ID", nullable = false)
	private Datasource datasource;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "CTRLD_VOCAB_DOMAIN_ID", nullable = false)
	private ControlledVocabularyDomain domain;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "CTRLD_VOCAB_CONTEXT_ID", nullable = false)
	private ControlledVocabularyContext context;
	
	@NotNull
	@Column(name = "CTRLD_VOCAB_NAME", nullable = false)
	private String name;
	
	@Column(name = "REFERENCE_ID")
	private String referenceId;
	
	protected ControlledVocabulary() {
		
	}
	
	public ControlledVocabulary(Datasource datasource, 
			ControlledVocabularyDomain domain,
			ControlledVocabularyContext context,
			String name,
			Curator creator) {
		super(creator);
		this.datasource = datasource;
		this.domain = domain;
		this.context = context;
		this.name = name;
	}
	
	public Datasource getDatasource() {
		return datasource;
	}
	
	protected void setDatasource(Datasource datasource) {
		this.datasource = datasource;
	}
	
	public ControlledVocabularyDomain getDomain() {
		return domain;
	}
	
	public void setDomain(ControlledVocabularyDomain domain) {
		this.domain = domain;
	}
	
	public ControlledVocabularyContext getContext() {
		return context;
	}

	public void setContext(ControlledVocabularyContext context) {
		this.context = context;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	@Override
	public String toString() {
		return name;
	}
}
