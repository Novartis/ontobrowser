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

import org.coode.owlapi.obo.parser.OBOVocabulary;

import com.novartis.pcs.ontology.service.parser.InvalidFormatException;

public class RelationshipTagHandler extends OBOTagHandler {
	
	public RelationshipTagHandler(OBOParseContext context) {
		super(OBOVocabulary.RELATIONSHIP, context);
	}

	@Override
	void handleTagValue(String tag, String value, 
			String qualifierBlock, String comment) {
		int space = indexOf(value, ' ', 0);
		if(space == -1) {
			throw new InvalidFormatException("Invalid OBO relationship tag (missing relationship): " + value);
		}
		String type = context.unescapeTagValue(value.substring(0, space));
		String refId = context.unescapeTagValue(value.substring(space+1));
		
		if(refId == null || refId.length() == 0) {
			throw new InvalidFormatException("Invalid OBO relationship tag (missing id): " + value);
		}
		
		context.addRelationship(type, refId, false); 
	}
}
