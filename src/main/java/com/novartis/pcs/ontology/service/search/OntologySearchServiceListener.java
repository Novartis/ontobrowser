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

import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;

public class OntologySearchServiceListener {
	@EJB
	OntologySearchServiceLocal service;
	
	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception {
		Object obj = ctx.proceed();
		if(obj instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>)obj;
			for(Object o : collection) {
				update(o);
			}
		} else {
			update(obj);
		}
		
		return obj;
	}
	
	private void update(Object obj) {
		Term term = null;
		if(obj instanceof Term) {
			term = (Term)obj;
		} else if(obj instanceof Synonym) {
			Synonym synonym = (Synonym)obj;
			term = synonym.getTerm();
		}
		
		if(term != null) {
			service.update(term);
		}
	}
}
