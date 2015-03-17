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
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;

@Entity
@Table(name = "CTRLD_VOCAB_TERM_LINK", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"CTRLD_VOCAB_TERM_ID", "LINKED_CTRLD_VOCAB_TERM_ID"})})
@AttributeOverride(name = "id", 
		column = @Column(name = "CTRLD_VOCAB_TERM_LINK_ID", unique = true, nullable = false))
public class ControlledVocabularyTermLink extends ModifiableEntity {
	private static final long serialVersionUID = 1L;
		
	@Valid
	@ManyToOne
	@JoinColumn(name = "CTRLD_VOCAB_TERM_ID", nullable = false)
	private ControlledVocabularyTerm controlledVocabularyTerm;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "LINKED_CTRLD_VOCAB_TERM_ID", nullable = false)
	private ControlledVocabularyTerm linkedControlledVocabularyTerm;
		
	@Column(name = "USAGE_COUNT", nullable = false)
	private int usage;
	
	protected ControlledVocabularyTermLink() {
		
	}
	
	public ControlledVocabularyTermLink(ControlledVocabularyTerm controlledVocabulary, 
			ControlledVocabularyTerm linkedControlledVocabulary, int usage, Curator creator) {
		super(creator);
		setControlledVocabularyTerm(controlledVocabularyTerm);
		setLinkedControlledVocabularyTerm(linkedControlledVocabularyTerm);
		setUsage(usage);
		
		controlledVocabulary.getLinks().add(this);
	}
	
	public ControlledVocabularyTerm getControlledVocabularyTerm() {
		return controlledVocabularyTerm;
	}
	
	public void setControlledVocabularyTerm(ControlledVocabularyTerm controlledVocabularyTerm) {
		this.controlledVocabularyTerm = controlledVocabularyTerm;
	}
	
	public ControlledVocabularyTerm getLinkedControlledVocabularyTerm() {
		return linkedControlledVocabularyTerm;
	}
	
	public void setLinkedControlledVocabularyTerm(ControlledVocabularyTerm linkedControlledVocabularyTerm) {
		this.linkedControlledVocabularyTerm = linkedControlledVocabularyTerm;
	}
	
	public int getUsage() {
		return usage;
	}
	
	public void setUsage(int usgae) {
		this.usage = usgae;
	}
}
