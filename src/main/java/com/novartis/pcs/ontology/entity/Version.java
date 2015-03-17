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
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;

/**
 * Version entity
 */
@Entity
@Table(name = "VERSION")
@AttributeOverrides( {
		@AttributeOverride(name = "id", column = 
				@Column(name = "VERSION_ID", unique = true, nullable = false)) })
public class Version extends CreatableEntity implements Comparable<Version> {
	private static final long serialVersionUID = 1L;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "PUBLISHED_BY")
	private Curator publishedBy;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "PUBLISHED_DATE")
	private Date publishedDate;

	protected Version() {
	}
	
	public Version(Curator creator) {
		super(creator);
	}

	public Curator getPublishedBy() {
		return publishedBy;
	}

	public void setPublishedBy(Curator publishedBy) {
		this.publishedBy = publishedBy;
	}

	public Date getPublishedDate() {
		return publishedDate;
	}

	public void setPublishedDate(Date publishedDate) {
		this.publishedDate = publishedDate;
	}

	@Override
	public int compareTo(Version other) {
		return publishedDate.compareTo(other.publishedDate);
	}
}
