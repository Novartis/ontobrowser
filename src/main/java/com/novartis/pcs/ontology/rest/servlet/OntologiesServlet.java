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

import static com.novartis.pcs.ontology.service.export.OntologyFormat.Manchester;
import static com.novartis.pcs.ontology.service.export.OntologyFormat.OBO;
import static com.novartis.pcs.ontology.service.export.OntologyFormat.OWLXML;
import static com.novartis.pcs.ontology.service.export.OntologyFormat.RDFXML;
import static com.novartis.pcs.ontology.service.export.OntologyFormat.Turtle;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novartis.pcs.ontology.dao.OntologyDAOLocal;
import com.novartis.pcs.ontology.dao.TermDAOLocal;
import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.DuplicateEntityException;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.rest.json.CrossReferenceMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.DatasourceMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.OntologyDelegate;
import com.novartis.pcs.ontology.rest.json.OntologyMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.RelationshipMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.RelationshipTypeMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.SynonymMixInAnnotations;
import com.novartis.pcs.ontology.rest.json.TermMixInAnnotations;
import com.novartis.pcs.ontology.service.OntologyCuratorServiceLocal;
import com.novartis.pcs.ontology.service.export.OntologyExportServiceLocal;
import com.novartis.pcs.ontology.service.export.OntologyFormat;
import com.novartis.pcs.ontology.service.export.OntologyNotFoundException;
import com.novartis.pcs.ontology.service.importer.OntologyImportServiceLocal;
import com.novartis.pcs.ontology.service.parser.InvalidFormatException;

/**
 * A simple REST service for ontologies. Produces OBO, OWL and JSON formats
 * 
 * Note: Could use JAX-RS (e.g. RESTEasy or Jersey) but this RESTful
 * service is so trivial that it does not warrant a framework.
 * 
 * @author Carlo Ravagli
 *
 */
@WebServlet("/ontologies/*")
@SuppressWarnings("serial")
public class OntologiesServlet extends HttpServlet {
	private static final String MEDIA_TYPE_JSON = "application/json";
	private static final String MEDIA_TYPE_OBO = "application/obo";
	private static final Map<String,OntologyFormat> mediaTypes;
	
	static {
        mediaTypes = new LinkedHashMap<String,OntologyFormat>();
        // a registered media type does not exist for OBO
        mediaTypes.put("text/xml", RDFXML);
        mediaTypes.put("application/xml", RDFXML);
        mediaTypes.put("application/rdf+xml", RDFXML);
        mediaTypes.put("application/owl+xml",OWLXML);
        mediaTypes.put("text/owl-manchester", Manchester);
        mediaTypes.put("text/turtle", Turtle);
        mediaTypes.put("text/obo", OBO);
        mediaTypes.put("text/plain", OBO);
        mediaTypes.put(MEDIA_TYPE_OBO, OBO);
        mediaTypes.put(MEDIA_TYPE_JSON, null);
    }
			
	@EJB
	private OntologyExportServiceLocal exportService;
	
	@EJB
	private OntologyImportServiceLocal importService;
	
	@EJB
	private OntologyCuratorServiceLocal curatorService;
	
	@EJB
	protected OntologyDAOLocal ontologyDAO;
	
	@EJB
	protected TermDAOLocal termDAO;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public void init() throws ServletException {
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
		boolean includeNonPublicXrefs = Boolean.parseBoolean(
				StringUtils.trimToNull(request.getParameter("nonpublic-xrefs")));
		if(mediaType == null) {
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			response.setContentLength(0);
		} else if(pathInfo != null && pathInfo.length() > 1) {	
			String ontologyName = pathInfo.substring(1);
			if(mediaType.equals(MEDIA_TYPE_JSON)) {
				serialize(ontologyName, response);
			} else {
				export(ontologyName, includeNonPublicXrefs, mediaType, response);
			}
		} else {
			mediaType = getExpectedMediaType(request, 
					Collections.singletonList(MEDIA_TYPE_JSON));
			if(mediaType.equals(MEDIA_TYPE_JSON)) {
				serializeAll(response);
			} else {
				response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
				response.setContentLength(0);
			}
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
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String mediaType = StringUtils.trimToNull(request.getContentType());
		String encoding = StringUtils.trimToNull(request.getCharacterEncoding());	
		String pathInfo = StringUtils.trimToNull(request.getPathInfo());
		Curator curator = loadCurator(request);
		
		if(mediaType != null && mediaType.indexOf(';') > 0) {
			mediaType = mediaType.substring(0, mediaType.indexOf(';'));
		}
		
		if(!StringUtils.equalsIgnoreCase(mediaType,MEDIA_TYPE_OBO)
				|| !StringUtils.equalsIgnoreCase(encoding,"utf-8")) {
			log("Failed to import ontology: invalid media type or encoding " 
					+ mediaType + ";charset=" + encoding);
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		} else if(pathInfo == null || pathInfo.length() <= 1) {
			log("Failed to import ontology: ontology name not include in path");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} else if(curator == null) {
			log("Failed to import ontology: curator not found in request");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} else {
			try {
				String ontologyName = pathInfo.substring(1);
				importService.importOntology(ontologyName, request.getInputStream(), curator);
				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setHeader("Cache-Control", "public, max-age=0");
			} catch(DuplicateEntityException e) {
				log("Failed to import ontology: duplicate term", e);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			} catch(InvalidEntityException e) {
				log("Failed to import ontology: invalid entity", e);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			} catch(InvalidFormatException e) {
				log("Failed to import ontology: invalid format", e);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			} catch(Exception e) {
				log("Failed to import ontology: system error", e);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
		response.setContentLength(0);
	}
	
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String mediaType = getExpectedMediaType(request);
		
		OntologyFormat format = mediaTypes.get(mediaType);
		if(format != null) {
			// Preflight CORS support
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setHeader("Access-Control-Allow-Methods", "GET,PUT");
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
		return getExpectedMediaType(request, mediaTypes.keySet());
	}
	
	private String getExpectedMediaType(HttpServletRequest request, 
			Collection<String> acceptedMediaTypes) {
		String mediaType = null;
		String acceptHeader = request.getHeader("Accept");
		if(acceptHeader != null) {
			mediaType = StringUtils.trimToNull(MIMEParse.bestMatch(acceptedMediaTypes, acceptHeader));
		}
		
		return mediaType;
	}
	
	private void serializeAll(HttpServletResponse response) {
		try {
			List<Ontology> all = ontologyDAO.loadByStatus(EnumSet.of(Status.APPROVED));
			List<Ontology> ontologies = new ArrayList<Ontology>();
			
			for(Ontology ontology : all) {
				if(!ontology.isCodelist()) {
					ontologies.add(ontology);
				}
			}
			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setContentType(MEDIA_TYPE_JSON + ";charset=utf-8");
			response.setHeader("Cache-Control", "public, max-age=0");
			
			// As per jackson javadocs - Encoding will be UTF-8
			mapper.writeValue(response.getOutputStream(), ontologies);
			
		} catch (Exception e) {
			log("Failed to serialize ontologies to JSON", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentLength(0);
		}
	}
	
	private void serialize(String ontologyName, HttpServletResponse response) {
		try {
			Ontology ontology = ontologyDAO.loadByName(ontologyName);
			if(ontology == null || ontology.isCodelist()) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentLength(0);
			} else {
				Collection<Term> terms = termDAO.loadAll(ontology);
				OntologyDelegate delegate = new OntologyDelegate(ontology, terms);
								
				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setContentType(MEDIA_TYPE_JSON + ";charset=utf-8");
				response.setHeader("Cache-Control", "public, max-age=0");
				
				// As per jackson javadocs - Encoding will be UTF-8
				mapper.writeValue(response.getOutputStream(), delegate);
			}
		} catch (Exception e) {
			log("Failed to serialize ontology to JSON", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentLength(0);
		}
	}
		
	private void export(String ontologyName, boolean includeNonPublicXrefs, String mediaType,
			HttpServletResponse response) {
		OntologyFormat format = mediaTypes.get(mediaType);
		if(format != null) {
			try {				
				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setContentType(mediaType + ";charset=utf-8");
				response.setHeader("Cache-Control", "public, max-age=0");
				
				exportService.exportOntology(ontologyName, response.getOutputStream(), format,
						includeNonPublicXrefs);
			} catch(OntologyNotFoundException e) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentLength(0);
			} catch(Exception e) {
				log("Failed to export ontology", e);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentLength(0);
			}
		} else {
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			response.setContentLength(0);
		}
	}
	
	private String getUsername(HttpServletRequest request) {
		String username = request.getRemoteUser();
		
		if(username == null) {
			Principal principal = request.getUserPrincipal();
			if(principal != null) {
				username = principal.getName();
			}
		}
		
		return username;
	}
	
	public Curator loadCurator(HttpServletRequest request) {
		Curator curator = null;
		String username = getUsername(request);
						
		if(username != null) {
			curator = curatorService.loadByUsername(username);
		}
				
		return curator;
	}
	
}
