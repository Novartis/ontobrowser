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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.coode.owlapi.obo.parser.OBOVocabulary;

import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.util.UrlParser;
import com.novartis.pcs.ontology.service.parser.InvalidFormatException;

public class XrefTagHandler extends OBOTagHandler {		
	public XrefTagHandler(OBOParseContext context) {
		super(OBOVocabulary.XREF, context);
	}

	@Override
	void handleTagValue(String tag, String value, 
			String qualifierBlock, String comment) {
		int colon = indexOf(value, ':', 0);
		int quote = indexOf(value, '"', colon+1);
		
		String datasource = null;
		String refId = null;
		if(colon != -1) {	
			datasource = value.substring(0, colon);
			refId = quote == -1 ? value.substring(colon+1) : value.substring(colon+1, quote);					
		} else {
			datasource = value;
		}		
		
		datasource = StringUtils.trimToNull(datasource);
		refId = StringUtils.trimToNull(refId);
		
		if(datasource == null) {
			throw new InvalidFormatException("Invalid OBO xref (no datasource): " + value);
		}
		
		datasource = context.unescapeTagValue(datasource);
		
		if(refId != null) {
			refId = context.unescapeTagValue(refId);	
		}						 
		
		CrossReference xref = null;
		if(UrlParser.isValidProtocol(datasource)) {
			if(refId == null) {
				throw new InvalidFormatException("Invalid xref URL: " + value);
			}
			
			String url = datasource + ":" + refId;
			xref = addCrossReference(url, false);
			
		} else {
			xref = addCrossReference(datasource, refId, false);
		}
		
		if(xref != null && quote != -1) {
			MutableInt fromIndex = new MutableInt(quote);
			String description = substr(value, '"', '"', fromIndex);
			description = context.unescapeTagValue(description);
			xref.setDescription(description);
		}
	}
}
