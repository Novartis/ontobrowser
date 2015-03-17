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
package com.novartis.pcs.ontology.service.search.result;

import java.io.Serializable;


@SuppressWarnings("serial")
public class SearchResult implements Serializable, Comparable<SearchResult> {
	protected String ontology;
	protected String referenceId;
	protected String term;
	protected float score;
	protected boolean synonym;
	
	protected SearchResult() {
		
	}
	
	public SearchResult(String ontology, String referenceId, String term, float score) {
		this(ontology, referenceId, term, score, false);
	}
	
	public SearchResult(String ontology, String referenceId, String term, float score, boolean synonym) {
		this.ontology = ontology;
		this.referenceId = referenceId;
		this.term = term;
		this.score = score;
		this.synonym = synonym;
	}
	
	public String getOntology() {
		return ontology;
	}
	
	public String getReferenceId() {
		return referenceId;
	}
			
	public String getTerm() {
		return term;
	}
	
		
	public boolean isSynonym() {
		return synonym;
	}

	public float getScore() {
		return score;
	}

	@Override
	public int compareTo(SearchResult result) {
		return Math.round(result.score - score);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == this)
			return true;
		if(obj != null && this.getClass() == obj.getClass()) {
			SearchResult result = (SearchResult) obj;
			return referenceId.equals(result.referenceId)
					&& term.equals(result.term);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return referenceId.hashCode();
	}

	@Override
	public String toString() {
		return referenceId;
	}
}
