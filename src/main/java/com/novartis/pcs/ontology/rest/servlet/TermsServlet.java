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
package com.novartis.pcs.ontology.rest.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.persistence.EntityNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.novartis.pcs.ontology.dao.TermDAOLocal;
import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.rest.json.CrossReferenceMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.DatasourceMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.OntologyMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.RelationshipMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.RelationshipTypeMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.SynonymMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.TermMixInAnnotations;
import com.novartis.pcs.ontology.service.search.OntologySearchServiceLocal;
import com.novartis.pcs.ontology.service.search.result.HTMLSearchResult;

/**
 * A simple REST service for synonyms. Produces JSON only.
 * 
 * Note: Could use JAX-RS (e.g. RESTEasy or Jersey) but this RESTful
 * service is so trivial that it does not warrant a framework.
 * 
 * @author Carlo Ravagli
 *
 */
@WebServlet("/terms/*")
@SuppressWarnings("serial")
public class TermsServlet extends HttpServlet {
	private static final String MEDIA_TYPE_JSON = "application/json";
	
	@EJB
	protected OntologySearchServiceLocal searchService;
	
	@EJB
	protected TermDAOLocal termDAO;
		
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public void init() throws ServletException {
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
    	mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    	mapper.addMixInAnnotations(Datasource.class, DatasourceMixInAnnotations.class);    	
    	mapper.addMixInAnnotations(RelationshipType.class, RelationshipTypeMixInAnnotations.class);
    	mapper.addMixInAnnotations(Ontology.class, OntologyMixInAnnotations.class);
    	mapper.addMixInAnnotations(Term.class, TermMixInAnnotations.class);
    	mapper.addMixInAnnotations(Synonym.class, SynonymMixInAnnotations.class);
    	mapper.addMixInAnnotations(Relationship.class, RelationshipMixInAnnotations.class);
    	mapper.addMixInAnnotations(CrossReference.class, CrossReferenceMixInAnnotations.class);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String mediaType = getExpectedMediaType(request);
		String pathInfo = StringUtils.trimToNull(request.getPathInfo());
		if(mediaType == null || !MEDIA_TYPE_JSON.equals(mediaType)) {
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			response.setContentLength(0);
		} else if(pathInfo != null && pathInfo.length() > 1) {	
			String referenceId = pathInfo.substring(1);
			serialize(referenceId, response);
		} else {
			// perform search using query string params
			String name = StringUtils.trimToNull(request.getParameter("name"));
			String ontology = StringUtils.trimToNull(request.getParameter("ontology"));
			boolean includeSynonyms = Boolean.parseBoolean(
					request.getParameter("synonyms"));
			int maxResults = Integer.MAX_VALUE;
			
			try {
				maxResults = Integer.parseInt(request.getParameter("max"));
				if(maxResults <= 0) {
					maxResults = Integer.MAX_VALUE;
				}
			} catch(Exception e) {
				maxResults = Integer.MAX_VALUE;
			}
			
			serialize(name, ontology, includeSynonyms, maxResults, response);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		resp.setContentLength(0);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		resp.setContentLength(0);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		resp.setContentLength(0);
	}
	
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String mediaType = getExpectedMediaType(request);
				
		if(MEDIA_TYPE_JSON.equals(mediaType)) {
			// Preflight CORS support
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setHeader("Access-Control-Allow-Methods", "GET");
			response.setIntHeader("Access-Control-Max-Age", 60*60*24);
			response.setContentType(mediaType + ";charset=utf-8");
			response.setContentLength(0);
		} else {
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			response.setContentLength(0);
		}
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		return System.currentTimeMillis();
	}
	
	private String getExpectedMediaType(HttpServletRequest request) {
		String mediaType = null;
		String acceptHeader = request.getHeader("Accept");
		if(acceptHeader != null) {
			mediaType = StringUtils.trimToNull(
					MIMEParse.bestMatch(Collections.singleton(MEDIA_TYPE_JSON), acceptHeader));
		}
		
		return mediaType;
	}
		
	private void serialize(String referenceId, HttpServletResponse response) {
		try {
			Term term = termDAO.loadByReferenceId(referenceId, true);
						
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setContentType(MEDIA_TYPE_JSON + ";charset=utf-8");
			response.setHeader("Cache-Control", "public, max-age=0");
			
			// As per jackson javadocs - Encoding will be UTF-8
			mapper.writeValue(response.getOutputStream(), term);
		} catch (EntityNotFoundException e) {
			log("Failed to find term with reference id: " + referenceId, e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentLength(0);
		} catch (Exception e) {
			log("Failed to serialize term to JSON", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentLength(0);
		}
	}
	
	private void serialize(String name, String ontology, boolean synonyms, int max, 
			HttpServletResponse response) {
		try {
			List<HTMLSearchResult> results = Collections.emptyList();
						
			if(name != null) {
				results = searchService.search(name, synonyms);				
			}
			
			if(!results.isEmpty() && (ontology != null || max < Integer.MAX_VALUE)) {
				List<HTMLSearchResult> filtered = new ArrayList<HTMLSearchResult>(results.size());
				for(HTMLSearchResult result : results) {
					if(ontology.equalsIgnoreCase(result.getOntology())) {
						filtered.add(result);
						if(filtered.size() == max) {
							break;
						}
					}
				}
				results = filtered;
			}
				
			if(!results.isEmpty()) {				
				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setContentType(MEDIA_TYPE_JSON + ";charset=utf-8");
				response.setHeader("Cache-Control", "public, max-age=0");
				
				// As per jackson javadocs - Encoding will be UTF-8
				mapper.writeValue(response.getOutputStream(), results);
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentLength(0);
			}
		} catch (Exception e) {
			log("Failed to serialize search results to JSON", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentLength(0);
		}
	}
}
