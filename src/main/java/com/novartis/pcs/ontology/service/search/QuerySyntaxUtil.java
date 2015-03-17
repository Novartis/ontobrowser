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
package com.novartis.pcs.ontology.service.search;

public class QuerySyntaxUtil {
	
	// escaping that supports phrase queries
	public static String escapeQueryPattern(String s) {
		StringBuilder escaped = new StringBuilder(s.length());
		int leadingQuoteIndex = -1;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			
			if(c == '"') {
				if(leadingQuoteIndex >= 0) { 
					if(escaped.charAt(escaped.length()-1) == ' ') {
						escaped.deleteCharAt(escaped.length()-1);
						i--;
					}
					
					if(leadingQuoteIndex == escaped.length()-1) {
						escaped.deleteCharAt(leadingQuoteIndex);
						i--;
						leadingQuoteIndex = -1;
						continue;
					}
				} 
				leadingQuoteIndex = leadingQuoteIndex >= 0 ? -1 : escaped.length();
				escaped.append(c);
				continue;
			}
			
			
			if(TermNameTokenizer.isNotTokenChar(c)) {
				if(escaped.length() > 0 
						&& escaped.charAt(escaped.length()-1) != ' '
						&& escaped.charAt(escaped.length()-1) != '"') {
					escaped.append(" ");
				}
			} else {
				switch (c) {
				case '+':
				case '-':
				case '!':
				case '(':
				case ')':
				case '{':
				case '}':
				case '[':
				case ']':
				case '^':
				case '~':
				case '*':
				case '?':
				case ':':
				case '&':
				case '|':
				case '\\':
					escaped.append('\\');
				default:
					escaped.append(c);
				}
			}
		}
		
		if(escaped.length() > 0 && escaped.charAt(escaped.length()-1) == ' ') {
			escaped.deleteCharAt(escaped.length()-1);
		}
		
		if(leadingQuoteIndex >= 0) {
			escaped.deleteCharAt(leadingQuoteIndex);
		}
		
		leadingQuoteIndex = escaped.indexOf("\"");
		if(leadingQuoteIndex >= 0) {
			escapePhrase(escaped, leadingQuoteIndex);
		}
		
		return escaped.toString();
	}
	
	private static void escapePhrase(StringBuilder s, int leadingQuoteIndex) {
		while(leadingQuoteIndex >= 0) {
			int trailingQuoteIndex = s.indexOf("\"", leadingQuoteIndex+1);
			if(trailingQuoteIndex > leadingQuoteIndex) {
				int spaceIndex = s.indexOf(" ",leadingQuoteIndex+1);
				if(spaceIndex > leadingQuoteIndex && spaceIndex < trailingQuoteIndex) {
					int wordLen = 0;
					for(int i = leadingQuoteIndex+1; i < trailingQuoteIndex; i++) {
						char c = s.charAt(i);
						
						//remove escaping added above
						if(c == '\\') {
							s.deleteCharAt(i);
							i--;
							trailingQuoteIndex--;
							continue;
						}
												
						if(TermNameTokenizer.isNotTokenChar(c)) {
							if(wordLen < EdgeNGramTokenFilter.DEFAULT_MIN_GRAM_SIZE) {
								s.delete(i-wordLen, i+1);
								i -= wordLen + 1;
								trailingQuoteIndex -= wordLen + 1;
							} else {
								s.setCharAt(i, ' ');
							}
							
							wordLen = 0;
						} else {
							wordLen++;
						}
					}
					
					if(wordLen < EdgeNGramTokenFilter.DEFAULT_MIN_GRAM_SIZE) {
						s.delete(trailingQuoteIndex-wordLen, trailingQuoteIndex);
						trailingQuoteIndex -= wordLen;
						
						if(s.charAt(trailingQuoteIndex-1) == ' ') {
							s.deleteCharAt(trailingQuoteIndex-1);
							trailingQuoteIndex--;
						}
					}
					
					leadingQuoteIndex = trailingQuoteIndex+1 < s.length() ?
							s.indexOf("\"", trailingQuoteIndex+1) : -1;
				} else { // not a phrase
					s.deleteCharAt(trailingQuoteIndex);
					s.deleteCharAt(leadingQuoteIndex);
					leadingQuoteIndex = trailingQuoteIndex-1 < s.length() ?
							s.indexOf("\"", trailingQuoteIndex-1) : -1;
				}
			} else {
				s.deleteCharAt(leadingQuoteIndex);
				break;
			}
		}
	}
}
