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
package com.novartis.pcs.ontology.service.search;

import java.util.Collection;
import java.util.List;

import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.search.result.HTMLSearchResult;
import com.novartis.pcs.ontology.service.search.result.InvalidQuerySyntaxException;

public interface OntologySearchService {
	
	public List<HTMLSearchResult> search(String pattern, boolean includeSynonyms)
			throws InvalidQuerySyntaxException;
	
	public void update(Term term);
	
	public void update(Collection<Term> term);
	
	public void delete(Term term);
}
