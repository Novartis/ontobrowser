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
package com.novartis.pcs.ontology.entity.util;

public class UrlParser {
	
	@SuppressWarnings("unused")
	public static String parse(String spec) {
		int i, limit, c;
		int start = 0;

		String protocol = null;
		String authority = null;
		String userInfo = null;
		String host = null;
		int port = -1;
		String path = null;
		String query = null;
		String ref = null;

		boolean isRelPath = false;
		boolean queryOnly = false;

		try {
			limit = spec.length();
			while ((limit > 0) && (spec.charAt(limit - 1) <= ' ')) {
				limit--;	//eliminate trailing whitespace
			}
			while ((start < limit) && (spec.charAt(start) <= ' ')) {
				start++;	// eliminate leading whitespace
			}

			for (i = start ; (i < limit) &&
			((c = spec.charAt(i)) != '/') ; i++) {
				if (c == ':') {
					String s = spec.substring(start, i).toLowerCase();
					if (isValidProtocol(s)) {
						protocol = s;
						start = i + 1;
					}
					break;
				}
			}

			if (protocol == null) {
				throw new IllegalArgumentException("Invalid URL protocol");
			}

			i = spec.indexOf('#', start);
			if (i >= 0) {
				ref = spec.substring(i + 1, limit);
				limit = i;
			}

			// FIX: should not assume query if opaque
			// Strip off the query part
			if (start < limit) {
				int queryStart = spec.indexOf('?');
				queryOnly = queryStart == start;
				if ((queryStart != -1) && (queryStart < limit)) {
					query = spec.substring(queryStart+1, limit);
					if (limit > queryStart)
						limit = queryStart;
					spec = spec.substring(0, queryStart);
				}
			}

			// Parse the authority part if any
			boolean isUNCName = (start <= limit - 4) &&
			(spec.charAt(start) == '/') &&
			(spec.charAt(start + 1) == '/') &&
			(spec.charAt(start + 2) == '/') &&
			(spec.charAt(start + 3) == '/');
			if (!isUNCName && (start <= limit - 2) && (spec.charAt(start) == '/') && 
					(spec.charAt(start + 1) == '/')) {
				start += 2;
				i = spec.indexOf('/', start);
				if (i < 0) {
					i = spec.indexOf('?', start);
					if (i < 0)
						i = limit;
				}

				host = authority = spec.substring(start, i);

				int ind = authority.indexOf('@');
				if (ind != -1) {
					userInfo = authority.substring(0, ind);
					host = authority.substring(ind+1);
				} else {
					userInfo = null;
				}
				if (host != null) {
					ind = host.indexOf(':');
					port = -1;
					if (ind >= 0) {
						// port can be null according to RFC2396
						if (host.length() > (ind + 1)) {
							port = Integer.parseInt(host.substring(ind + 1));
						}
						host = host.substring(0, ind);
					}
				} else {
					host = "";
				}
				if (port < -1)
					throw new IllegalArgumentException("Invalid port number :" +
							port);
				start = i;
				// If the authority is defined then the path is defined by the
				// spec only; See RFC 2396 Section 5.2.4.
				if (authority != null && authority.length() > 0)
					path = "";
			} 

			if (host == null) {
				host = "";
			}

			// Parse the file path if any
			if (start < limit) {
				if (spec.charAt(start) == '/') {
					path = spec.substring(start, limit);
				} else if (path != null && path.length() > 0) {
					isRelPath = true;
					int ind = path.lastIndexOf('/');
					String seperator = "";
					if (ind == -1 && authority != null)
						seperator = "/";
					path = path.substring(0, ind + 1) + seperator +
					spec.substring(start, limit);

				} else {
					String seperator = (authority != null) ? "/" : "";
					path = seperator + spec.substring(start, limit);
				}
			} else if (queryOnly && path != null) {
				int ind = path.lastIndexOf('/');
				if (ind < 0)
					ind = 0;
				path = path.substring(0, ind) + "/";
			}
			if (path == null)
				path = "";

			if (isRelPath) {
				// Remove embedded /./
				while ((i = path.indexOf("/./")) >= 0) {
					path = path.substring(0, i) + path.substring(i + 2);
				}
				// Remove embedded /../ if possible
				i = 0;
				while ((i = path.indexOf("/../", i)) >= 0) {
					/* 
					 * A "/../" will cancel the previous segment and itself, 
					 * unless that segment is a "/../" itself
					 * i.e. "/a/b/../c" becomes "/a/c"
					 * but "/../../a" should stay unchanged
					 */
					if (i > 0 && (limit = path.lastIndexOf('/', i - 1)) >= 0 &&
							(path.indexOf("/../", limit) != 0)) {
						path = path.substring(0, limit) + path.substring(i + 3);
						i = 0;
					} else {
						i = i + 3;
					}
				}
				// Remove trailing .. if possible
				while (path.endsWith("/..")) {
					i = path.indexOf("/..");
					if ((limit = path.lastIndexOf('/', i - 1)) >= 0) {
						path = path.substring(0, limit+1);
					} else {
						break;
					}
				}
				// Remove starting .
				if (path.startsWith("./") && path.length() > 2)
					path = path.substring(2);

				// Remove trailing .
				if (path.endsWith("/."))
					path = path.substring(0, path.length() -1);
			}

			if(host == null || host.length() == 0) {
				throw new IllegalArgumentException("Invalid URL: no host specifed");
			}

			if(isRelPath) {
				throw new IllegalArgumentException("Invalid URL: relative path");
			}

			// pre-compute length of StringBuffer
			int len = protocol.length() + 1;
			if (authority != null && authority.length() > 0)
				len += 2 + authority.length();
			if (path != null && path.length() > 0) {
				len += path.length();
			}
			if (query != null && query.length() > 0) {
				len += 1 + query.length();
			}
			if (ref != null && ref.length() > 0) 
				len += 1 + ref.length();

			StringBuilder result = new StringBuilder(len);
			result.append(protocol);
			result.append(":");
			if (authority != null && authority.length() > 0) {
				result.append("//");
				result.append(authority);
			}
			if (path != null && path.length() > 0) {
				result.append(path);
			}
			if (query != null && query.length() > 0) {
				result.append('?');
				result.append(query);
			}
			if (ref != null && ref.length() > 0) {
				result.append("#");
				result.append(ref);
			}
			return result.toString();
		} catch(IllegalArgumentException e) {
			throw e;
		} catch(Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public static boolean isValidProtocol(String protocol) {
		return protocol.equals("http") || protocol.equals("https");
	}
}
