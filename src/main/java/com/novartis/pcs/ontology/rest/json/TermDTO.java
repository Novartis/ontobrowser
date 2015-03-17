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

import java.net.URL;

import javax.naming.InitialContext;

import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Term;

public class TermDTO {
	private String ontology;
	private String termId;
	private String termName;
	private String termUrl;
	private String termStatus;
		
	private static final URL baseURL;
	
	static {
		InitialContext context = null;
		URL url = null;		
		try {
			context = new InitialContext();
			url = (URL)context.lookup("java:global/ontobrowser/url");
		} catch(Throwable t) {
			url = null;
		} finally {
			if(context != null) {
				try {
					context.close();
				} catch(Throwable t) {
					
				}
			}
		}
		baseURL = url;
	}
	
	public TermDTO(Term term) {
		Ontology ontology = term.getOntology();
				
		this.ontology = ontology.getName();
		this.termId = term.getReferenceId();
		this.termName = term.getName();
		this.termUrl = baseURL != null ? 
				baseURL.toString() + "index.html#" + term.getReferenceId() : null;
		this.termStatus = term.getStatus().toString();
	}

	public String getOntology() {
		return ontology;
	}

	public String getTermId() {
		return termId;
	}

	public String getTermName() {
		return termName;
	}

	public String getTermUrl() {
		return termUrl;
	}

	public String getTermStatus() {
		return termStatus;
	}
}
