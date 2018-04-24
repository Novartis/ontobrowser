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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.coode.owlapi.obo.parser.OBOVocabulary;

import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.entity.util.UrlParser;
import com.novartis.pcs.ontology.service.parser.InvalidFormatException;

public class DefTagHandler extends OBOTagHandler {		
	public DefTagHandler(OBOParseContext context) {
		super(OBOVocabulary.DEF, context);
	}

	@Override
	void handleTagValue(String tag, String value, 
			String qualifierBlock, String comment) {
		
		MutableInt fromIndex = new MutableInt();
		String definition = substr(value, '"', '"', fromIndex); 
		
		if(definition == null) {
			throw new InvalidFormatException("Invalid OBO def tag (missing quotes): " + value);
		}
		
		definition = context.unescapeTagValue(definition);
		
		VersionedEntity entity = context.getCurrentEntity();
		if(entity instanceof Term) {
			Term term = (Term)entity;
			term.setDefinition(definition);
			String xrefList = substr(value, '[', ']', fromIndex);
			if(xrefList != null) {
				List<String> xrefs = split(xrefList, ',');
				for(String xref : xrefs) {
					int colon = indexOf(xref, ':', 0);
					int quote = indexOf(xref, '"', colon+1);
					
					String datasource = null;
					String refId = null;
					if(colon != -1) {	
						datasource = xref.substring(0, colon);
						refId = quote == -1 ? xref.substring(colon+1) : xref.substring(colon+1, quote);					
					} else {
						datasource = xref;
					}		
					
					datasource = StringUtils.trimToNull(datasource);
					refId = StringUtils.trimToNull(refId);
					
					if(datasource == null) {
						throw new InvalidFormatException("Invalid OBO xref (no datasource): " + xref);
					}
					
					datasource = context.unescapeTagValue(datasource);
					if(refId != null) {
						refId = context.unescapeTagValue(refId);	
					}
					
					CrossReference defXref = null;
					if(UrlParser.isValidProtocol(datasource)) {
						if(refId == null) {
							throw new InvalidFormatException("Invalid xref URL: " + xref);
						}							
						String url = datasource + ":" + refId;
						defXref = addCrossReference(url, true);
					} else {
						defXref = addCrossReference(datasource, refId, true);
					}
					
					if(defXref != null && quote != -1) {
						fromIndex.setValue(quote);
						String description = substr(xref, '"', '"', fromIndex);
						description = context.unescapeTagValue(description);
						defXref.setDescription(description);
					}
				}
			}
		} else if(entity instanceof RelationshipType) {
			RelationshipType relationshipType = (RelationshipType)entity;
			relationshipType.setDefintion(definition);
		}
	}
}
