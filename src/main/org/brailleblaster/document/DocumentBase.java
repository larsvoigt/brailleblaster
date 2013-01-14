/* BrailleBlaster Braille Transcription Application
  *
  * Copyright (C) 2010, 2012
  * ViewPlus Technologies, Inc. www.viewplus.com
  * and
  * Abilitiessoft, Inc. www.abilitiessoft.com
  * and
  * American Printing House for the Blind, Inc. www.aph.org
  *
  * All rights reserved
  *
  * This file may contain code borrowed from files produced by various 
  * Java development teams. These are gratefully acknoledged.
  *
  * This file is free software; you can redistribute it and/or modify it
  * under the terms of the Apache 2.0 License, as given at
  * http://www.apache.org/licenses/
  *
  * This file is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE
  * See the Apache 2.0 License for more details.
  *
  * You should have received a copy of the Apache 2.0 License along with 
  * this program; see the file LICENSE.
  * If not, see
  * http://www.apache.org/licenses/
  *
  * Maintained by John J. Boyer john.boyer@abilitiessoft.com
*/

package org.brailleblaster.document;

import org.brailleblaster.BBIni;
import org.brailleblaster.util.CheckLiblouisutdmlLog;
import org.liblouis.liblouisutdml;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import java.io.InputStream;
import org.brailleblaster.util.FileUtils;

/**
 * This class gives access to the facilities of the document package, 
 * which encapsulates knowledge of the vocabulary of the particular 
 * document. Other classes thus deal with an abstraction by calling 
 methods in this class. Thus they have no need to worry about the 
 * vocabulary of the particular document, unless they are displaying a 
 * tree view. 
 */
public class DocumentBase {

private FileUtils fu = new FileUtils();
private String fileSep = BBIni.getFileSep();
private Semantics sm = new Semantics();
private Styles st = new Styles();
private Actions act = new Actions();
private liblouisutdml lutdml = liblouisutdml.getInstance();

/**
 * Parse the document and set up the neccessary data structures. This 
 * method handles input that does not come from files.
 * @param inputStream a stream of bytes
 * @param configFile The name of a liblouisutdml configuration file
 * @param configSettings additional configuration settings
 */
public void startDocument (InputStream inputStream, String configFile, 
String configSettings) throws Exception {
String fileName = "xxx";
sm.makeSemantics (fileName);
}

/**
 * Similar to previous method, except that it handles input files 
 * explicitly.
 * @param completePath complete path to the file
 */
public void startDocument (String completePath, String configFile, 
String configSettings) throws Exception {
setupFromFile (completePath, configFile, configSettings);
}

/**
 * Save the document, Which contains UTDML and may have been edited, so 
 * that work can be resumed.
 * @param completePath Complete path to where the file should be saved
 */
public void saveWorkingFile (String completePath) {
int extPos = completePath.lastIndexOf (".") + 1;
String ext = completePath.substring (extPos);
if (!ext.equalsIgnoreCase ("utd")) {
throw new IllegalArgumentException ("File name must end in 'utd'.");
}
sm.saveWorkingFile (completePath);
}

/**
 * Save the document, with all print and Braille editsl <brl> nodes and 
 * meta,name,brl are removed.
 * @param completePath the location in which the file is to be saved
 */
public void saveEnhancedDocument (String completePath) {
sm.saveEnhancedDocument (completePath);
}

/**
 * Return the tree created by parsing the document.
 */
public Document getDocumentTree() {
return sm.workingDocument;
}

/**
 * Edit or creat a style to be used in the word processor view.
 * @param stylename the name of the style to be edited or created
 */
public void editCreateStyle (String styleName) {
Styles.StyleType styleType = st.readStyle (styleName);
st.editStyle (styleType);
st.writeStyle (styleType);
}

public Nodes getNodes (Node node, String xpathExpr) {
return sm.getNodes (node, xpathExpr);
}

public Node getContextNode() {
return null;
}

/**
 * Render a document with UTDML, then parse the result and set up data 
 * structures.. This is for documents that exist as 
 * files, either in their own right or as temparary files.
 * @param completePath complete path to the input file
 * @param configFile name of a liblouisutdml configuration file
 * @param configSettings a String containing additional configuration 
 settings of the form
 *    setting1 value\n setting2 value\n ...
 */
private boolean setupFromFile (String completePath, String 
configFile, 
String configSettings) throws Exception {
String configFileWithPath = fu.findInProgramData ("lbu_files" + fileSep 
+ configFile);
String configWithUTD;
if (configSettings == null) {
configWithUTD = "formatFor utd\n mode notUC\n";
} else {
configWithUTD = configSettings + "formatFor utd\n mode notUC\n";
}
String outFile = BBIni.getTempFilesPath() + fileSep + 
"outFile.utd";
String logFile = BBIni.getLogFilesPath() + fileSep + 
"liblouisutdml.log";
boolean success = false;
int extPos = completePath.lastIndexOf (".") + 1;
String ext = completePath.substring (extPos);
if (ext.equalsIgnoreCase ("xml")) {
success = lutdml.translateFile (configFileWithPath, completePath, 
outFile, 
logFile, configWithUTD, 0);
} else
if (ext.equalsIgnoreCase ("txt")) {
success = lutdml.translateTextFile (configFileWithPath, completePath, 
outFile, 
logFile, configWithUTD, 0);
} else
if (ext.equalsIgnoreCase ("brf")) {
success = lutdml.backTranslateFile (configFileWithPath, completePath, 
outFile, 
logFile, configWithUTD, 0);
} else
if (ext.equalsIgnoreCase ("utd")) {
sm.makeSemantics (completePath);
success = true;
} else {
throw new IllegalArgumentException 
(completePath + " not .xml, .txt, or .brf");
}
new CheckLiblouisutdmlLog().displayLog();
if (!success) {
return false;
}
sm.makeSemantics (outFile);
return true;
}

}
