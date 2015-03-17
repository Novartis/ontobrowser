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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.novartis.pcs.ontology.entity.Term;

public class TermCell extends AbstractCell<Term> {
	
	public TermCell() {
		
	}

	@Override
	public void render(Context context, Term term, SafeHtmlBuilder sb) {
		if(term != null) {
			sb.appendHtmlConstant("<span class=\"codelist-term\">");
			sb.appendEscaped(term.getName());		
			sb.appendHtmlConstant("</span>");
		}
	}
}
