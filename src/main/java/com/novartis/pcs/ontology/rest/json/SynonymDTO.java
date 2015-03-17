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

import com.novartis.pcs.ontology.entity.ControlledVocabulary;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Synonym;

public class SynonymDTO extends TermDTO {
	private String synonymId;
	private String synonym;
	private String synonymType;
	private String synonymStatus;
	private String datasource;
	private String vocabulary;
		
	public SynonymDTO(Synonym synonym) {
		super(synonym.getTerm());
		ControlledVocabularyTerm vocabTerm = synonym.getControlledVocabularyTerm();
		ControlledVocabulary vocab = vocabTerm != null ? vocabTerm.getControlledVocabulary() : null;
		Datasource datasource = vocab != null ? vocab.getDatasource() : synonym.getDatasource();
		String referenceId = vocabTerm != null ? vocabTerm.getReferenceId() : synonym.getReferenceId();

		this.synonymId = referenceId;
		this.synonym = synonym.getSynonym();
		this.synonymType = synonym.getType().toString();
		this.synonymStatus = synonym.getStatus().toString();
		this.datasource = datasource != null ? datasource.getAcronym() : null;
		this.vocabulary = vocab != null ? vocab.getReferenceId() : null;
	}

	public String getSynonymId() {
		return synonymId;
	}

	public String getSynonym() {
		return synonym;
	}

	public String getSynonymType() {
		return synonymType;
	}

	public String getSynonymStatus() {
		return synonymStatus;
	}

	public String getDatasource() {
		return datasource;
	}

	public String getVocabulary() {
		return vocabulary;
	}
}
