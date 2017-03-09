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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.apache.commons.io.IOUtils;

/**
 * Session Bean implementation class DOTProcessImpl
 */
@Stateless
@Local(DOTProcessLocal.class)
public class DOTProcessImpl implements DOTProcessLocal {
	private static final Charset CHARSET = Charset.forName("UTF-8");
	private static final String EOL = System.lineSeparator();
	private static final String DOT_END = "}" + EOL;
	private static final byte[] SVG_START = "<svg width=".getBytes(CHARSET);
	private static final byte[] SVG_END = ("</svg>" + EOL).getBytes(CHARSET);
	
		
	private Logger logger = Logger.getLogger(getClass().getName());
	
	@Resource(lookup="java:global/ontobrowser/dot/path")
	private String dotPath;
			
	private Process process;
	private byte[] buffer;
	private int count;
	private int[] skip;
		
    /**
     * Default constructor. 
     */
    public DOTProcessImpl() {

    }
    
    @PostConstruct
    protected void init() {
    	ProcessBuilder processBuilder = new ProcessBuilder(
				Arrays.asList(dotPath, "-Tsvg"));
		
		try {
			process = processBuilder.start();
			buffer = new byte[262144];
			count = 0;
			skip = new int[256];
		} catch (IOException e) {
			String msg = "Failed to start dot process";
			logger.log(Level.WARNING, msg, e);
			throw new RuntimeException(msg, e);
		}
	}
        
    @PreDestroy
    protected void destroy() {
    	OutputStream stdin = process.getOutputStream();
    	InputStream stdout = process.getInputStream();
    	InputStream stderr = process.getErrorStream();
    	try {
    		// Closing stdin (which sends EOF) causes the dot process to exit
    		logger.info("Closing stdin on dot process");
    		stdin.close();
    		
    		// On some OSs (e.g. OSX) closing the input stream
    		// (incorrectly) seems to re-send the stream's internal
    		// buffer contents. The causes the dot process to
    		// generate output on stdout which needs to be read
    		// so the process can exit cleanly. Otherwise, the
    		// process.waitFor() call below blocks indefinitely. 
    		logger.info("Reading dot process stdout stream");
    		while(stdout.read(buffer) != -1);   		
    		
    		logger.info("Reading dot process stderr stream");
    		while(stderr.read(buffer) != -1);
    		
    		logger.info("Waiting for dot process to terminate");
			if(process.waitFor() != 0) {
				String msg = "dot process exited unsuccuessfully: " + process.exitValue();
				logger.warning(msg);
			}
			buffer = null;
			skip = null;
		} catch (Exception e) {
			String msg = "Failed to shutdown dot process cleanly";
			logger.log(Level.WARNING, msg, e);
			process.destroy();
		} finally {
			logger.info("Closing dot process stdout stderr streams");
			IOUtils.closeQuietly(stdout);
			IOUtils.closeQuietly(stderr);
		}
    }
    
    @Override
    @SuppressWarnings("unused")
    public String submit(String dot) {
    	OutputStream stdin = process.getOutputStream();
    	InputStream stdout = process.getInputStream();
    	InputStream stderr = process.getErrorStream();
    	
    	// the dot process only starts writing to stdout after
    	// receiving the terminating '}' character on stdin.
    	// We need to ensure that '}' is at the end of the input string
    	// so we don't cause a deadlock. Note: because of dot's well defined
    	// behavior we don't need multiple threads to wait on the dot process
    	// stdout and stderr. BTW, creating threads in a SLSB is illegal!
    	if(!dot.endsWith(DOT_END)) {
    		throw new IllegalArgumentException("Invalid DOT syntax: must end with '}'");
    	}
    	
    	try {
    		stdin.write(dot.getBytes(CHARSET));
			stdin.flush();
			
			for(int spin = 0; stdout.available() == 0; spin++) {
	    		if(stderr.available() > 0) {
	    			int n = stderr.read(buffer);
	    			String msg = new String(buffer, 0, n, "UTF-8");
	    			logger.severe("dot process stderr: " + msg);
	    			throw new RuntimeException(msg);
	    		}
	    	}
	    	
			copy(stdout);
						
			return substr(SVG_START, SVG_END);
		} catch(IOException e) {
			String msg = "IO error occured while interacting with dot process";
			logger.log(Level.SEVERE, msg, e);
			throw new RuntimeException(msg, e);
		}
    }
    
    private void copy(InputStream in) throws IOException {
    	int n = 0; 
        count = 0;
    	while((n = in.read(buffer, count, buffer.length-count)) != -1) {  
        	count += n;
        	
        	if(end()) {
				return;
        	}
        	
        	if(buffer.length - count <= 4096) {
        		buffer = Arrays.copyOf(buffer, buffer.length + 32768);
        	}
        }
        
        throw new EOFException();
    }
    
    private boolean end() {
    	if(count < SVG_END.length)
    		return false;
    	
    	for(int i = count - SVG_END.length, j = 0; i < count; i++, j++) {
    		if(buffer[i] != SVG_END[j])
    			return false;
    	}
    	return true;
    }
    
    private String substr(byte[] from, byte[] to) {
    	int start = indexOf(from,  0);
    	if(start == -1) {
    		throw new RuntimeException("Failed to find start of graph markup: " + new String(from, CHARSET));
    	}
    	// buffer content ends with SVG_END (see end method above)
    	int end = to == SVG_END ? count : indexOf(to, start + from.length) + to.length;
    	if(end == -1) {
    		throw new RuntimeException("Failed to find end of graph markup: " + new String(to, CHARSET));
    	}
    	return new String(buffer, start, end - start, CHARSET);
    }
    
    // Boyer-Moore-Horspool text search.
    // http://www.dcc.uchile.cl/~rbaeza/handbook/algs/7/713b.srch.c
    private int indexOf(byte[] target, int fromIndex) {
    	final int m = target.length - 1;
    	int i, j, k;
    	
    	for (k = 0; k < skip.length; k++)
    		skip[k] = target.length;
    	
    	for (k = 0; k < m; k++)
    		skip[target[k] & 0xFF] = m - k;

    	for (k = fromIndex + m; k < count; k += skip[buffer[k] & 0xFF]) {
    		for (j = m, i = k; j >= 0 && buffer[i] == target[j]; j--, i--);
    		if (j == -1) return i + 1;
    	}
    	return -1;
    }
}
