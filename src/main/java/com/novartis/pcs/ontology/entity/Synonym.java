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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.novartis.pcs.ontology.entity.util.UrlParser;

/**
 * Synonym entity
 */
@Entity
@Table(name = "TERM_SYNONYM")
@AttributeOverride(name = "id", 
		column = @Column(name = "TERM_SYNONYM_ID", unique = true, nullable = false))
@AssociationOverride(name = "curatorActions", joinColumns = @JoinColumn(name = "TERM_SYNONYM_ID"))
@NamedQueries({
		@NamedQuery(name=Synonym.QUERY_BY_TERM_REF_ID,
				query="select s from Synonym as s inner join s.term as t where upper(t.referenceId) = :termRefId",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") }),
		@NamedQuery(name=Synonym.QUERY_BY_CTRLD_VOCAB_TERM,
				query="select s from Synonym as s where s.controlledVocabularyTerm = :ctrldVocabTerm",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") }),
		@NamedQuery(name=Synonym.QUERY_BY_SYNONYM,
				query="select s from Synonym as s where lower(trim(s.synonym)) = :synonym",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") }),
		@NamedQuery(name=Synonym.QUERY_BY_DATASOURCE,
				query="select s from Synonym as s"
					+ " where s.datasource = :datasource"
					+ " or s.controlledVocabularyTerm in (select t.id"
					+ " from ControlledVocabularyTerm as t"
					+ " inner join t.controlledVocabulary as v"
					+ " where v.datasource = :datasource)",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") }),
		@NamedQuery(name=Synonym.QUERY_BY_CTRLD_VOCAB_REF_ID,
				query="select s from Synonym as s"
					+ " where s.controlledVocabularyTerm in (select t.id"
					+ " from ControlledVocabularyTerm as t"
					+ " inner join t.controlledVocabulary as v"
					+ " where v.datasource = :datasource"
					+ " and v.referenceId = :referenceId)",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class Synonym extends VersionedEntity implements ReplaceableEntity<Synonym>, Comparable<Synonym> {
	private static final long serialVersionUID = 1L;

	public enum Type {BROAD, EXACT, NARROW, RELATED};
	
	public static final String QUERY_BY_TERM_REF_ID = "Synonym.loadByTermRefId";
	public static final String QUERY_BY_CTRLD_VOCAB_TERM = "Synonym.loadByCtrldVocabTermId";
	public static final String QUERY_BY_SYNONYM = "Synonym.loadBySynonym";
	public static final String QUERY_BY_DATASOURCE = "Synonym.loadByDatasource";
	public static final String QUERY_BY_CTRLD_VOCAB_REF_ID = "Synonym.loadByCtrldVocabRefId";
	
	@NotNull
	@Valid
	@ManyToOne(optional=false)
	@JoinColumn(name = "TERM_ID", nullable = false)
	private Term term;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "CTRLD_VOCAB_TERM_ID")
	private ControlledVocabularyTerm controlledVocabularyTerm;
	
	@NotNull
	@Column(name = "TERM_SYNONYM", nullable = false)
	private String synonym;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "SYNONYM_TYPE", nullable = false)
	private Type type;
	
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
	
	@Valid
	@ManyToOne(cascade={CascadeType.PERSIST})
	@JoinColumn(name = "REPLACED_BY")
	private Synonym replacedBy;
		
	protected Synonym() {
	}

	public Synonym(Term term, String synonym, Type type, 
			Curator creator, Version version) {
    	super(creator, version);
		setTerm(term);
		setSynonym(synonym);
		setType(type);
		
		term.getSynonyms().add(this);
	}
			
	public Term getTerm() {
		return this.term;
	}

	public void setTerm(Term term) {
		this.term = term;
	}
		
	public ControlledVocabularyTerm getControlledVocabularyTerm() {
		return controlledVocabularyTerm;
	}

	public void setControlledVocabularyTerm(
			ControlledVocabularyTerm controlledVocabularyTerm) {
		this.controlledVocabularyTerm = controlledVocabularyTerm;
	}

	public String getSynonym() {
		return this.synonym;
	}

	public void setSynonym(String synonym) {
		this.synonym = synonym;
	}

	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
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

	@Override
	public Synonym getReplacedBy() {
		return replacedBy;
	}

	@Override
	public void setReplacedBy(Synonym replacedBy) {
		this.replacedBy = replacedBy;
	}

	@Override
	public int compareTo(Synonym other) {
		return this.synonym.compareTo(other.synonym);
	}
	
	@Override
	public String toString() {
		return getSynonym();
	}

	public Synonym saveNewSynonym(Version version) {
		setStatus(getStatus());
		if(getStatus().equals(Status.APPROVED)) {
			setApprovedVersion(version);
		}
		setControlledVocabularyTerm(getControlledVocabularyTerm());
		setDatasource(getDatasource());
		setReferenceId(getReferenceId());
		setUrl(getUrl());
		return this;
	}
}
