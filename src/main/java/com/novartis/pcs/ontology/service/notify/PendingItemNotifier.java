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
package com.novartis.pcs.ontology.service.notify;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import com.novartis.pcs.ontology.dao.RelationshipDAOLocal;
import com.novartis.pcs.ontology.dao.SynonymDAOLocal;
import com.novartis.pcs.ontology.dao.TermDAOLocal;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;

@Stateless
@Local(PendingItemNotifierLocal.class)
public class PendingItemNotifier implements PendingItemNotifierLocal {
		
	private static Logger logger = Logger.getLogger(
			PendingItemNotifier.class.getName());
			
	@EJB
	protected TermDAOLocal termDAO;
				
	@EJB
	protected SynonymDAOLocal synonymDAO;
			
	@EJB
	protected RelationshipDAOLocal relationshipDAO;

	@EJB
	private EmailSenderLocal emailSender;
	
	public void notifyCurators() {
		try {
			EnumSet<Status> pending = EnumSet.of(Status.PENDING);
						
			List<Term> terms = termDAO.loadByStatus(pending);
			List<Relationship> relationships = relationshipDAO.loadByStatus(pending);
			List<Synonym> synonyms = synonymDAO.loadByStatus(pending);
												
			if(!terms.isEmpty() || !relationships.isEmpty() || !synonyms.isEmpty()) {
				EnumSet<Entity> entityTypes = EnumSet.noneOf(Entity.class);
				String subject = "Approval Required";
				StringBuilder body = new StringBuilder();
								
				body.append("The following ontology items require approval (or rejection).");
				
				if(!terms.isEmpty()) {
					entityTypes.add(Entity.TERM);
					body.append("\n\nTerms:");
					for(Term term : terms) {
						body.append("\n\t")
							.append(term.getName())
							.append(" (")
							.append(term.getOntology().getName())
							.append(")");
					}
				}
				
				if(!relationships.isEmpty()) {
					entityTypes.add(Entity.TERM_RELATIONSHIP);
					body.append("\n\nRelationships:");
					for(Relationship relationship : relationships) {
						body.append("\n\t")
							.append(relationship.getTerm().getName())
							.append(" ").append(relationship.getType().getRelationship()).append(" ")
							.append(relationship.getRelatedTerm().getName())
							.append(" (")
							.append(relationship.getTerm().getOntology().getName())
							.append(")");
					}
				}
				
				if(!synonyms.isEmpty()) {
					char[] arrow = {' ', 0x2192, ' '};
					entityTypes.add(Entity.TERM_SYNONYM);
					body.append("\n\nSynonyms:");
					for(Synonym synonym : synonyms) {
						body.append("\n\t")
							.append(synonym.getSynonym())
							.append(arrow)
							.append(synonym.getTerm().getName())
							.append(" (")
							.append(synonym.getTerm().getOntology().getName())
							.append(")");
					}
				}
				
				CuratorCriteria criteria = new AllAuthorisedCuratorsCriteria(entityTypes);	
				emailSender.send(subject, body.toString(), criteria);
			} else {
				logger.info("No pending items, notification email not sent");
			}
			
		} catch (Exception e) {
			String msg = "Failed to send email notification";
			logger.log(Level.SEVERE, msg, e);
			throw new RuntimeException(msg, e);
		}
	}
}

