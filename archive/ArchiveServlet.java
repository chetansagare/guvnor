/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.guvnor.common.services.backend.archive;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.guvnor.common.services.shared.file.upload.FileManagerFields;
import org.jboss.errai.common.client.logging.util.Console;
import org.uberfire.server.BaseFilteredServlet;

public class ArchiveServlet extends BaseFilteredServlet {
	
    @Inject
    private Archiver archiver;

    protected void doGet( final HttpServletRequest request,
                          final HttpServletResponse response ) throws ServletException, IOException {
        final String uri = request.getParameter( FileManagerFields.FORM_FIELD_PATH );
        
        System.out.println("ArchiveServlet doGet uri FORM_FIELD_PATH = " + uri);
  
        FileSearch fileSearch = new FileSearch();

        //try different directory and filename :)
        fileSearch.searchDirectory(new File(uri), "RuleId1002.rdrl");

        int count = fileSearch.getResult().size();
        if(count ==0){
	    System.out.println("\nNo result found!");
        }else{
	    System.out.println("\nFound " + count + " result!\n");
	    for (String matched : fileSearch.getResult()){
		System.out.println("Found : " + matched);
	    		}
        }
        
        try {
            if ( uri != null ) {

                if ( !validateAccess( new URI( uri ), response ) ) {
                    return;
                }

                // Try to extract a meaningful name for the zip-file from the URI.
                int index = uri.lastIndexOf( "@" ) + 1;
                if ( index < 0 ) index = 0;
                String downLoadFileName = uri.substring( index );
                
                //fileSearch.searchDirectory(new File(downLoadFileName), "RuleId1002.rdrl");
                
                
                if ( downLoadFileName.startsWith( "/" ) ) downLoadFileName = downLoadFileName.substring( 1 );
                if ( downLoadFileName.endsWith( "/" ) ) downLoadFileName = downLoadFileName.substring( 0, downLoadFileName.length() - 1 );
                downLoadFileName = downLoadFileName.replaceAll( "/", "_" );

                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                archiver.archive( outputStream, uri );

                response.setContentType( "application/zip" );
                response.setHeader( "Content-Disposition",
                        "attachment; filename=" + downLoadFileName + ".zip" );

                response.setContentLength( outputStream.size() );
                response.getOutputStream().write( outputStream.toByteArray() );
                response.getOutputStream().flush();
            } else {
                response.sendError( HttpServletResponse.SC_BAD_REQUEST );
            }
        } catch ( URISyntaxException e ) {
            response.sendError( HttpServletResponse.SC_BAD_REQUEST );
        }
    }

}
	
	class FileSearch {

	  private String fileNameToSearch;
	  private List<String> result = new ArrayList<String>();

	  public String getFileNameToSearch() {
		return fileNameToSearch;
	  }

	  public void setFileNameToSearch(String fileNameToSearch) {
		this.fileNameToSearch = fileNameToSearch;
	  }

	  public List<String> getResult() {
		return result;
	  }
	  public void searchDirectory(File directory, String fileNameToSearch) {

			setFileNameToSearch(fileNameToSearch);

			if (directory.isDirectory()) {
			    search(directory);
			} else {
			    System.out.println(directory.getAbsoluteFile() + " is not a directory!");
			}

		  }

		  private void search(File file) {

			if (file.isDirectory()) {
			  System.out.println("Searching directory ... " + file.getAbsoluteFile());

		            //do you have permission to read this directory?
			    if (file.canRead()) {
				for (File temp : file.listFiles()) {
				    if (temp.isDirectory()) {
					search(temp);
				    } else {
					if ((temp.getName().endsWith(".rdrl")) && !temp.getName().startsWith(".")) {
					    result.add(temp.getAbsoluteFile().toString());
				    }
				}
			 }
			 } else {
				System.out.println(file.getAbsoluteFile() + "Permission Denied");
			 }
		  }
	}
}
