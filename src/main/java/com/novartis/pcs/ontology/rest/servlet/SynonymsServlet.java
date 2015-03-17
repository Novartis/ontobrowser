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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.novartis.pcs.ontology.dao.DatasourceDAOLocal;
import com.novartis.pcs.ontology.dao.SynonymDAOLocal;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.rest.json.SynonymDTO;

/**
 * A simple REST service for synonyms. Produces JSON only.
 * 
 * Note: Could use JAX-RS (e.g. RESTEasy or Jersey) but this RESTful
 * service is so trivial that it does not warrant a framework.
 * 
 * @author Carlo Ravagli
 *
 */

@WebServlet("/synonyms/*")
@SuppressWarnings("serial")
public class SynonymsServlet extends HttpServlet {
	private static final String MEDIA_TYPE_JSON = "application/json";		
	
	@EJB
	protected DatasourceDAOLocal datasourceDAO;
	
	@EJB
	protected SynonymDAOLocal synonymDAO;
	
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public void init() throws ServletException {
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
    	mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String mediaType = getExpectedMediaType(request);
		String pathInfo = StringUtils.trimToNull(request.getPathInfo());
		if(mediaType == null || !MEDIA_TYPE_JSON.equals(mediaType)) {
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			response.setContentLength(0);
		} else if(pathInfo == null || pathInfo.length() == 1) {	
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentLength(0);
		} else {
			String datasourceAcronym = null;
			String vocabRefId = null;
			boolean pending = Boolean.parseBoolean(
					request.getParameter("pending"));
			int i = pathInfo.indexOf('/', 1);
			if(i != -1) {
				datasourceAcronym = pathInfo.substring(1, i);
				vocabRefId = pathInfo.substring(i+1);
			} else {
				datasourceAcronym = pathInfo.substring(1);
			}
			serialize(datasourceAcronym, vocabRefId, pending, response);
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
		
	private void serialize(String datasourceAcronym, String vocabRefId, boolean pending, 
			HttpServletResponse response) {
		try {
			List<SynonymDTO> dtos = new ArrayList<SynonymDTO>(1024);
			Datasource datasource = datasourceDAO.loadByAcronym(datasourceAcronym);
			if(datasource != null) {			
				Collection<Synonym> synonyms = vocabRefId != null ? 
						synonymDAO.loadByCtrldVocabRefId(datasource, vocabRefId) : 
						synonymDAO.loadByDatasource(datasource);
				EnumSet<Status> statusSet = pending ? 
						EnumSet.of(Status.PENDING, Status.APPROVED) :
						EnumSet.of(Status.APPROVED);
				
				for(Synonym synonym : synonyms) {
					if(statusSet.contains(synonym.getStatus())) {
						dtos.add(new SynonymDTO(synonym));
					}
				}
			}
			
			if(!dtos.isEmpty()) {			
				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setContentType(MEDIA_TYPE_JSON + ";charset=utf-8");
				response.setHeader("Cache-Control", "public, max-age=0");
				
				// As per jackson javadocs - Encoding will be UTF-8
				mapper.writeValue(response.getOutputStream(), dtos);
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentLength(0);
			}
		} catch (Exception e) {
			log("Failed to serialize synonyms to JSON", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentLength(0);
		}
	}
}
