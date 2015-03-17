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
package com.novartis.pcs.ontology.service.export;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.novartis.pcs.ontology.entity.Relationship;

public class RelationshipComparator implements Comparator<Relationship> {

	private static List<String> order = Arrays.asList("is_a",
		"union_of",
		"disjoint_from");
	
	@Override
	public int compare(Relationship r1, Relationship r2) {
		if(r1.isIntersection() != r2.isIntersection()) {
			if(r2.isIntersection()) {
				return orderOf(r1) == 0 ? -1 : 1;
			} else {
				return orderOf(r2) == 0 ? 1 : -1;
			}
		} else {
			int order1 = orderOf(r1);
			int order2 = orderOf(r2);
			return order1 - order2;
		}
	}
	
	private int orderOf(Relationship r) {
		int i = order.indexOf(r.getType().getRelationship());
		return i < 0 ? order.size() : i;
	}
}
