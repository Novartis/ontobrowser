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

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Synonym;

@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="referenceId")
public interface TermMixInAnnotations extends VersionedEntityMixInAnnotations {		
	@JsonBackReference("terms") Ontology getOntology();
	
	@JsonBackReference("terms") void setOntology(Ontology ontology);
	
	@JsonManagedReference("synonyms") Set<Synonym> getSynonyms();
	
	@JsonManagedReference("synonyms") void setSynonyms(Set<Synonym> synonyms);
	
	@JsonManagedReference("relationships") Set<Relationship> getRelationships();
	
	@JsonManagedReference("relationships") void setRelationships(Set<Relationship> relationships);
			
	@JsonManagedReference("crossReferences") Set<CrossReference> getCrossReferences();
	
	@JsonManagedReference("crossReferences") void setCrossReferences(Set<CrossReference> crossReferences);
	
	@JsonInclude(Include.NON_DEFAULT) boolean isRoot();
}
