package org.brailleblaster.document;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import org.brailleblaster.BBIni;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.StylesType;
import org.brailleblaster.util.FileUtils;

public class ConfigFileHandler {
	String configFile, documentConfig;
	FileUtils fu;
	
	public ConfigFileHandler(String configFile){
		this.configFile = configFile;
		this.fu = new FileUtils();
		this.documentConfig = null;
	}
	
	public ConfigFileHandler(String configFile, String documentName){
		this.configFile = configFile;
		this.fu = new FileUtils();
		this.documentConfig = fu.getFileName(documentName) + ".cfg";
	}
	
	public void updateStyle(org.brailleblaster.perspectives.braille.document.BBSemanticsTable.Styles newStyle){
		String path = BBIni.getUserProgramDataPath() + BBIni.getFileSep() + "liblouisutdml" + BBIni.getFileSep() + "lbu_files" + BBIni.getFileSep() + configFile;
		if(!fu.exists(path))
			fu.copyFile(BBIni.getProgramDataPath() + BBIni.getFileSep() + "liblouisutdml" + BBIni.getFileSep() + "lbu_files" + BBIni.getFileSep() + configFile, path);
		
		String key = "style " + newStyle.getName();
		String entry = formatStyleEntry(newStyle);
		
		String fileString = getFileContentsAsString(path);
		
		if(fileString != null){
			int startIndex = fileString.indexOf(key);
			int endIndex = fileString.substring(startIndex + 5).indexOf("style");
			
			if(endIndex != -1)
				fileString = fileString.replace(fileString.substring(startIndex, startIndex + endIndex + 5), entry);
			else
				fileString = fileString.replace(fileString.substring(startIndex), entry);
			
			fu.writeToFile(path, fileString);
		}
	}
	
	public void appendStyle(org.brailleblaster.perspectives.braille.document.BBSemanticsTable.Styles style){
		String path = BBIni.getUserProgramDataPath() + BBIni.getFileSep() + "liblouisutdml" + BBIni.getFileSep() + "lbu_files" + BBIni.getFileSep() + configFile;
		if(!fu.exists(path))
			fu.copyFile(BBIni.getProgramDataPath() + BBIni.getFileSep() + "liblouisutdml" + BBIni.getFileSep() + "lbu_files" + BBIni.getFileSep() + configFile, path);
		
		String entry = formatStyleEntry(style);
		
		fu.appendToFile(path, entry);
	}
	
	public void updateDocumentStyle(org.brailleblaster.perspectives.braille.document.BBSemanticsTable.Styles newStyle){
		String path = BBIni.getTempFilesPath() + BBIni.getFileSep() + documentConfig;
		if(!fu.exists(path))
			fu.create(path);
		
		String key = "style " + newStyle.getName();
		String entry = formatStyleEntry(newStyle);
		
		String fileString = getFileContentsAsString(path);
		
		if(fileString != null){
			int startIndex = fileString.indexOf(key);
			int endIndex = fileString.substring(startIndex + 5).indexOf("style");
			
			if(endIndex != -1)
				fileString = fileString.replace(fileString.substring(startIndex, startIndex + endIndex + 5), entry);
			else
				fileString = fileString.replace(fileString.substring(startIndex), entry);
			
			fu.writeToFile(path, fileString);
		}
	}
	
	public void appendDocumentStyle(org.brailleblaster.perspectives.braille.document.BBSemanticsTable.Styles style){
		String path = BBIni.getTempFilesPath() + BBIni.getFileSep() + documentConfig;
		if(!fu.exists(path))
			fu.create(path);
		
		String entry = formatStyleEntry(style);
		
		fu.appendToFile(path, entry);
	}
	
	private String formatStyleEntry(org.brailleblaster.perspectives.braille.document.BBSemanticsTable.Styles style){
		String entry = "style " + style.getName() + "\n"; 
		
		Set<StylesType> set = style.getKeySet();
    	for(StylesType type : set)
    		entry += "\t" + type.toString() + " " + style.get(type) + "\n";
		
    	return entry;
	}
	
	private String getFileContentsAsString(String path){
		 BufferedReader reader = null;

		try {
			reader = new BufferedReader( new FileReader (path));
			String line = null;
			StringBuilder stringBuilder = new StringBuilder();
			String ls = System.getProperty("line.separator");

			while( ( line = reader.readLine() ) != null ) {
				stringBuilder.append( line );
				stringBuilder.append( ls );
			}

			return stringBuilder.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		finally {
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void deleteStyle(String style){
		String path = BBIni.getUserProgramDataPath() + BBIni.getFileSep() + "liblouisutdml" + BBIni.getFileSep() + "lbu_files" + BBIni.getFileSep() + configFile;
		if(!fu.exists(path))
			fu.copyFile(BBIni.getProgramDataPath() + BBIni.getFileSep() + "liblouisutdml" + BBIni.getFileSep() + "lbu_files" + BBIni.getFileSep() + configFile, path);
		
		String key = "style " + style;
		
		String fileString = getFileContentsAsString(path);
		
		if(fileString != null){
			int startIndex = fileString.indexOf(key);
			int endIndex = fileString.substring(startIndex + 5).indexOf("style");
			
			if(endIndex != -1)
				fileString = fileString.replace(fileString.substring(startIndex, startIndex + endIndex + 5), "");
			else
				fileString = fileString.replace(fileString.substring(startIndex), "");
			
			fu.writeToFile(path, fileString);
		}
	}
	
	public void restoreDefaults(){
		String path = BBIni.getUserProgramDataPath() + BBIni.getFileSep() + "liblouisutdml" + BBIni.getFileSep() + "lbu_files" + BBIni.getFileSep() + configFile;
		if(fu.exists(path))
			fu.deleteFile(path);	
	}
}
