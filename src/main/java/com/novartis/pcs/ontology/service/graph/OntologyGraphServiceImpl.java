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
package com.novartis.pcs.ontology.service.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityNotFoundException;

import com.novartis.pcs.ontology.dao.RelationshipDAOLocal;
import com.novartis.pcs.ontology.dao.TermDAOLocal;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.util.StatusChecker;
import com.novartis.pcs.ontology.service.util.TermNameComparator;

@Stateless
@Local(OntologyGraphServiceLocal.class)
@Remote(OntologyGraphServiceRemote.class)
public class OntologyGraphServiceImpl implements OntologyGraphServiceRemote, OntologyGraphServiceLocal {
	private Logger logger = Logger.getLogger(getClass().getName()); 
	
	@EJB
	protected TermDAOLocal termDAO;
	
	@EJB
	private RelationshipDAOLocal relationshipDAO;
	
	@EJB
	private DOTProcessLocal process;
		
    /**
     * Default constructor. 
     */
    public OntologyGraphServiceImpl() {
    }
    		
	@Override
	public String createGraph(String termRefId) {
		return createGraph(termRefId, GraphOrientation.TB);
	}
	
	@Override
	public String createGraph(String termRefId, GraphOrientation orientation) {
		try {
			Term term = termDAO.loadByReferenceId(termRefId);
			String dot = createDOT(term, orientation);
			return process.submit(dot);
		} catch(EntityNotFoundException e) {
			String msg = "Failed to create graph for term " 
					+ termRefId 
					+ " because it does not exist";
			logger.log(Level.WARNING, msg);
			throw e;
		} catch(Exception e) {
			String msg = "Failed to create graph for term: " + termRefId;
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		}
	}
	
	private String createDOT(Term term, GraphOrientation orientation) throws IOException {
		StringBuilder dot = new StringBuilder(2048);
		Collection<Relationship> hierarchy = relationshipDAO.loadHierarchy(term.getId());
			
		Collection<Term> terms = new LinkedHashSet<Term>();
		terms.add(term); // for case where term is the top of the tree and hierarchy is empty		
		for (Relationship relationship : hierarchy) {
			if(StatusChecker.isValid(relationship) 
					&& StatusChecker.isValid(relationship.getTerm())
					&& StatusChecker.isValid(relationship.getRelatedTerm())) {
				terms.add(relationship.getTerm());
				terms.add(relationship.getRelatedTerm());
			}
		}
				
		dot.append("digraph \"").append(escape(term.getName())).append("\" {\n");
		dot.append("\tgraph [rankdir=")
			.append(orientation.name())
			.append(", ranksep=0.3, pad=0.4, margin=0];\n");
		dot.append("\tnode [id=\"\\N\", fontname=\"Tahoma\", fontsize=\"8\"")
			.append(", fontcolor=\"#333333\", penwidth=\"2\", style=\"filled\"")
			.append(", color=\"#333333\", fillcolor=\"#E3E3E3\"];\n");	
		dot.append("\tedge [id=\"\\T-\\H\", dir=\"back\", penwidth=\"2\"];\n");
		
		terms = new ArrayList<Term>(terms);
		Collections.sort((List<Term>)terms, new TermNameComparator());
		for (Term t : terms) {						
			dot.append("\t\"").append(escape(t.getReferenceId()))
					.append("\" [URL=\"#").append(t.getReferenceId())
					.append("\", label=\"").append(escape(t.getName())).append("\"");
			
			if(t.equals(term)) {
				dot.append(", fillcolor=\"#91B9CE\"");
			}
			dot.append("];\n");
		}
		
		int invisibleCount = 0;
		for (Relationship relationship : hierarchy) {
			if(StatusChecker.isValid(relationship) 
					&& StatusChecker.isValid(relationship.getTerm())
					&& StatusChecker.isValid(relationship.getRelatedTerm())) {
				String type = relationship.getType().getRelationship();
				String srcId = escape(relationship.getRelatedTerm().getReferenceId());
				String destId = escape(relationship.getTerm().getReferenceId());
				String color = relationship.getType().getEdgeColour();
				if(color == null) {
					color = "#000000";
				}
				
				dot.append("\t\"")
					.append(srcId).append("\" -> \"").append(destId)
					.append("\" [edgetooltip=\"").append(escape(type))
					.append("\", color=\"").append(color).append("\"];\n");
				
				if(term.equals(relationship.getRelatedTerm()) && !relationship.isLeaf()) {
					String grandChildColour = "#000000";				
					String grandChildId = "invisible" + (++invisibleCount);
					
					dot.append("\t\"").append(grandChildId)
						.append("\" [shape=\"point\", style=\"invis\"];\n");
					
					dot.append("\t\"")
						.append(destId).append("\" -> \"").append(grandChildId)
						.append("\" [edgetooltip=\"").append("...")
						.append("\", color=\"").append(grandChildColour).append("\"];\n");
				}
			}
		}
		
		dot.append("}\n");
		
		return dot.toString();
	}
	
	private String escape(String s) {
		return s.replace("\"", "\\\"");
	}
}
