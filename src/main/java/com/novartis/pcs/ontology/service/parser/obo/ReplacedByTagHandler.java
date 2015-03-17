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

import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.ReplaceableEntity;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity;

public class ReplacedByTagHandler extends OBOTagHandler {
	
	public ReplacedByTagHandler(OBOParseContext context) {
		super(OBOVocabulary.REPLACED_BY, context);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void handleTagValue(String tag, String value, 
			String qualifierBlock, String comment) {
		VersionedEntity entity = context.getCurrentEntity();
		if(entity instanceof ReplaceableEntity) {
			ReplaceableEntity replaceable = (ReplaceableEntity)entity;
			String refId = context.unescapeTagValue(value);
			ReplaceableEntity<?> replacement = null;
			if(entity instanceof Term) {
				replacement = context.getTerm(refId);
			} else if(entity instanceof RelationshipType) {
				replacement = context.getRelationshipType(refId);
			}
			if(replacement != null 
					&& !replaceable.equals(replaceable.getReplacedBy())) {
				replaceable.setReplacedBy(replacement);
			}
		}
	}
}
