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

import org.apache.commons.lang.mutable.MutableInt;
import org.coode.owlapi.obo.parser.OBOVocabulary;

import com.novartis.pcs.ontology.entity.CrossReference;
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
		if(colon == -1) {
			throw new InvalidFormatException("Invalid OBO xref (no datasource): " + value);
		}
		String datasourceAcronym = value.substring(0,colon);
		int quote = indexOf(value, '"', colon+1);
		String refId = quote == -1 ? value.substring(colon+1) : value.substring(colon+1, quote);
		
		datasourceAcronym = context.unescapeTagValue(datasourceAcronym);
		refId = context.unescapeTagValue(refId);
		CrossReference xref = null;
		
		if(UrlParser.isValidProtocol(datasourceAcronym)) {
			String url = datasourceAcronym + ":" + refId;
			xref = addCrossReference(url, false);
		} else {
			xref = addCrossReference(datasourceAcronym, refId, false);
		}
				
		if(quote != -1) {
			MutableInt fromIndex = new MutableInt(quote);
			String description = substr(value, '"', '"', fromIndex);
			description = context.unescapeTagValue(description);
			xref.setDescription(description);
		}
	}
}
