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
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.novartis.pcs.ontology.entity.util.UrlParser;

/**
 * Synonym entity
 */
@Entity
@Table(name = "TERM_XREF")
@AttributeOverride(name = "id", 
		column = @Column(name = "TERM_XREF_ID", unique = true, nullable = false))
public class CrossReference extends ModifiableEntity {
	private static final long serialVersionUID = 1L;
	
	@NotNull
	@Valid
	@ManyToOne(optional=false)
	@JoinColumn(name = "TERM_ID", nullable = false)
	private Term term;
			
	@Valid
	@ManyToOne
	@JoinColumn(name = "DATASOURCE_ID")
	private Datasource datasource;
	
	@Column(name = "REFERENCE_ID")
	private String referenceId;
	
	@Column(name = "DESCRIPTION")
	private String description;
	
	@Column(name = "XREF_URL")
	private String url;
	
	@Column(name = "IS_DEFINITION_XREF")
	private boolean definitionCrossReference;
		
	protected CrossReference() {
	}
	
	public CrossReference(Term term, String url, Curator creator) {
    	super(creator);
		setTerm(term);
		setUrl(url);
		
		term.getCrossReferences().add(this);
	}

	public CrossReference(Term term, Datasource datasource, String referenceId, 
			Curator creator) {
    	super(creator);
		setTerm(term);
		setDatasource(datasource);
		setReferenceId(referenceId);
		
		term.getCrossReferences().add(this);
	}
			
	public Term getTerm() {
		return this.term;
	}

	public void setTerm(Term term) {
		this.term = term;
	}
		
	public Datasource getDatasource() {
		return datasource;
	}

	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		if(url != null && url.trim().length() > 0) {
			try {
				this.url = UrlParser.parse(url.trim());
			} catch(Exception e) {
				throw new IllegalArgumentException("Invalid URL: " + e.getMessage());
			}
		} else {
			this.url = null;
		}
	}
	
	public boolean isDefinitionCrossReference() {
		return definitionCrossReference;
	}

	public void setDefinitionCrossReference(boolean definitionCrossReference) {
		this.definitionCrossReference = definitionCrossReference;
	}

	@Override
	public String toString() {
		String s = "";
		
		if(datasource != null) {
			s = datasource.getAcronym() + ":" + referenceId;
		} else if(url != null) {
			s = url;
		}
		
		return s;
	}
}
