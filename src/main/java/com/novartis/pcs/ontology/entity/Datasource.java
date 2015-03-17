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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * Datasource entity
 */
@Entity
@Table(name = "DATASOURCE", uniqueConstraints = {
		@UniqueConstraint(columnNames = "DATASOURCE_ACRONYM"),
		@UniqueConstraint(columnNames = "DATASOURCE_NAME") })
@AttributeOverride(name = "id", 
		column = @Column(name = "DATASOURCE_ID", unique = true, nullable = false))
@NamedQueries({
		@NamedQuery(name=Datasource.QUERY_BY_ACRONYM,
				query="select d from Datasource as d where d.acronym = :acronym",
				hints={ @QueryHint(name="org.hibernate.cacheable", value="true") })
})
public class Datasource extends ModifiableEntity {
	private static final long serialVersionUID = 1L;
	
	public static final String QUERY_BY_ACRONYM = "Datasource.loadByAcronym";

	@NotNull
	@Column(name = "DATASOURCE_NAME", unique = true, nullable = false)
	private String name;
	
	@NotNull
	@Column(name = "DATASOURCE_ACRONYM", unique = true, nullable = false)
	private String acronym;
	
	@Column(name = "DATASOURCE_URI")
	private String uri;
	
	@Column(name = "IS_INTERNAL", nullable = false)
	private boolean internal;
	
	@Column(name = "IS_PUBLIC", nullable = false)
	private boolean publiclyAccessible;
		
	protected Datasource() {
	}

	public Datasource(String name, String acronym, 
			Curator creator) {
    	super(creator);
		setName(name);
		setAcronym(acronym);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAcronym() {
		return acronym;
	}

	public void setAcronym(String acronym) {
		this.acronym = acronym;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public boolean isInternal() {
		return internal;
	}

	public void setInternal(boolean internal) {
		this.internal = internal;
	}
	
	public boolean isPubliclyAccessible() {
		return publiclyAccessible;
	}

	public void setPubliclyAccessible(boolean publiclyAccessible) {
		this.publiclyAccessible = publiclyAccessible;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
