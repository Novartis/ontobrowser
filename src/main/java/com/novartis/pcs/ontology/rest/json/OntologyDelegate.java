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
package com.novartis.pcs.ontology.rest.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorAction;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.Version;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.service.util.TermReferenceIdComparator;

public class OntologyDelegate extends Ontology {
	private static final long serialVersionUID = 1L;
	
	private final Ontology ontology;
	private Collection<Term> terms;
	
	public OntologyDelegate(Ontology ontology, Collection<Term> terms) {
		this.ontology = ontology;
		this.terms = filter(terms);
	}
		
	public Version getApprovedVersion() {
		return ontology.getApprovedVersion();
	}

	public Curator getCreatedBy() {
		return ontology.getCreatedBy();
	}

	public Date getCreatedDate() {
		return ontology.getCreatedDate();
	}

	public Version getCreatedVersion() {
		return ontology.getCreatedVersion();
	}

	public List<CuratorAction> getCuratorActions() {
		return ontology.getCuratorActions();
	}

	public String getDescription() {
		return ontology.getDescription();
	}

	public long getId() {
		return ontology.getId();
	}

	public String getName() {
		return ontology.getName();
	}

	public Version getObsoleteVersion() {
		return ontology.getObsoleteVersion();
	}

	public String getReferenceIdPrefix() {
		return ontology.getReferenceIdPrefix();
	}

	public Ontology getReplacedBy() {
		return ontology.getReplacedBy();
	}

	public Date getSourceDate() {
		return ontology.getSourceDate();
	}

	public String getSourceFormat() {
		return ontology.getSourceFormat();
	}

	public String getSourceNamespace() {
		return ontology.getSourceNamespace();
	}

	public String getSourceRelease() {
		return ontology.getSourceRelease();
	}

	public String getSourceUri() {
		return ontology.getSourceUri();
	}

	public Status getStatus() {
		return ontology.getStatus();
	}

	public boolean isInternal() {
		return ontology.isInternal();
	}

	public void setApprovedVersion(Version approvedVersion) {
		ontology.setApprovedVersion(approvedVersion);
	}

	public void setCreatedBy(Curator createdBy) {
		ontology.setCreatedBy(createdBy);
	}

	public void setCreatedDate() {
		ontology.setCreatedDate();
	}

	public void setCreatedDate(Date createdDate) {
		ontology.setCreatedDate(createdDate);
	}

	public void setCreatedVersion(Version createdVersion) {
		ontology.setCreatedVersion(createdVersion);
	}

	public void setCuratorActions(List<CuratorAction> curatorActions) {
		ontology.setCuratorActions(curatorActions);
	}

	public void setDescription(String description) {
		ontology.setDescription(description);
	}

	public void setInternal(boolean internal) {
		ontology.setInternal(internal);
	}

	public void setName(String name) {
		ontology.setName(name);
	}

	public void setObsoleteVersion(Version obsoleteVersion) {
		ontology.setObsoleteVersion(obsoleteVersion);
	}

	public void setReferenceIdPrefix(String referenceIdPrefix) {
		ontology.setReferenceIdPrefix(referenceIdPrefix);
	}

	public void setReplacedBy(Ontology replacedBy) {
		ontology.setReplacedBy(replacedBy);
	}

	public void setSourceDate(Date sourceDate) {
		ontology.setSourceDate(sourceDate);
	}

	public void setSourceFormat(String sourceFormat) {
		ontology.setSourceFormat(sourceFormat);
	}

	public void setSourceNamespace(String sourceNamespace) {
		ontology.setSourceNamespace(sourceNamespace);
	}

	public void setSourceRelease(String sourceRelease) {
		ontology.setSourceRelease(sourceRelease);
	}

	public void setSourceUri(String sourceUri) {
		ontology.setSourceUri(sourceUri);
	}

	public void setStatus() {
		ontology.setStatus();
	}

	public void setStatus(Status status) {
		ontology.setStatus(status);
	}	
		
	public Collection<Term> getTerms() {
		return terms;
	}

	public void setTerms(Collection<Term> terms) {
		this.terms = filter(terms);
	}
	
	public int hashCode() {
		return ontology.hashCode();
	}
	
	public boolean equals(Object obj) {
		return ontology.equals(obj);
	}
	
	public String toString() {
		return ontology.toString();
	}
	
	private static Collection<Term> filter(Collection<Term> terms) {
		Collection<Term> valid = new ArrayList<Term>();
		for(Term term : terms) {
			if(term.getStatus().equals(Status.APPROVED) || term.getStatus().equals(Status.OBSOLETE)) {
				filter(term.getSynonyms());
				filter(term.getRelationships());
				
				for(Synonym synonym : term.getSynonyms()) {
					if(synonym.getControlledVocabularyTerm() != null) {
		        		ControlledVocabularyTerm ctrldVocabTerm = synonym.getControlledVocabularyTerm();
		        		Datasource datasource = ctrldVocabTerm.getControlledVocabulary().getDatasource();
		        		String refId = ctrldVocabTerm.getReferenceId();
		        		
		        		synonym.setControlledVocabularyTerm(null);
		        		synonym.setDatasource(datasource);
		        		synonym.setReferenceId(refId);
		        	}
				}
				valid.add(term);
			}
    	}
		Collections.sort((List<Term>)valid, new TermReferenceIdComparator());
		return valid;
	}
	
	private static <T extends VersionedEntity> void filter(Set<T> entities) {
		Set<T> invalid = new HashSet<T>();
    	for(T entity : entities) {
    		if(!entity.getStatus().equals(Status.APPROVED)) {
    			invalid.add(entity);
    		}
    	}
    	entities.removeAll(invalid);
	}
}
