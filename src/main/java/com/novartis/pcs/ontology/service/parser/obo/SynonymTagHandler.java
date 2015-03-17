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

import java.util.List;

import org.apache.commons.lang.mutable.MutableInt;
import org.coode.owlapi.obo.parser.OBOVocabulary;

import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.util.UrlParser;
import com.novartis.pcs.ontology.service.parser.InvalidFormatException;

public class SynonymTagHandler extends OBOTagHandler {		
	
	public SynonymTagHandler(OBOParseContext context) {
		super(OBOVocabulary.SYNONYM, context);
	}

	@Override
	void handleTagValue(String tag, String value, 
			String qualifierBlock, String comment) {
		MutableInt fromIndex = new MutableInt();
		String synonym = substr(value, '"', '"', fromIndex); 
		
		if(synonym == null) {
			throw new InvalidFormatException("Invalid OBO synonym tag (missing quotes): " + value);
		}
		
		synonym = context.unescapeTagValue(synonym);
		Synonym.Type type = parseType(value, fromIndex);
						
		String xrefList = substr(value, '[', ']', fromIndex);
		if(xrefList != null && xrefList.length() > 0) {
			List<String> xrefs = split(xrefList, ',');
			for(String xref : xrefs) {
				int colon = indexOf(xref, ':', 0);
				if(colon == -1) {
					throw new InvalidFormatException("Invalid OBO synonym xref (no datasource): " + xref);
				}
				String datasource = xref.substring(0,colon);
				int quote = indexOf(xref, '"', colon+1);
				String refId = quote == -1 ? xref.substring(colon+1) : xref.substring(colon+1, quote);
				
				datasource = context.unescapeTagValue(datasource);
				refId = context.unescapeTagValue(refId);
				
				Synonym synonymObj = null; 
								
				if(UrlParser.isValidProtocol(datasource)) {
					String url = datasource + ":" + refId;
					synonymObj = context.addSynonym(synonym, type, url);					
				} else {
					synonymObj = context.addSynonym(synonym, type, datasource, refId);
				}
				
				if(quote != -1) {
					fromIndex.setValue(quote);
					String description = substr(xref, '"', '"', fromIndex);
					description = context.unescapeTagValue(description);
					synonymObj.setDescription(description);
				}
			}
		} else {
			context.addSynonym(synonym, type);
		}
	}
	
	private Synonym.Type parseType(String value, MutableInt fromIndex) {
		Synonym.Type type = Synonym.Type.RELATED;
		
		while(fromIndex.intValue() < value.length() 
				&& Character.isWhitespace(value.charAt(fromIndex.intValue()))) {
			fromIndex.increment();
		}
		
		int start = fromIndex.intValue();
		
		while(fromIndex.intValue() < value.length() 
				&& Character.isLetter(value.charAt(fromIndex.intValue()))) {
			fromIndex.increment();
		}
		
		if(start < fromIndex.intValue()) {
			String scope = value.substring(start, fromIndex.intValue());
			try {
				type = Synonym.Type.valueOf(scope.toUpperCase());
			} catch(IllegalArgumentException e) {
				type = Synonym.Type.RELATED;
				fromIndex.setValue(start);
			}
		}
				
		return type;
	}
	
	
}
