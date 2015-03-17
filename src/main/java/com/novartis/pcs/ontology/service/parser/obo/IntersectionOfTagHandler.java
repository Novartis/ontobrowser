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

public class IntersectionOfTagHandler extends OBOTagHandler {
	
	public IntersectionOfTagHandler(OBOParseContext context) {
		super(OBOVocabulary.INTERSECTION_OF, context);
	}

	@Override
	void handleTagValue(String tag, String value, 
			String qualifierBlock, String comment) {
		String type = OBOVocabulary.IS_A.getName();
		String refId = null;
		
		int space = indexOf(value, ' ', 0);
		if(space != -1) {
			type = context.unescapeTagValue(value.substring(0, space));
			refId = context.unescapeTagValue(value.substring(space+1)); 
		} else {
			refId = context.unescapeTagValue(value);
		} 
		
		if(refId == null || refId.length() == 0) {
			throw new InvalidFormatException("Invalid OBO intersection_of tag (missing id): " + value);
		}
		
		context.addRelationship(type, refId, true); 
	}
}
