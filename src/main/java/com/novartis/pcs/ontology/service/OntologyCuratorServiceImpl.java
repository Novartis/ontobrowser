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
package com.novartis.pcs.ontology.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorAction;
import com.novartis.pcs.ontology.entity.CuratorAction.Action;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.InvalidPasswordException;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.Version;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.service.search.OntologySearchServiceListener;
import com.novartis.pcs.ontology.service.util.CuratorActionComparator;
import com.novartis.pcs.ontology.service.util.StatusChecker;

/**
 * Session Bean implementation class OntologyCuratorServiceImpl
 */
@Stateless
@Local(OntologyCuratorServiceLocal.class)
@Remote(OntologyCuratorServiceRemote.class)
public class OntologyCuratorServiceImpl extends OntologyService implements OntologyCuratorServiceRemote, OntologyCuratorServiceLocal {		
	/**
     * Default constructor. 
     */
    public OntologyCuratorServiceImpl() {
    }

	@Override
	public Curator loadByUsername(String username) {
		Curator curator = curatorDAO.loadByUsername(username);
				
		if(curator != null && curator.isActive()) {
			return curator;
		}
		return null;
	}
	
	@Override
	public Collection<Term> loadPendingTerms() {
		return termDAO.loadByStatus(EnumSet.of(Status.PENDING));
	}

	@Override
	public Collection<Synonym> loadPendingSynonyms() {
		return synonymDAO.loadByStatus(EnumSet.of(Status.PENDING));
	}

	@Override
	public Collection<Relationship> loadPendingRelationships() {
		return relationshipDAO.loadByStatus(EnumSet.of(Status.PENDING));
	}

	@Override
	public Relationship approveRelationship(long relationshipId, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Relationship relationship = relationshipDAO.load(relationshipId, true);
		Version version = lastUnpublishedVersion(curator);
				
		if(!relationship.getTerm().getStatus().equals(Status.APPROVED)) {
			throw new InvalidEntityException(relationship.getTerm(), 
					"Relationship child term has not yet been approved");
		}
		
		if(!relationship.getRelatedTerm().getStatus().equals(Status.APPROVED)) {
			throw new InvalidEntityException(relationship.getRelatedTerm(), 
					"Relationship parent term has not yet been approved");
		}
		
		createCuratorActionAndUpdateStatus(relationship, Action.APPROVE,
				Status.APPROVED, curator, comments, version);
		
		return relationship;
	}
	
	@Override
	public Synonym approveSynonym(long synonymId, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Synonym synonym = synonymDAO.load(synonymId, true);
		Version version = lastUnpublishedVersion(curator);
						
		if(!synonym.getTerm().getStatus().equals(Status.APPROVED)) {
			throw new InvalidEntityException(synonym.getTerm(), 
					"Term has not yet been approved");
		}
		
		createCuratorActionAndUpdateStatus(synonym, Action.APPROVE,
				Status.APPROVED, curator, comments, version);
		
		return synonym;
	}

	@Override
	public Term approveTerm(long termId, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Term term = termDAO.load(termId, true);
		Version version = lastUnpublishedVersion(curator);
		
		if(!term.getOntology().isCodelist()) {
			if(term.getRelationships() == null 
					|| StatusChecker.valid(term.getRelationships()).size() == 0) {
				throw new InvalidEntityException(term, "Cannot approve term with no valid relationships");
			}
		}
		
		createCuratorActionAndUpdateStatus(term, Action.APPROVE,
				Status.APPROVED, curator, comments, version);
				
		if(!term.getOntology().isCodelist()) {
			for(Relationship relationship : term.getRelationships()) {
				if(relationship.getStatus().equals(Status.PENDING) 
						&& relationship.getRelatedTerm().getStatus().equals(Status.APPROVED)) {
					createCuratorActionAndUpdateStatus(relationship, Action.APPROVE,
							Status.APPROVED, curator, comments, version);
				}
			}
		}
		
		return term;
	}
	
	@Override
	public Relationship rejectRelationship(long relationshipId, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Relationship relationship = relationshipDAO.load(relationshipId, true);
		Version version = lastUnpublishedVersion(curator);
		createCuratorActionAndUpdateStatus(relationship, Action.REJECT,
				Status.REJECTED, curator, comments, version);
		return relationship;
	}

	@Override
	@Interceptors({OntologySearchServiceListener.class})
	public Synonym rejectSynonym(long synonymId, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);		
		Synonym synonym = synonymDAO.load(synonymId, true);
		Version version = lastUnpublishedVersion(curator);
		createCuratorActionAndUpdateStatus(synonym, Action.REJECT,
				Status.REJECTED, curator, comments, version);
		return synonym;
	}

	@Override
	@Interceptors({OntologySearchServiceListener.class})
	public Term rejectTerm(long termId, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		Term term = termDAO.load(termId, true);
		Version version = lastUnpublishedVersion(curator);
		
		createCuratorActionAndUpdateStatus(term, Action.REJECT,
				Status.REJECTED, curator, comments, version);
		
		if(term.getStatus().equals(Status.REJECTED)) {
			for(Synonym synonym : term.getSynonyms()) {
				if(synonym.getStatus().equals(Status.PENDING)) {
					synonym.setStatus(Status.REJECTED);
				}
			}
			
			if(!term.getOntology().isCodelist()) {
				for(Relationship relationship : term.getRelationships()) {
					if(relationship.getStatus().equals(Status.PENDING)) {
						relationship.setStatus(Status.REJECTED);
					}
				}
				
				Collection<Relationship> descendents = relationshipDAO.loadByRelatedTermId(termId);
				for (Relationship relationship : descendents) {
					if(relationship.getStatus().equals(Status.PENDING)) {
						relationship.setStatus(Status.REJECTED);
					}
				}
			}
		}
		return term;
	}
	
	@Override
	@Interceptors({OntologySearchServiceListener.class})
	public Relationship obsoleteRelationship(long relationshipId, long replacementRelationshipId,
			String comments, String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		BigDecimal approvalWeight = curator.getEntityApprovalWeight(Entity.TERM_RELATIONSHIP);
		Relationship relationship = relationshipDAO.load(relationshipId);
		Version version = lastUnpublishedVersion(curator);
		Term term = relationship.getTerm();
		int count = 0;
		
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		if(!relationship.getStatus().equals(Status.APPROVED)) {
			throw new InvalidEntityException(relationship, "Relationship is not approved");
		}
		
		if(!approvalWeight.equals(BigDecimal.ONE)) {
			throw new InvalidEntityException(curator, 
					"Curator is not authorised to render relationships obsolete");
		}
		
		for(Relationship r : term.getRelationships()) {
			if(!r.equals(relationship) && r.getStatus().equals(Status.APPROVED)) {
				count++;
			}
		}
		
		if(count == 0) {
			throw new InvalidEntityException(term, 
					"Term requires at least one approved relationship");
		}
		
		relationship.setStatus(Status.OBSOLETE);
		relationship.setObsoleteVersion(version);
		if(replacementRelationshipId != 0L) {
			Relationship replacementRelationship = relationshipDAO.load(replacementRelationshipId);
			if(!replacementRelationship.getStatus().equals(Status.APPROVED)) {
				throw new InvalidEntityException(replacementRelationship, "Replacement relationship is not approved");
			}
			relationship.setReplacedBy(replacementRelationship);			
		}
						
		CuratorAction obsoleting = new CuratorAction(curator, Action.REPLACE, relationship); 
		obsoleting.setComments(
					comments != null && comments.trim().length() > 0 ? comments.trim() : null);
						
		return relationship;
	}
	
	@Override
	@Interceptors({OntologySearchServiceListener.class})
	public Synonym obsoleteSynonym(long synonymId, long replacementSynonymId, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		BigDecimal approvalWeight = curator.getEntityApprovalWeight(Entity.TERM_SYNONYM);
		Synonym synonym = synonymDAO.load(synonymId);
		Version version = lastUnpublishedVersion(curator);
				
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		if(!synonym.getStatus().equals(Status.APPROVED)) {
			throw new InvalidEntityException(synonym, "Synonym is not approved");
		}
		
		if(!approvalWeight.equals(BigDecimal.ONE)) {
			throw new InvalidEntityException(curator, 
					"Curator is not authorised to render synonyms obsolete");
		}
		
		synonym.setStatus(Status.OBSOLETE);
		synonym.setObsoleteVersion(version);
		if(replacementSynonymId != 0L) {
			Synonym replacementSynonym = synonymDAO.load(replacementSynonymId);
			if(!replacementSynonym.getStatus().equals(Status.APPROVED)) {
				throw new InvalidEntityException(replacementSynonym, "Replacement synonym is not approved");
			}
			synonym.setReplacedBy(replacementSynonym);			
		}
						
		CuratorAction obsoleting = new CuratorAction(curator, Action.REPLACE, synonym); 
		obsoleting.setComments(
					comments != null && comments.trim().length() > 0 ? comments.trim() : null);
						
		return synonym;
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	@Interceptors({OntologySearchServiceListener.class})
	public Term obsoleteTerm(long termId, long replacementTermId, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(curatorUsername);
		BigDecimal approvalWeight = curator.getEntityApprovalWeight(Entity.TERM);
		Term term = termDAO.load(termId);
		Version version = lastUnpublishedVersion(curator);
		
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
				
		if(!term.getStatus().equals(Status.APPROVED)) {
			throw new InvalidEntityException(term, "Term is not approved");
		}
		
		if(!approvalWeight.equals(BigDecimal.ONE)) {
			throw new InvalidEntityException(curator, "Curator is not authorised to render terms obsolete");
		}
		
		term.setStatus(Status.OBSOLETE);
		term.setObsoleteVersion(version);
		if(replacementTermId != 0L) {
			Term replacementTerm = termDAO.load(replacementTermId);
						
			if(replacementTerm == null) {
				throw new InvalidEntityException(replacementTerm, "Replacement term does not exist");
			}
			
			if(replacementTerm.equals(term)) {
				throw new InvalidEntityException(replacementTerm, "Cannot replace term with itself");
			}
			
			if(!replacementTerm.getStatus().equals(Status.APPROVED)) {
				throw new InvalidEntityException(replacementTerm, "Replacement term is not approved");
			}
			
			for(Synonym synonym : term.getSynonyms()) {
				if(StatusChecker.isValid(synonym)) {
					Synonym newSynonym = new Synonym(replacementTerm, synonym.getSynonym(), 
							synonym.getType(), curator, version);
					newSynonym.setStatus(synonym.getStatus());
					if(synonym.getStatus().equals(Status.APPROVED)) {
						newSynonym.setApprovedVersion(version);
					}
					newSynonym.setControlledVocabularyTerm(synonym.getControlledVocabularyTerm());
					newSynonym.setDatasource(synonym.getDatasource());
					newSynonym.setReferenceId(synonym.getReferenceId());
					newSynonym.setUrl(synonym.getUrl());
					synonymDAO.save(newSynonym);
					
					if(synonym.getStatus().equals(Status.APPROVED)) {
						// Status will get set to OBSOLETE below
						synonym.setReplacedBy(newSynonym);
					}
				}
			}
			
			
			Collection<Relationship> descendents = relationshipDAO.loadByRelatedTermId(termId);
			for (Relationship relationship : descendents) {
				if(StatusChecker.isValid(relationship)
						&& !relationship.getTerm().equals(replacementTerm)) {
					Relationship newRelationship = new Relationship(relationship.getTerm(),
							replacementTerm, relationship.getType(), curator, version);
					newRelationship.setStatus(relationship.getStatus());
					if(relationship.getStatus().equals(Status.APPROVED)) {
						newRelationship.setApprovedVersion(version);
					}
					newRelationship.setIntersection(relationship.isIntersection());
					relationshipDAO.save(newRelationship);
										
					if(relationship.getStatus().equals(Status.APPROVED)) {
						// Status will get set to OBSOLETE below
						relationship.setReplacedBy(newRelationship);
					}
				}
			}
			
			term.setReplacedBy(replacementTerm);			
		}
								
		CuratorAction obsoleting = new CuratorAction(curator, Action.REPLACE, term); 
		obsoleting.setComments(
					comments != null && comments.trim().length() > 0 ? comments.trim() : null);
		
		Collection<Synonym> synonyms = new ArrayList<Synonym>(term.getSynonyms());
		for(Synonym synonym : synonyms) {
			switch(synonym.getStatus()) {
			case PENDING:
				term.getSynonyms().remove(synonym);
				synonymDAO.delete(synonym);
				break;
			case APPROVED:
				synonym.setStatus(Status.OBSOLETE);
				synonym.setObsoleteVersion(version);
				break;
			}
		}
		
		Collection<Relationship> relationships = new ArrayList<Relationship>(term.getRelationships());
		for(Relationship relationship : relationships) {
			switch(relationship.getStatus()) {
			case PENDING:
				term.getRelationships().remove(relationship);
				relationshipDAO.delete(relationship);
				break;
			case APPROVED:
				relationship.setStatus(Status.OBSOLETE);
				relationship.setObsoleteVersion(version);
				break;
			}
		}
		
		Collection<Relationship> descendents = relationshipDAO.loadByRelatedTermId(termId);
		for (Relationship relationship : descendents) {
			switch(relationship.getStatus()) {
			case PENDING:
				Term childTerm = relationship.getTerm();
				childTerm.getRelationships().remove(relationship);
				relationshipDAO.delete(relationship);
				break;
			case APPROVED:
				relationship.setStatus(Status.OBSOLETE);
				relationship.setObsoleteVersion(version);
				break;
			}
		}
		
		return term;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends VersionedEntity> Set<T> approve(Set<T> pending, String comments, 
			String curatorUsername) throws InvalidEntityException {				
		Set<T> approved = new LinkedHashSet<T>();
		for(T entity : pending) {
			if(entity instanceof Synonym) {
				Synonym synonym = (Synonym)entity;
				approved.add((T)approveSynonym(synonym.getId(), comments, curatorUsername));
			} else if(entity instanceof Relationship) {
				Relationship relationship = (Relationship)entity;
				approved.add((T)approveRelationship(relationship.getId(), comments, curatorUsername));
			} else if(entity instanceof Term) {
				Term term = (Term)entity;
				approved.add((T)approveTerm(term.getId(), comments, curatorUsername));
			} else {
				throw new UnsupportedOperationException("Approving " 
						+ entity.getClass().getSimpleName()
						+ " has not been implemented");
			}
		}
		
		return approved;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends VersionedEntity> Set<T> reject(Set<T> pending, String comments, 
			String curatorUsername) throws InvalidEntityException {
		Set<T> rejected = new LinkedHashSet<T>();
		for(T entity : pending) {
			if(entity instanceof Synonym) {
				Synonym synonym = (Synonym)entity;
				rejected.add((T)rejectSynonym(synonym.getId(), comments, curatorUsername));
			} else if(entity instanceof Relationship) {
				Relationship relationship = (Relationship)entity;
				rejected.add((T)rejectRelationship(relationship.getId(), comments, curatorUsername));
			} else if(entity instanceof Term) {
				Term term = (Term)entity;
				rejected.add((T)rejectTerm(term.getId(), comments, curatorUsername));
			} else {
				throw new UnsupportedOperationException("Rejecting " 
						+ entity.getClass().getSimpleName()
						+ " has not been implemented");
			}
		}
		return rejected;
	}
	
	@Override
	public Collection<CuratorAction> loadCuratorActions() {
		List<CuratorAction> actions = curatorActionDAO.loadAll();
		Collections.sort(actions, new CuratorActionComparator());
		return actions;
	}
	
	@Override
	public void changePassword(String username, String oldPassword, String newPassword)
			throws InvalidEntityException {
		Curator curator = curatorDAO.loadByUsername(username);
		if(curator != null) {
			String oldPasswordEncrypted = encryptPassword(oldPassword);
			if(!oldPasswordEncrypted.equals(curator.getPassword())) {
				throw new InvalidPasswordException(curator, "Current password incorrect");
			}	
						
			curator.setPassword(encryptPassword(newPassword));
			curator.setPasswordExpired(false);
			curator.setModifiedBy(curator);
			curator.setModifiedDate(new Date());
		}
	}
	
	private String encryptPassword(String password) {
		try {
			MessageDigest digester = MessageDigest.getInstance("SHA1");
			Base64.Encoder encoder = Base64.getEncoder();
			return "{SHA}" + encoder.encode(digester.digest(password.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA1 not supported by JVM", e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 not supported by JVM", e);
		}
	}

	private void createCuratorActionAndUpdateStatus(VersionedEntity entity, 
			Action actionType, Status status, Curator curator, String comments, Version version) throws InvalidEntityException {
		Entity entityType = Entity.valueOf(entity);
		BigDecimal approvalWeight = curator.getEntityApprovalWeight(entityType);
		Collection<CuratorAction> actions = entity.getCuratorActions();
		CuratorAction newAction = null;
		
		comments = comments != null && comments.trim().length() > 0 ?
				comments.trim() : null;
		
		if(curator == null || !curator.isActive()) {
			throw new InvalidEntityException(curator, "Curator is invalid/inactive");
		}
		
		if(!entity.getStatus().equals(Status.PENDING)) {
			throw new InvalidEntityException(entity, 
					entity.getClass().getSimpleName() + " is not pending approval");
		}
						
		if(approvalWeight.equals(BigDecimal.ZERO)) {
			throw new InvalidEntityException(curator, 
					"Curator is not authorised to approve/reject " + entity.getClass().getSimpleName() + "s");
		}
		
		if(entity.getCreatedBy().equals(curator)) {
			throw new InvalidEntityException(curator, 
					"Curator cannot approve/reject a " + entity.getClass().getSimpleName() + "that s/he created");
		}
		
		BigDecimal sum = approvalWeight;
		for(CuratorAction action : actions) {
			if(action.getCurator().equals(curator)) { 
				if(action.getAction().equals(actionType)) {
					action.setComments(comments);
					return;
				} else {
					action.setAction(actionType);
					newAction = action;
				}
			} else if(action.getAction().equals(actionType)) {
				Curator actionCurator = action.getCurator();
				sum = sum.add(actionCurator.getEntityApprovalWeight(entityType));
			}
		}
		
		if(sum.compareTo(BigDecimal.ONE) >= 0) {
			entity.setStatus(status);
			if(status.equals(Status.APPROVED)) {
				entity.setApprovedVersion(version);
			}
		}
		
		if(newAction == null) {
			newAction = new CuratorAction(curator, actionType, entity);
		}
		newAction.setComments(comments);
	}
}
