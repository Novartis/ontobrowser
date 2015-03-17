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
import java.util.Collections;

import javax.ejb.EJB;
import javax.persistence.EntityNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.novartis.pcs.ontology.service.graph.GraphOrientation;
import com.novartis.pcs.ontology.service.graph.OntologyGraphServiceLocal;

/**
 * A simple graph service
 * 
 * @author Carlo Ravagli
 *
 */
@WebServlet("/graphs/*")
@SuppressWarnings("serial")
public class GraphServlet extends HttpServlet {
	private static final String MEDIA_TYPE_SVG = "image/svg+xml";
			
	@EJB
	private OntologyGraphServiceLocal graphService;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {	
		String pathInfo = StringUtils.trimToNull(request.getPathInfo());
		String orientation = StringUtils.trimToNull(request.getParameter("orientation"));
		String callback = StringUtils.trimToNull(request.getParameter("callback"));
		String mediaType = getExpectedMediaType(request);
				
		if(pathInfo != null && pathInfo.length() > 1) {	
			String termRefId = pathInfo.substring(1);					
			graph(termRefId, mediaType, orientation, callback, response);
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentLength(0);
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
				
		if(mediaType != null) {
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
					MIMEParse.bestMatch(Collections.singleton(MEDIA_TYPE_SVG), acceptHeader));
		}
		
		return mediaType;
	}
			
	private void graph(String termRefId, String mediaType, String orientation, 
			String callback, HttpServletResponse response) {
		GraphOrientation graphOrientation = GraphOrientation.TB;
		
		if(orientation != null) {
			try {
				graphOrientation = GraphOrientation.valueOf(orientation);
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentLength(0);
				return;
			}
		}
		
		if(mediaType == null) {
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			response.setContentLength(0);
			return;
		}
			
		try {
			String content = graphService.createGraph(termRefId, graphOrientation);
			// JSONP support
			if(callback != null) {
				StringBuilder builder = new StringBuilder(
						callback.length() + content.length() + 5);
				builder.append(callback);
				builder.append("(\"");
				builder.append(StringEscapeUtils.escapeJavaScript(content));
				builder.append("\");");
				content = builder.toString();
				mediaType = "application/javascript";
			}
			
			byte[] contentBytes = content.getBytes("UTF-8");
			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setContentType(mediaType + ";charset=utf-8");
			response.setHeader("Cache-Control", "public, max-age=0");
			response.getOutputStream().write(contentBytes);
		} catch(EntityNotFoundException e) {
			log("Failed to find term with reference id: " + termRefId, e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentLength(0);
		} catch(Exception e) {
			log("Failed to create graph for term " + termRefId, e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentLength(0);
		} 
	}
}
