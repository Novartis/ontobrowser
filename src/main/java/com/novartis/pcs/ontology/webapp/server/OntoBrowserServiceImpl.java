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
package com.novartis.pcs.ontology.webapp.server;


import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.novartis.pcs.ontology.entity.ControlledVocabulary;
import com.novartis.pcs.ontology.entity.ControlledVocabularyContext;
import com.novartis.pcs.ontology.entity.ControlledVocabularyDomain;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTermLink;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorAction;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.DuplicateEntityException;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Synonym.Type;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.service.OntologyCuratorServiceLocal;
import com.novartis.pcs.ontology.service.OntologySynonymServiceLocal;
import com.novartis.pcs.ontology.service.OntologyTermServiceLocal;
import com.novartis.pcs.ontology.service.graph.OntologyGraphServiceLocal;
import com.novartis.pcs.ontology.service.search.OntologySearchServiceLocal;
import com.novartis.pcs.ontology.service.search.result.HTMLSearchResult;
import com.novartis.pcs.ontology.service.search.result.InvalidQuerySyntaxException;
import com.novartis.pcs.ontology.service.util.TermNameComparator;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserService;

/**
 * The server side implementation of the RPC service.
 */

@WebServlet("/ontobrowser/service")
@SuppressWarnings("serial")
public class OntoBrowserServiceImpl extends RemoteServiceServlet implements
		OntoBrowserService {
	private Logger logger = Logger.getLogger(getClass().getName());
	
	@EJB
	private OntologyCuratorServiceLocal curatorService;
	
	@EJB
	private OntologyTermServiceLocal termService;
	
	@EJB
	private OntologySynonymServiceLocal synonymService;
	
	@EJB
	private OntologySearchServiceLocal searchService;
	
	@EJB
	private OntologyGraphServiceLocal graphService;
		
	private String getUsername() {
		HttpServletRequest request = getThreadLocalRequest();
		String username = request.getRemoteUser();
		
		if(username == null) {
			Principal principal = request.getUserPrincipal();
			if(principal != null) {
				username = principal.getName();
			}
		}
		
		//return username;
		return "RAVAGCA1";
	}
	
	@Override
	public Curator loadCurrentCurator() {
		Curator curator = null;
		String username = getUsername();
		
		logger.info("Loading curator: " + username);
		
		if(username != null) {
			curator = curatorService.loadByUsername(username);
		}
				
		return curator;
	}
	
	@Override
	public List<Term> loadRootTerms() {
		List<Term> terms = asList(termService.loadRoots());
		Collections.sort(terms, new Comparator<Term>() {
			@Override
			public int compare(Term t1, Term t2) {
				return t1.getOntology().getName().compareToIgnoreCase(
						t2.getOntology().getName());
			}
		});
		return terms;
	}
	
	@Override
	public List<Term> loadOntologyTerms(String ontology) {
		List<Term> terms = asList(termService.loadAll(ontology));
		Collections.sort(terms, new TermNameComparator());
		return terms;
	}

	@Override
	public Term loadTerm(String referenceId) {
		return termService.loadByReferenceId(referenceId);
	}
		
	@Override
	public String loadSVG(String referenceId) {
		return graphService.createGraph(referenceId);
	}
	
	@Override
	public List<HTMLSearchResult> search(String pattern, boolean includeSynonyms) throws InvalidQuerySyntaxException {
		return searchService.search(pattern, includeSynonyms);
	}

	@Override
	public List<RelationshipType> loadAllRelationshipTypes() {
		return asList(termService.loadAllRelationshipTypes());
	}
	
	@Override
	public List<Term> loadLastCreatedTerms(int max) {
		return asList(termService.loadLastCreated(max));
	}

	@Override
	public Term createChildTerm(String ontologyName, String termName,
			String definition, String url, String comments,
			String relatedTermRefId, String relationshipType,
			String datasourceAcronym, String referenceId,
			List<ControlledVocabularyTerm> synonyms,
			Synonym.Type synonymType) 
					throws DuplicateEntityException, InvalidEntityException {
		String username = getUsername();
		
		logger.info(username + " creating term: " + termName);
		
		return termService.createTerm(ontologyName, termName, 
				definition, url, comments,
				relatedTermRefId, relationshipType,
				datasourceAcronym, referenceId,
				synonyms, synonymType, username);
	}

	@Override
	public Term addSynonym(String termRefId, String synonym, Type type,
			String datasource, String referenceId) 
			throws DuplicateEntityException, InvalidEntityException {
		String username = getUsername();
		
		logger.info(username + " adding synonym: " + synonym + " for " + termRefId);
		
		return termService.addSynonym(termRefId, synonym, type, datasource, referenceId, username);
	}
		
	@Override
	public Term addSynonyms(String termRefId, 
			Collection<ControlledVocabularyTerm> terms,
			Synonym.Type type) 
			throws DuplicateEntityException, InvalidEntityException {
		String username = getUsername();
		
		logger.info(username + " adding synonyms from vocab for " + termRefId);
		
		return termService.addSynonyms(termRefId, terms, type, username);
	}
	
	@Override
	public Term addRelationship(String termRefId, String relatedTermRefId,
			String relationship) throws DuplicateEntityException, InvalidEntityException {
		String username = getUsername();
		
		logger.info(username + " adding relationship: " 
				+ termRefId + " " + relationship + " " + relatedTermRefId);
		
		return termService.addRelationship(termRefId, relatedTermRefId, relationship, username);
	}
	
	@Override
	public List<Datasource> loadPublicDatasources() {
		return asList(synonymService.loadPublicDatasources());
	}

	@Override
	public List<ControlledVocabulary> loadControlledVocabularies() {
		return asList(synonymService.loadControlledVocabulariesWithUnmappedTerms());
	}
	
	@Override
	public List<ControlledVocabularyTerm> loadControlledVocabularyTerms(
			ControlledVocabularyDomain domain) {
		return asList(synonymService.loadUnmappedControlledVocabularyTerms(domain));
	}

	@Override
	public List<ControlledVocabularyTerm> loadControlledVocabularyTerms(
			ControlledVocabularyDomain domain, ControlledVocabularyContext context) {
		return asList(synonymService.loadUnmappedControlledVocabularyTerms(
				domain, context));
	}
	
	@Override
	public List<ControlledVocabularyTerm> loadControlledVocabularyTerms(
			ControlledVocabularyDomain domain, Datasource datasource) {
		return asList(synonymService.loadUnmappedControlledVocabularyTerms(
				domain, datasource));
	}

	@Override
	public List<ControlledVocabularyTerm> loadControlledVocabularyTerms(
			ControlledVocabularyDomain domain, 
			ControlledVocabularyContext context,
			Datasource datasource) {
		return asList(synonymService.loadUnmappedControlledVocabularyTerms(
				domain, context, datasource));
	}
	
	@Override
	public void excludeControlledVocabularyTerms(
			Set<ControlledVocabularyTerm> terms) throws InvalidEntityException {
		String username = getUsername();
		logger.info(username + " excluding " + terms.size() + " controlled vocab terms");
		synonymService.excludeUnmappedControlledVocabularyTerms(terms, username);
	}
	
	@Override
	public List<ControlledVocabularyTermLink> loadControlledVocabularyTermLinks(
			ControlledVocabularyTerm term) {
		return asList(synonymService.loadControlledVocabularyTermLinks(term));
	}

	@Override
	public List<Term> loadPendingTerms() {
		return asList(curatorService.loadPendingTerms());
	}

	@Override
	public List<Synonym> loadPendingSynonyms() {
		return asList(curatorService.loadPendingSynonyms());
	}

	@Override
	public List<Relationship> loadPendingRelationships() {
		return asList(curatorService.loadPendingRelationships());
	}

	@Override
	public <T extends VersionedEntity> Set<T> approve(Set<T> pending,
			String comments) throws InvalidEntityException {
		String username = getUsername();
		logger.info(username + " approving " + pending.size() + " items"); 
		return curatorService.approve(pending, comments, username);
	}

	@Override
	public <T extends VersionedEntity> Set<T> reject(Set<T> pending, 
			String comments) throws InvalidEntityException {
		String username = getUsername();
		logger.info(username + " rejecting " + pending.size() + " items");
		
		return curatorService.reject(pending, comments, username);
	}
		
	@Override
	public Term updateTerm(long termId, String definition, String url,
			String comments) throws InvalidEntityException {
		return termService.updateTerm(termId, definition, url, comments, getUsername());
	}

	@Override
	public Synonym updateSynonym(long synonymId, Type type) 
			throws InvalidEntityException {
		return termService.updateSynonym(synonymId, type, getUsername());
	}

	@Override
	public Relationship updateRelationship(long relationshipId,
			String relationship) throws DuplicateEntityException, InvalidEntityException {
		return termService.updateRelationship(relationshipId, relationship, getUsername());
	}
	
	@Override
	public Term obsoleteTerm(long termId, long replacementTermId,
			String comments) throws InvalidEntityException {
		return curatorService.obsoleteTerm(termId, replacementTermId, comments, getUsername());
	}
	
	@Override
	public Synonym obsoleteSynonym(long synonymId, long replacementSynonymId,
			String comments) throws InvalidEntityException {
		return curatorService.obsoleteSynonym(synonymId, replacementSynonymId, comments, getUsername());
	}

	@Override
	public Relationship obsoleteRelationship(long relationshipId,
			long replacementRelationshipId, String comments)
			throws InvalidEntityException {
		return curatorService.obsoleteRelationship(relationshipId, replacementRelationshipId, comments, getUsername());
	}

	@Override
	public List<CuratorAction> loadCuratorActions() {
		return asList(curatorService.loadCuratorActions());
	}
	
	@Override
	public <T extends VersionedEntity> void delete(T entity)
			throws InvalidEntityException {
		String username = getUsername();
		logger.info(username + " deleteing " 
				+ entity.getClass().getSimpleName() + ": " + entity.toString());
				
		if(entity instanceof Synonym) {
			termService.deleteSynonym(entity.getId(), username);
		} else if(entity instanceof Relationship) {
			termService.deleteRelationship(entity.getId(), username);
		} else if(entity instanceof Term) {
			termService.deleteTerm(entity.getId(), username);
		}
	}
	
	@Override
	public void changePassword(String oldPassword, String newPassword) throws InvalidEntityException {
		String username = getUsername();
		if(username != null) {
			logger.info(username + " changing password");
			curatorService.changePassword(username, oldPassword, newPassword);
		}
	}

	private <T> List<T> asList(Collection<T> collection) {
		if(collection instanceof List<?>) {
			return (List<T>)collection;
		}
		return new ArrayList<T>(collection);
	}
	
}

