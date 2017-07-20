/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.guvnor.common.services.backend.archive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;

import javax.inject.Inject;
import javax.inject.Named;

import org.drools.compiler.compiler.DrlParser;
import org.drools.compiler.compiler.xml.XmlDumper;
import org.drools.compiler.lang.descr.PackageDescr;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.DirectoryStream;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;

public class Archiver {

    private Path originalPath;

    private ZipWriter zipWriter;

    private IOService ioService;

    public Archiver() {
    }

    @Inject
    public Archiver(@Named("ioStrategy") IOService ioService) {
        this.ioService = ioService;
    }

    public void archive( final ByteArrayOutputStream outputStream,
                         final String uri ) throws IOException, URISyntaxException {

        init(outputStream, uri);
        zip();
    }

    private void zip() throws IOException {
        if (Files.isDirectory(originalPath)) {
            addPath(Files.newDirectoryStream(originalPath));
        } else {
            addFile(originalPath);
        }
        zipWriter.close();
    }
  //System.out.println("[ Original_Path_to_String : "+originalPath.toString()+" ] [ Original_Path_File_Name : "+ originalPath.getFileName()+" ] [ File_System_Name : "+originalPath.getFileSystem()+" ]  [ Root : "+originalPath.getRoot()+" ] [ Get_Name_Method : "+originalPath.getName(0)+" ]");
    
    private void init( final ByteArrayOutputStream outputStream,
                       final String uri ) throws URISyntaxException {
        this.originalPath = ioService.get(new URI(uri));
        this.zipWriter = new ZipWriter( outputStream );
    }

    private void addPath(DirectoryStream<Path> directoryStream) throws IOException {
        for (Path subPath : directoryStream) {
            if (Files.isDirectory(subPath)) {
                addPath(Files.newDirectoryStream(subPath));
            } else {
                if(subPath.getFileName().toString().endsWith(".rdrl") && !subPath.getFileName().toString().startsWith(".")) {
            		addFile(subPath);
                }
            }
        }
    }

    private void addFile( final Path subPath ) throws IOException {
    	
    	try {
    		String str = convertDrlFileToXml(ioService.newInputStream( subPath ));
			System.out.println(str);
			zipWriter.addFile( getZipEntry( subPath ),
					new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)) );
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private ZipEntry getZipEntry( final Path subPath ) {

    	String xmlFileName = subPath.getFileName().toString().replaceAll(".rdrl",".xml");
        return new ZipEntry(xmlFileName);
    }

    static class FileNameResolver {

        static protected String resolve( final String subPath,
                                         final String originalPath ) {
            if ("/".equals(originalPath)) {
                return subPath.substring(originalPath.length());
            } else {
                return getBaseFolder(originalPath) + subPath.substring(originalPath.length() + 1);
            }
        }

        private static String getBaseFolder( final String originalPath ) {
            if (originalPath.contains("/")) {
                return originalPath.substring(originalPath.lastIndexOf("/") + 1) + "/";
            } else {
                return originalPath + "/";
            }
        }
    }
    private static String convertDrlFileToXml(InputStream ip) throws Exception {
    	  Reader source = new InputStreamReader(ip);
    	  DrlParser drlParser = new DrlParser();
    	  PackageDescr pkgDesc = drlParser.parse(source);
    	  XmlDumper xmlDumper = new XmlDumper();
    	  String xml = xmlDumper.dump(pkgDesc);
    	  return xml;
    	 }
}