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
package com.novartis.pcs.ontology.service.parser.obo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.coode.owlapi.obo.parser.OBOVocabulary;

import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.parser.InvalidFormatException;


abstract class OBOTagHandler {
	protected final OBOVocabulary tagName;
	protected final OBOParseContext context;
	
	protected OBOTagHandler(OBOVocabulary tagName, OBOParseContext context) {
		this.tagName = tagName;
		this.context = context;
	}

	OBOVocabulary getTagName() {
		return tagName;
	}
		
	abstract void handleTagValue(String tag, String value, 
    		String qualifierBlock, String comment);
	
	protected int indexOf(String value, char ch, int fromIndex) {
		boolean inQuote = false;
		for(int i = fromIndex; i < value.length(); i++) {
			char c = value.charAt(i);
			
			// skip escaped char
			if(c == '\\') {
				i++;
				continue;
			}
			
			if(ch != '"') {
				if(c == '"') {
					inQuote = !inQuote;
					continue;
				}
				
				// skip quoted char e.g. chars in xref description
				if(inQuote) {
					continue; 			
				}
			}
			
			if(c == ch) {
				return i;
			}
		}
		
		return -1;
	}
	
	protected String substr(String value, char startChar, char endChar, MutableInt fromIndex) {
		String substr = null;
		int start = indexOf(value, startChar, fromIndex.intValue());
		if(start != -1) {
			int end = indexOf(value, endChar, start+1);
			
			if(end == -1) {
				throw new InvalidFormatException("Invalid OBO " + tagName.getName() + " format: " + value);
			}
			substr = value.substring(start+1, end);
			fromIndex.setValue(end+1);
		}
		return substr;
	}
	
	protected List<String> split(String value, char splitChar) {
		List<String> list = new ArrayList<String>();
		int start = 0;
		while(start < value.length()) {
			int end = indexOf(value, splitChar, start);
			
			if(end == -1) {
				list.add(value.substring(start));
				break;
			}
			list.add(value.substring(start, end));
			start = end + 1;
		}
		
		return list;
	}
	
	protected CrossReference addCrossReference(String url, boolean definition) {
    	Term term = context.getCurrentTerm();
    	CrossReference xref = null;
    	
    	for(CrossReference existing : term.getCrossReferences()) {
    		if(existing.isDefinitionCrossReference() == definition
    				&& existing.getDatasource() == null
    				&& url.equals(existing.getUrl())) {
    			xref = existing;
    			break;
    		}
    	}
    	
    	if(xref == null) {
    		try {
				xref = new CrossReference(term, url, context.getCurator());
			} catch(IllegalArgumentException e) {
				throw new InvalidFormatException("Invalid OBO xref URL: " + url);
			}
    	} 
    	
    	xref.setDefinitionCrossReference(definition);
    	return xref;
    }
		
	protected CrossReference addCrossReference(String datasourceAcronym, String refId, boolean definition) {
		Term term = context.getCurrentTerm();
    	Datasource datasource = context.getDatasource(datasourceAcronym);
		
    	CrossReference xref = null;  	    	
    	for(CrossReference existing : term.getCrossReferences()) {
    		if(existing.isDefinitionCrossReference() == definition
    				&& existing.getUrl() == null
    				&& datasource.equals(existing.getDatasource())) {
    			if(!StringUtils.equals(refId, existing.getReferenceId())) {
    				existing.setReferenceId(refId);
    			}
    			xref = existing;
    			break;
    		}
    	}
    	
    	if(xref == null) {
    		xref = new CrossReference(term, datasource, refId, context.getCurator());
    	} 
    	
    	xref.setDefinitionCrossReference(definition);
     	return xref;
    }
}
