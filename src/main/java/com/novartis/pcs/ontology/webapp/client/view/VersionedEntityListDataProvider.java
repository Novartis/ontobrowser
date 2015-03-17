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
package com.novartis.pcs.ontology.webapp.client.view;

import com.google.gwt.view.client.ProvidesKey;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity;

public class VersionedEntityListDataProvider<T extends VersionedEntity> extends
		EntityListDataProvider<T> {
		
	public VersionedEntityListDataProvider() {
		super();
	}
	
	public VersionedEntityListDataProvider(
			ProvidesKey<T> keyProvider) {
		super(keyProvider);
	}

		
	protected boolean filter(T entity) {
		Curator curator = entity.getCreatedBy();	
		
		if(curator.getUsername().toLowerCase().contains(filter)) {
			return false;
		} else if(entity instanceof Synonym) {
			Synonym synonym = (Synonym)entity;
			Term term = synonym.getTerm();
			Ontology ontology = term.getOntology();
			Datasource datasource = synonym.getDatasource();
			
			if(datasource == null && synonym.getControlledVocabularyTerm() != null) {
				datasource = synonym.getControlledVocabularyTerm()
						.getControlledVocabulary()
						.getDatasource();
			}
			
			if(synonym.getSynonym().toLowerCase().contains(filter)
					|| term.getName().toLowerCase().contains(filter)
					|| ontology.getName().toLowerCase().contains(filter)
					|| (datasource != null 
							&& datasource.getAcronym().toLowerCase().contains(filter))) {  
				return false;  
			}
		} else if(entity instanceof Relationship) {
			Relationship relationship = (Relationship)entity;
			Term term = relationship.getTerm();
			Term relatedTerm = relationship.getRelatedTerm();
			Ontology ontology = term.getOntology();
			
			if(term.getName().toLowerCase().contains(filter)
					|| relatedTerm.getName().toLowerCase().contains(filter)
					|| ontology.getName().toLowerCase().contains(filter)) {  
				return false;  
			}
		} else if(entity instanceof Term) {
			Term term = (Term)entity;
			Ontology ontology = term.getOntology();
			
			if(term.getName().toLowerCase().contains(filter)
					|| ontology.getName().toLowerCase().contains(filter)) {  
				return false;  
			}
		}
		
		return true;
	}
}
