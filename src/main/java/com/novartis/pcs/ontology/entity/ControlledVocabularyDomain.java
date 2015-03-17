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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "CTRLD_VOCAB_DOMAIN", uniqueConstraints = {
		@UniqueConstraint(columnNames = "CTRLD_VOCAB_DOMAIN")})
@AttributeOverride(name = "id", 
		column = @Column(name = "CTRLD_VOCAB_DOMAIN_ID", unique = true, nullable = false))
public class ControlledVocabularyDomain extends ModifiableEntity {
	private static final long serialVersionUID = 1L;
	
	@NotNull
	@Column(name = "CTRLD_VOCAB_DOMAIN", nullable = false)
	private String name;
	
	@Valid
	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(name = "CTRLD_VOCAB_DOMAIN_ONTOLOGY", 
			joinColumns = { 
					@JoinColumn(name = "CTRLD_VOCAB_DOMAIN_ID", nullable = false) }, 
			inverseJoinColumns = { 
					@JoinColumn(name = "ONTOLOGY_ID", nullable = false) })
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	private Set<Ontology> ontologies = new HashSet<Ontology>(0);
	
	protected ControlledVocabularyDomain() {
		
	}
	
	public ControlledVocabularyDomain(String name,
			Curator creator) {
		super(creator);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Set<Ontology> getOntologies() {
		return ontologies;
	}

	public void setOntologies(Set<Ontology> ontologies) {
		this.ontologies = ontologies;
	}

	@Override
	public String toString() {
		return name;
	}
}
