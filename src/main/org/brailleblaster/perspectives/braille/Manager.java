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

package org.brailleblaster.perspectives.braille;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.print.PrintException;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Text;

import org.brailleblaster.BBIni;
import org.brailleblaster.archiver.Archiver;
import org.brailleblaster.archiver.ArchiverFactory;
import org.brailleblaster.document.BBDocument;
import org.brailleblaster.document.BBSemanticsTable;
import org.brailleblaster.document.BBSemanticsTable.Styles;
import org.brailleblaster.document.BBSemanticsTable.StylesType;
import org.brailleblaster.localization.LocaleHandler;
import org.brailleblaster.perspectives.braille.mapping.MapList;
import org.brailleblaster.perspectives.braille.mapping.TextMapElement;
import org.brailleblaster.perspectives.braille.messages.Message;
import org.brailleblaster.perspectives.braille.views.BrailleView;
import org.brailleblaster.perspectives.braille.views.TextView;
import org.brailleblaster.perspectives.braille.views.TreeView;
import org.brailleblaster.printers.PrintPreview;
import org.brailleblaster.printers.PrintersManager;
import org.brailleblaster.stylePanel.StyleManager;
import org.brailleblaster.util.FileUtils;
import org.brailleblaster.util.Notify;
import org.brailleblaster.util.YesNoChoice;
import org.brailleblaster.util.Zipper;
import org.brailleblaster.wordprocessor.BBFileDialog;
import org.brailleblaster.wordprocessor.FontManager;
import org.brailleblaster.wordprocessor.WPManager;
import org.daisy.printing.PrinterDevice;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;

//This class manages each document in an MDI environment. It controls the braille View and the daisy View.
public class Manager {
	WPManager wp;
	TabItem item;
	Group group;
	TreeView treeView;
	private TextView text;
	private BrailleView braille;
	StyleManager sm;
	FormLayout layout;
	Control [] tabList;
	BBSemanticsTable styles;
	static int docCount = 0;
	String documentName = null;
	private boolean metaContent = false;
	String logFile = "Translate.log";
	String configSettings = null;
	static String recentFileName = null;
	LocaleHandler lh = new LocaleHandler();
	static Logger logger;
	public BBDocument document;
	private boolean simBrailleDisplayed = false;
	MapList list;
	String zippedPath;
	String workingFilePath;
	FileUtils fu;
	String currentConfig;
	
	//Constructor that sets things up for a new document.
	public Manager(WPManager wp, String docName) {
		if(docName != null)
			currentConfig = BBIni.getDefaultConfigFile();
		else 
			currentConfig = "nimas.cfg";
		
		this.fu = new FileUtils();
		this.styles = new BBSemanticsTable(currentConfig);
		this.documentName = docName;
		this.list = new MapList(this);
		this.wp = wp;
		this.item = new TabItem(wp.getFolder(), 0);
		this.group = new Group(wp.getFolder(),SWT.NONE);
		this.group.setLayout(new FormLayout());	
		this.sm = new StyleManager(this);
		this.treeView = new TreeView(this, this.group);
		this.text = new TextView(this.group, this.styles);
		this.braille = new BrailleView(this.group, this.styles);
		this.item.setControl(this.group);
		initializeDocumentTab();
		this.document = new BBDocument(this, this.styles);
		FontManager.setFontWidth(this);
		
		logger = BBIni.getLogger();
		
		docCount++;
		
		if(docName != null)
			openDocument(docName);
		else {
			initializeAllViews(docName, BBIni.getProgramDataPath() + BBIni.getFileSep() + "xmlTemplates" + BBIni.getFileSep() + "dtbook.xml", null);
			Nodes n = this.document.query("/*/*[2]/*[2]/*[1]/*[1]");
			((Element)n.get(0)).appendChild(new Text(""));
			this.list.add(new TextMapElement(0, 0, n.get(0).getChild(0)));
			setTabTitle(docName);
		}				
	}
	

	private void initializeDocumentTab(){
		FontManager.setShellFonts(wp.getShell(), this);	
		setTabList();
		wp.getShell().layout();
	}
	
	private void setTabList(){
		if(sm.tableIsVisible()){
			tabList = new Control[]{treeView.view, sm.getGroup(), getText().view, getBraille().view};
		}
		else {
			tabList = new Control[]{treeView.view, getText().view, getBraille().view};
		}
		group.setTabList(tabList);
	}
	
	public void fileSave(){	
		// Borrowed from Save As function. Different document types require 
		// different save methods.
		if(workingFilePath == null){
			saveAs();
		}
		else {
			checkForUpdatedViews();
			if(workingFilePath.endsWith("xml")){
				if(fu.createXMLFile(document.getNewXML(), workingFilePath)){
					String tempSemFile = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(workingFilePath) + ".sem"; 
					copySemanticsFile(tempSemFile, fu.getPath(workingFilePath) + BBIni.getFileSep() + fu.getFileName(workingFilePath) + ".sem");
				}
				else {
					new Notify("An error occured while saving your document.  Please check your original document.");
				}
			}
			else if(workingFilePath.endsWith("utd")) {		
				document.setOriginalDocType(document.getDOM());
				if(fu.createXMLFile(document.getDOM(), workingFilePath)){
					String tempSemFile = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(workingFilePath) + ".sem"; 
					copySemanticsFile(tempSemFile, fu.getPath(workingFilePath) + BBIni.getFileSep() + fu.getFileName(workingFilePath) + ".sem");
				}
				else {
					new Notify("An error occured while saving your document.  Please check your original document.");
				}
			}
			else if(workingFilePath.endsWith("brf")){
				if(!document.createBrlFile(this, workingFilePath)){
					new Notify("An error has occurred.  Please check your original document");
				}
			}
			
			// If the document came from a zip file, then rezip it.
			if(zippedPath.length() > 0)
			{
				// Create zipper.
				Zipper zpr = new Zipper();
				// Input string.
				String sp = BBIni.getFileSep();
				String inPath = BBIni.getTempFilesPath() + zippedPath.substring(zippedPath.lastIndexOf(sp), zippedPath.lastIndexOf(".")) + sp;
//				String inPath = zippedPath.substring(0, zippedPath.lastIndexOf(".")) + BBIni.getFileSep();
				// Zip it!
				zpr.Zip(inPath, zippedPath);
			}
		
			getText().hasChanged = false;
			getBraille().hasChanged = false;
		}
	}
	
	public void fileOpenDialog() {
		String tempName;

		String[] filterNames = new String[] { "XML", "XML ZIP", "XHTML", "HTML","HTM", "EPUB", "TEXT", "BRF", "UTDML working document", };
		String[] filterExtensions = new String[] { "*.xml", "*.zip", "*.xhtml","*.html", "*.htm", "*.epub", "*.txt", "*.brf", "*.utd", };
		BBFileDialog dialog = new BBFileDialog(wp.getShell(), SWT.OPEN, filterNames, filterExtensions);
		
		tempName = dialog.open();
		
		// Don't do any of this if the user failed to choose a file.
		if(tempName != null)
		{
			// 
			
			// Open it.
			if(workingFilePath != null || getText().hasChanged || getBraille().hasChanged || documentName != null){
				wp.addDocumentManager(tempName);
			}
			else {
				closeUntitledTab();
				openDocument(tempName);
				checkTreeFocus();
			}
			
		} // if(tempName != null)
	}
	
	public void openDocument(String fileName){
		
		// Create archiver and massage document if necessary.
		Archiver arch = ArchiverFactory.getArchive(fileName);
		String archFileName = arch.open();
		
		// File unsupported by Archiver. Let Braille Blaster handle it.
		if(archFileName != null)
			workingFilePath = archFileName;
		else
			workingFilePath = fileName;
		
		////////////////////////
		// Zip and Recent Files.
		/*
			// If the file opened was an xml zip file, unzip it.
			if(fileName.endsWith(".zip")) {
				// Create unzipper.
				Zipper unzipr = new Zipper();
				// Unzip and update "opened" file.
//				workingFilePath = unzipr.Unzip(fileName, fileName.substring(0, fileName.lastIndexOf(".")) + BBIni.getFileSep());
				String sp = BBIni.getFileSep();
				String tempOutPath = BBIni.getTempFilesPath() + fileName.substring(fileName.lastIndexOf(sp), fileName.lastIndexOf(".")) + sp;
				workingFilePath = unzipr.Unzip(fileName, tempOutPath);
				// Store paths.
				zippedPath = fileName;
			}
			else {
				// There is no zip file to deal with.
				zippedPath = "";
			}
			*/
			////////////////
			// Recent Files.
				
				// Get recent file list.
				ArrayList<String> strs = this.wp.getMainMenu().getRecentDocumentsList();
				
				// Search list for duplicate. If one exists, don't add this new one.
				for(int curStr = 0; curStr < strs.size(); curStr++) {
					if(strs.get(curStr).compareTo(fileName) == 0) {
						
						// This isn't a new document. First, remove from doc list and recent item submenu.
						wp.getMainMenu().getRecentDocumentsList().remove(curStr);
						wp.getMainMenu().getRecentItemSubMenu().getItem(curStr).dispose();
						
						// We found a duplicate, so there is no point in going further.
						break;
						
					} // if(strs.get(curStr)...
					
				} // for(int curStr = 0...
				
				// Add to top of recent items submenu.
				wp.getMainMenu().addRecentEntry(fileName);
				
			// Recent Files.
			////////////////

		// Zip and Recent Files.
		////////////////////////
		
		initializeAllViews(fileName, workingFilePath, null);
	}	
	
	private void initializeAllViews(String fileName, String filePath, String configSettings){
		try{
			if(document.startDocument(filePath, currentConfig, configSettings)){
				group.setRedraw(false);
				getText().view.setWordWrap(false);
				getBraille().view.setWordWrap(false);
				wp.getStatusBar().resetLocation(6,100,100);
				wp.getStatusBar().setText("Loading...");
				wp.startProgressBar(this);
				documentName = fileName;
				setTabTitle(fileName);
				treeView.setRoot(document.getRootElement(), this);
				initializeViews(document.getRootElement());
				document.notifyUser();
				getText().initializeListeners(this);
				getBraille().initializeListeners(this);
				treeView.initializeListeners(this);
				getText().hasChanged = false;
				getBraille().hasChanged = false;
				wp.getStatusBar().resetLocation(0,100,100);
				wp.getProgressBar().stop();
				wp.getStatusBar().setText("Words: " + getText().words);
				getBraille().setWords(getText().words);
				getText().view.setWordWrap(true);
				getBraille().view.setWordWrap(true);
				group.setRedraw(true);
			}
			else {
				System.out.println("The Document Base document tree is empty");
				logger.log(Level.SEVERE, "The Document Base document tree is null, the file failed to parse properly");
			}
		}
		catch(Exception e){
			e.printStackTrace();
			logger.log(Level.SEVERE, "Unforeseen Exception", e);
		}
	}
	
	private void initializeViews(Node current){
		if(current instanceof Text && !((Element)current.getParent()).getLocalName().equals("brl") && vaildTextElement(current.getValue())){
			getText().setText(current, list);
		}
		
		for(int i = 0; i < current.getChildCount(); i++){
			if(current.getChild(i) instanceof Element &&  ((Element)current.getChild(i)).getLocalName().equals("brl")){
				initializeBraille(current.getChild(i), list.getLast());
			}
			else {
				if(current.getChild(i) instanceof Element && !((Element)current.getChild(i)).getLocalName().equals("pagenum")){
					Element currentChild = (Element)current.getChild(i);
					document.checkSemantics(currentChild);
					if(!currentChild.getLocalName().equals("meta") & !currentChild.getAttributeValue("semantics").contains("skip"))
						initializeViews(currentChild);
				}
				else if(!(current.getChild(i) instanceof Element)) {
					initializeViews(current.getChild(i));
				}
			}
		}
	}
	
	private void initializeBraille(Node current, TextMapElement t){
		if(current instanceof Text && ((Element)current.getParent()).getLocalName().equals("brl")){
			Element grandParent = (Element)current.getParent().getParent();
			if(!(grandParent.getLocalName().equals("span") && document.checkAttributeValue(grandParent, "class", "brlonly")))
				getBraille().setBraille(current, t);
		}
		
		for(int i = 0; i < current.getChildCount(); i++){
			if(current.getChild(i) instanceof Element){
				initializeBraille(current.getChild(i), t);
			}
			else {
				initializeBraille(current.getChild(i), t);
			}
		}
	}
	
	public void dispatch(Message message){
		switch(message.type){
			case INCREMENT:
				handleIncrement(message);
				break;
			case DECREMENT:
				handleDecrement(message);
				break;
			case UPDATE_CURSORS:
				handleUpdateCursors(message);
				break;
			case SET_CURRENT:
				handleSetCurrent(message);
				break;
			case GET_CURRENT:
				handleGetCurrent(message);
				break;
			case TEXT_DELETION:
				handleTextDeletion(message);
				break;
			case UPDATE:
				handleUpdate(message);
				break;
			case INSERT_NODE:
				handleInsertNode(message);
				break;
			case REMOVE_NODE:
				handleRemoveNode(message);
				break;
			case UPDATE_STATUSBAR:
				handleUpdateStatusBar(message);
				break;
			case ADJUST_ALIGNMENT:
				handleAdjustAlignment(message);
				break;
			case ADJUST_INDENT:
				handleAdjustIndent(message);
				break;
			case ADJUST_RANGE:
				list.adjustOffsets(list.getCurrentIndex(), message);
				break;
			case GET_TEXT_MAP_ELEMENTS:
				list.findTextMapElements(message);
				break;
			case UPDATE_SCROLLBAR:
				handleUpdateScrollbar(message);
				break;
			case UPDATE_STYLE:
				handleUpdateStyle(message);
				break;
			default:
				break;
		}
	}
	
	private void handleIncrement(Message message){
		list.incrementCurrent(message);
		treeView.setSelection(list.getCurrent(), message, this);
		resetCursorData();
	}
	
	private void handleDecrement(Message message){
		list.decrementCurrent(message);
		treeView.setSelection(list.getCurrent(), message, this);
		resetCursorData();
	}
	
	private void handleUpdateCursors(Message message){
		message.put("element", list.getCurrent().n);
		if(message.getValue("sender").equals("text")){
			setUpdateCursorMessage(message, getText().positionFromStart, getText().cursorOffset);
			getBraille().updateCursorPosition(message);
		}
		else if(message.getValue("sender").equals("braille")) {
			setUpdateCursorMessage(message, getBraille().positionFromStart, getBraille().cursorOffset);
			getText().updateCursorPosition(message);
		}
		else if(message.getValue("sender").equals("tree")){
			setUpdateCursorMessage(message, getText().positionFromStart, getText().cursorOffset);
			getBraille().updateCursorPosition(message);
			setUpdateCursorMessage(message, getBraille().positionFromStart, getBraille().cursorOffset);
			getText().updateCursorPosition(message);
		}
	}
	
	private void setUpdateCursorMessage(Message m, int lastPosition, int offset){
		m.put("lastPosition", lastPosition);
		m.put("offset", offset);
		list.getCurrentNodeData(m);
	}
	
	private void handleSetCurrent(Message message){
		int index;
		list.checkList();
		if(message.getValue("isBraille").equals(true)){
			index = list.findClosestBraille(message);
			list.setCurrent(index);
			list.getCurrentNodeData(message);
			treeView.setSelection(list.getCurrent(), message, this);
		}
		else {
			message.put("selection", treeView.getSelection(list.getCurrent()));
			index = list.findClosest(message, 0, list.size() - 1);
			if(index == -1){
				list.getCurrentNodeData(message);
				treeView.setSelection(list.getCurrent(), message, this);
			}
			else {
				list.setCurrent(index);
				list.getCurrentNodeData(message);
				treeView.setSelection(list.getCurrent(), message, this);
			}
			sm.setStyleTableItem(list.getCurrent());
			resetCursorData();
		}
	}
	
	private void handleGetCurrent(Message message){
		message.put("selection", treeView.getSelection(list.getCurrent()));
		list.getCurrentNodeData(message);
		if(list.size() > 0)
			treeView.setSelection(list.getCurrent(), message, this);
	}
	
	private void handleTextDeletion(Message message){
		list.checkList();
		if((Integer)message.getValue("deletionType") == SWT.BS){
			if(list.hasBraille(list.getCurrentIndex())){
				getBraille().removeWhitespace(list.getCurrent().brailleList.getFirst().start + (Integer)message.getValue("length"),  (Integer)message.getValue("length"), SWT.BS, this);
			}
			list.shiftOffsetsFromIndex(list.getCurrentIndex(), (Integer)message.getValue("length"), (Integer)message.getValue("length"));
		}
		else if((Integer)message.getValue("deletionType") == SWT.DEL){
			list.shiftOffsetsFromIndex(list.getCurrentIndex() + 1, (Integer)message.getValue("length"), (Integer)message.getValue("length"));
			if(list.hasBraille(list.getCurrentIndex())){
				getBraille().removeWhitespace(list.get(list.getCurrentIndex() + 1).brailleList.getFirst().start,  (Integer)message.getValue("length"), SWT.DEL, this);
			}
		}
	}
	
	private void handleUpdate(Message message){
		message.put("selection", this.treeView.getSelection(list.getCurrent()));
		document.updateDOM(list, message);
		getBraille().updateBraille(list.getCurrent(), message);
		getText().reformatText(list.getCurrent().n, message, this);
		list.updateOffsets(list.getCurrentIndex(), message);
		list.checkList();
	}
	
	private void handleInsertNode(Message m){
		if(m.getValue("split").equals(true)){
			splitElement(m);
		}
		else {
			if(m.getValue("atStart").equals(true))
				insertElementAtBeginning(m);
			else
				insertElementAtEnd(m);
		}
	}
	
	private void splitElement(Message m){
		int treeIndex = treeView.getBlockElementIndex();
		
		ArrayList<Integer> originalElements = list.findTextMapElementRange(list.getCurrentIndex(), (Element)list.getCurrent().n.getParent(), true);
		ArrayList<Element> els = document.splitElement(list, list.getCurrent(), m);
		
		int textStart = list.get(originalElements.get(0)).start;
		int textEnd = list.get(originalElements.get(originalElements.size() - 1)).end;
		int brailleStart = list.get(originalElements.get(0)).brailleList.getFirst().start;	
			
		int brailleEnd = list.get(originalElements.get(originalElements.size() - 1)).brailleList.getLast().end;
				
		int currentIndex = list.getCurrentIndex();
		treeView.removeCurrent();
		
		for(int i = originalElements.size() - 1; i >= 0; i--){
			int pos = originalElements.get(i);
			
			if(pos < currentIndex){
				list.remove(pos);
				currentIndex--;
			}
			else if(pos >= currentIndex){
				list.remove(pos);
			}
		}
		
		getText().clearRange(textStart, textEnd - textStart);
		getBraille().clearRange(brailleStart, brailleEnd - brailleStart);
		list.shiftOffsetsFromIndex(currentIndex, -(textEnd - textStart), -(brailleEnd - brailleStart));	
		
		int firstElementIndex = currentIndex;
		currentIndex = insertElement(els.get(0), currentIndex, textStart, brailleStart) - 1;
		addTreeItems(firstElementIndex, currentIndex + 1, treeIndex);
		
		
		String insertionString = "";
		Styles style = styles.get(styles.getKeyFromAttribute(document.getParent(list.get(currentIndex).n, true)));

		if(style.contains(StylesType.linesBefore)){
			for(int i = 0; i < Integer.valueOf((String)style.get(StylesType.linesBefore)) + 1; i++){
				insertionString += "\n";
			}
		}
		else if(style.contains(StylesType.linesAfter)){
			for(int i = 0; i < Integer.valueOf((String)style.get(StylesType.linesAfter)) + 1; i++){
				insertionString += "\n";
			}
		}
		else {
			insertionString = "\n";
		}

		getText().insertText(list.get(currentIndex).end, insertionString);
		getBraille().insertText(list.get(currentIndex).brailleList.getLast().end, insertionString);
		//braille.insertLineBreak(list.get(currentIndex).brailleList.getLast().end);
		m.put("length", insertionString.length());
		
		int secondElementIndex = currentIndex + 1;
		currentIndex = insertElement(els.get(1), currentIndex + 1, list.get(currentIndex).end + insertionString.length(), list.get(currentIndex).brailleList.getLast().end + insertionString.length());
		addTreeItems(secondElementIndex, currentIndex,treeIndex + 1);
		list.shiftOffsetsFromIndex(currentIndex, list.get(currentIndex - 1).end - textStart, list.get(currentIndex - 1).brailleList.getLast().end - brailleStart);
	}
	
	private void addTreeItems(int start, int end, int treeIndex){
		Element parent = document.getParent(list.get(start).n, true);
		ArrayList<TextMapElement> elementList = new ArrayList<TextMapElement>();
		
		for(int i = start; i < end; i++){
			if(parent.equals(list.get(i).n.getParent())){
				elementList.add(list.get(i));
			}
		}
		
		if(elementList.size() > 0){
			treeView.newTreeItem(elementList, treeIndex);
		}
		else {
			treeView.newTreeItem(list.get(start), treeIndex);
		}
	}
	
	public int insertElement(Element e, int index, int start, int brailleStart){
		int count = e.getChildCount();
		int currentIndex = index;
		int currentStart = start;
		int currentBrailleStart = brailleStart;
		
		for(int i = 0; i < count; i++){
			if(e.getChild(i) instanceof Text){
				getText().insertText(list, currentIndex, currentStart, e.getChild(i));
				currentStart = list.get(currentIndex).end;
				i++;
				insertBraille((Element)e.getChild(i), currentIndex, currentBrailleStart);
				currentBrailleStart = list.get(currentIndex).brailleList.getLast().end;
				currentIndex++;
			}
			else if(e.getChild(i) instanceof Element && !((Element)e.getChild(i)).getLocalName().equals("brl")){
				currentIndex = insertElement((Element)e.getChild(i), currentIndex, currentStart, currentBrailleStart);
				currentStart = list.get(currentIndex - 1).end;
				currentBrailleStart = list.get(currentIndex - 1).brailleList.getLast().end;
			}
		}
		
		return currentIndex;
	}
	
	public void insertBraille(Element e, int index, int brailleStart){
		int count = e.getChildCount();
		
		for(int i = 0; i < count; i++){
			if(e.getChild(i) instanceof Text){
				getBraille().insert(list.get(index), e.getChild(i), brailleStart);
				brailleStart = list.get(index).brailleList.getLast().end;
			}
		}
	}
	
	private void insertElementAtBeginning(Message m){
		if(list.getCurrentIndex() > 0)
			document.insertEmptyTextNode(list, list.get(list.getCurrentIndex() - 1),  list.get(list.getCurrentIndex() - 1).end + 1, list.get(list.getCurrentIndex() - 1).brailleList.getLast().end + 1,list.getCurrentIndex());
		else
			document.insertEmptyTextNode(list, list.getCurrent(), list.getCurrent().start, list.getCurrent().brailleList.getFirst().start, list.getCurrentIndex());
			
		if(list.size() - 1 != list.getCurrentIndex() - 1){
			list.shiftOffsetsFromIndex(list.getCurrentIndex() + 1, 1, 1);
		}
		int index = treeView.getSelectionIndex();
		
		m.put("length", 1);
		m.put("newBrailleLength", 1);
		m.put("brailleLength", 0);

		if(list.getCurrentIndex()  > 0)
			getBraille().insertLineBreak(list.get(list.getCurrentIndex() - 1).brailleList.getLast().end);
		else
			getBraille().insertLineBreak(list.getCurrent().brailleList.getFirst().start - 1);
			
		treeView.newTreeItem(list.get(list.getCurrentIndex()), index);
	}
	
	private void insertElementAtEnd(Message m){
		document.insertEmptyTextNode(list, list.getCurrent(), list.getCurrent().end + 1, list.getCurrent().brailleList.getLast().end + 1, list.getCurrentIndex() + 1);
		if(list.size() - 1 != list.getCurrentIndex() + 1){
			list.shiftOffsetsFromIndex(list.getCurrentIndex() + 2, 1, 1);
		}
		int index = treeView.getSelectionIndex();
		
		m.put("length", 1);
		m.put("newBrailleLength", 1);
		m.put("brailleLength", 0);

		getBraille().insertLineBreak(list.getCurrent().brailleList.getLast().end);
		treeView.newTreeItem(list.get(list.getCurrentIndex() + 1), index + 1);
	}
	
	private void handleRemoveNode(Message message){
		int index = (Integer)message.getValue("index");
		document.updateDOM(list, message);
		list.get(index).brailleList.clear();
		treeView.removeItem(list.get(index), message);
		list.remove(index);
					
		if(list.size() == 0)
			getText().removeListeners();
	}
	
	private void handleUpdateStatusBar(Message message){
		getBraille().setWords(getText().words);
		wp.getStatusBar().setText((String)message.getValue("line"));
	}
	
	private void handleAdjustAlignment(Message message){
		getBraille().changeAlignment(list.getCurrent().brailleList.getFirst().start, (Integer)message.getValue("alignment"));
	}
	
	private void handleAdjustIndent(Message message){
		getBraille().changeIndent(list.getCurrent().brailleList.getFirst().start, message);	
	}
	
	private void handleUpdateScrollbar(Message message){
		if(message.getValue("sender").equals("braille")){
			getText().positionScrollbar(getBraille().view.getTopIndex());
		}
		else{
			getBraille().positionScrollbar(getText().view.getTopIndex());
		}
	}
	
	private void handleUpdateStyle(Message message){
		if(document.getDOM() != null){
			group.setRedraw(false);
			Element parent = document.getParent(list.getCurrent().n, true);
			message.put("previousStyle", styles.get(styles.getKeyFromAttribute(parent)));
			ArrayList<TextMapElement> itemList = list.findTextMapElements(list.getCurrentIndex(), parent, true);
		
			int start = list.getNodeIndex(itemList.get(0));
			int end = list.getNodeIndex(itemList.get(itemList.size() - 1));
			int currentIndex = list.getCurrentIndex();
			message.put("firstLine", getText().view.getLineAtOffset(itemList.get(0).start));
			
			for(int i = start; i <= end; i++){
				list.setCurrent(i);
				list.getCurrentNodeData(message);
				getText().adjustStyle(this, message, list.getCurrent().n);
				getBraille().adjustStyle(this, message, list.getCurrent());
				if(message.contains("linesBeforeOffset")){
					list.shiftOffsetsFromIndex(list.getCurrentIndex(), (Integer)message.getValue("linesBeforeOffset"), (Integer)message.getValue("linesBeforeOffset"));
					message.remove("linesBeforeOffset");
				}
		
				if(message.contains("linesAfterOffset")){
					list.shiftOffsetsFromIndex(list.getCurrentIndex() + 1, (Integer)message.getValue("linesAfterOffset"),  (Integer)message.getValue("linesAfterOffset"));
					message.remove("linesAfterOffset");
				}
			}
			list.setCurrent(currentIndex);
			document.changeSemanticAction(message, list.getCurrent().parentElement());
			group.setRedraw(true);
		}
	}
	
	public String getFileExt(String fileName) {
		String ext = "";
		String fn = fileName.toLowerCase();
		int dot = fn.lastIndexOf(".");
		if (dot > 0) {
			ext = fn.substring(dot + 1);
		}
		return ext;
	}
	
	public void saveAs(){
		String[] filterNames = new String[] {"XML", "BRF", "UTDML"};
		String[] filterExtensions = new String[] {".xml","*.brf", "*.utd"};
		BBFileDialog dialog = new BBFileDialog(wp.getShell(), SWT.SAVE, filterNames, filterExtensions);
		String filePath = dialog.open();
		
		if(filePath != null){
			checkForUpdatedViews();
			String ext = getFileExt(filePath);
			
			if(ext.equals("brf")){
				if(!this.document.createBrlFile(this, filePath)){
					new Notify("An error has occurred.  Please check your original document");
				}
			}
			else if(ext.equals("xml")){
			    if(fu.createXMLFile(document.getNewXML(), filePath)) {
			    	setTabTitle(filePath);
					documentName = filePath;
			    
			    	String tempSemFile; 			    
				    if(workingFilePath == null)
				    	tempSemFile = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName("outFile.utd") + ".sem";
				    else
				    	tempSemFile = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(workingFilePath) + ".sem";
				    
				    //String tempSemFile = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(workingFilePath) + ".sem"; 
			    	String savedSemFile = fu.getPath(filePath) + BBIni.getFileSep() + fu.getFileName(filePath) + ".sem";   
			    
			    	//Save new semantic file to correct location and temp folder for further editing
			    	copySemanticsFile(tempSemFile, savedSemFile);
			    	copySemanticsFile(tempSemFile, BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(filePath) + ".sem");
			    	
					//update working file path to newly saved file
			    	workingFilePath = filePath;				    
			    }
			    else {
			    	new Notify("An error occured while saving your document.  Please check your original document.");
			    }
			}
			else if(ext.equals("utd")) {				
				document.setOriginalDocType(document.getDOM());
				if(fu.createXMLFile(document.getDOM(), filePath)){
					setTabTitle(filePath);
			    	documentName = filePath;
				    
				    String fileName;
			    	if(workingFilePath == null)
				    	fileName = "outFile";
				    else
				    	fileName = fu.getFileName(workingFilePath);
				    
				    String tempSemFile = BBIni.getTempFilesPath() + BBIni.getFileSep() + fileName + ".sem"; 
			    	String savedTempFile = fu.getPath(filePath) + BBIni.getFileSep() + fu.getFileName(filePath) + ".sem";
				    
			    	copySemanticsFile(tempSemFile, savedTempFile);
			    	copySemanticsFile(tempSemFile, BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(filePath) + ".sem");

			    	workingFilePath = filePath;
				}
				else {
			    	new Notify("An error occured while saving your document.  Please check your original document.");
			    }
			}
		    getText().hasChanged = false;
			getBraille().hasChanged = false;			
		}
	}
	
	private void copySemanticsFile(String tempSemFile, String savedFilePath) {
		if(fu.exists(tempSemFile)){
    		fu.copyFile(tempSemFile, savedFilePath);
    	}
	}
	
	public void fileClose() {
		if (getText().hasChanged || getBraille().hasChanged) {
			YesNoChoice ync = new YesNoChoice(lh.localValue("hasChanged"));
			if (ync.result == SWT.YES) {
				this.fileSave();
			}
		}
		item.dispose();
	}
	
	private void setTabTitle(String pathName) {
		if(pathName != null){
			int index = pathName.lastIndexOf(File.separatorChar);
			if (index == -1) {
				item.setText(pathName);
			} 
			else {
				item.setText(pathName.substring(index + 1));
			}
		}
		else {
			if(docCount == 1){
				item.setText("Untitled");			
			}
			else {
				item.setText("Untitled #" + docCount);
			}
		}
	}
	public void nextElement(){
		if(list.size() != 0){		
			if(getText().view.isFocusControl()){
				getText().increment(this);
				getText().view.setCaretOffset(list.getCurrent().start);
			}
			else if(getBraille().view.isFocusControl()){
				getBraille().increment(this);
				getBraille().view.setCaretOffset(list.getCurrent().brailleList.getFirst().start);
			}
			else {
				Message message = Message.createIncrementMessage();
				dispatch(message);
				getText().view.setCaretOffset(list.getCurrent().start);
				getBraille().view.setCaretOffset(list.getCurrent().brailleList.getFirst().start);
			}
		}
	}
	
	public void prevElement(){
		if(list.size() != 0){
			if(getText().view.isFocusControl()){
				getText().decrement(this);
				getText().view.setCaretOffset(list.getCurrent().start);
			}
			else if(getBraille().view.isFocusControl()){
				getBraille().decrement(this);
				getBraille().view.setCaretOffset(list.getCurrent().brailleList.getFirst().start);
			}
			else {
				Message message = Message.createDecrementMessage();
				dispatch(message);
				getText().view.setCaretOffset(list.getCurrent().start);
				getBraille().view.setCaretOffset(list.getCurrent().brailleList.getFirst().start);
			}
		}
	}
	
	private void resetCursorData(){
		getText().positionFromStart = 0;
		getText().cursorOffset = 0;
		getBraille().positionFromStart = 0;
		getBraille().cursorOffset = 0;
	}
	
	public void textPrint(){
		PrintersManager pn = new PrintersManager(wp.getShell(), getText().view);
		pn.beginPrintJob();	
	}
	
	public void fileEmbossNow() {		
		Shell shell = new Shell(wp.getShell(), SWT.DIALOG_TRIM);
		PrintDialog embosser = new PrintDialog(shell);
		PrinterData data = embosser.open();
		
		if (data == null || data.equals("")) {
			return;
		}
		
		String filePath = BBIni.getTempFilesPath() + BBIni.getFileSep() + "tempBRF.brf";
		if(this.document.createBrlFile(this, filePath)){
			File translatedFile = new File(filePath);
			PrinterDevice embosserDevice;
			try {
				embosserDevice = new PrinterDevice(data.name, true);
				embosserDevice.transmit(translatedFile);
				translatedFile.delete();
			} catch (PrintException e) {
				new Notify(lh.localValue("cannotEmboss") + ": " + data.name + "\n" + e.getMessage());
				logger.log(Level.SEVERE, "Print Exception", e);
			}
		}
	}
	
	public void printPreview(){
		if(getBraille().view.getCharCount() > 0){
			new PrintPreview(this.getDisplay(), document, this);
		}
	}
	
	private void setCurrentOnRefresh(String sender, int offset, boolean isBraille){
		Message m = Message.createSetCurrentMessage(sender, offset, isBraille);
		dispatch(m);
	}
	
	public void refresh(){	
		int currentOffset;
		if(document.getDOM() != null){
			if(getText().view.isFocusControl()){
				currentOffset = getText().view.getCaretOffset();
				resetViews();
				
				if(currentOffset < getText().view.getCharCount()){
					getText().view.setCaretOffset(currentOffset);
				}
				else
					getText().view.setCaretOffset(0);
			
				setCurrentOnRefresh("text",currentOffset, false);
				getText().setPositionFromStart();
				getText().view.setFocus();
			}
			else if(getBraille().view.isFocusControl()){
				currentOffset = getBraille().view.getCaretOffset();
				resetViews();
			
				getBraille().view.setCaretOffset(currentOffset);
				setCurrentOnRefresh("braille",currentOffset, true);	
				getBraille().setPositionFromStart();
				getBraille().view.setFocus();
			}
			else if(treeView.tree.isFocusControl()){	
				if(getText().view.getCaretOffset() > 0)
					currentOffset = getText().view.getCaretOffset();
				else
					currentOffset = list.getCurrent().start;
			
				resetViews();

				setCurrentOnRefresh(null, currentOffset, false);
				getText().view.setCaretOffset(currentOffset);
				getText().setPositionFromStart();
			}
			else {
				currentOffset = getText().view.getCaretOffset();		
				resetViews();		
				setCurrentOnRefresh(null,currentOffset, false);
				getText().view.setCaretOffset(currentOffset);
				getText().setPositionFromStart();
			}
		}
	}
	
	private void resetViews(){
		try {
			boolean textChanged = getText().hasChanged;
			boolean brailleChanged = getBraille().hasChanged;
			
			String path = BBIni.getTempFilesPath() + BBIni.getFileSep() + "temp.xml";
			File f = new File(path);
			f.createNewFile();
			fu.createXMLFile(document.getNewXML(), path);
			list.clearList();
			getText().removeListeners();
			getText().resetView(group);
			getBraille().removeListeners();
			getBraille().resetView(group);
			treeView.removeListeners();
			treeView.resetView(group);
			initializeDocumentTab();
			getText().words = 0;
			updateTempFile();
			document.deleteDOM();
			
			String fileName;
			if(workingFilePath == null)
				fileName = "outFile";
			else
				fileName = fu.getFileName(workingFilePath);
			
			if(fu.exists(BBIni.getTempFilesPath() + BBIni.getFileSep() + fileName + ".sem"))
				initializeAllViews(documentName, path, "semanticFiles " + document.getSemanticFileHandler().getDefaultSemanticsFiles() +"," + BBIni.getTempFilesPath() + BBIni.getFileSep() + fileName + ".sem\n");
			else
				initializeAllViews(documentName, path, null);
			
			f.delete();
	
			getText().hasChanged = textChanged;
			getBraille().hasChanged = brailleChanged;
			
			if(workingFilePath == null && list.size() == 0){
				Nodes n = document.query("/*/*[2]/*[2]/*[1]/*[1]");
				((Element)n.get(0)).appendChild(new Text(""));
				list.add(new TextMapElement(0, 0, n.get(0).getChild(0)));
			}
		} 
		catch (IOException e) {
			new Notify("An error occurred while refreshing the document. Please save your work and try again.");
			e.printStackTrace();
			logger.log(Level.SEVERE, "IO Exception", e);
		}
	}
	
	private void updateTempFile(){
		String tempFile = document.getOutfile();
		if(!fu.createXMLFile(document.getDOM(), tempFile))
		    new Notify("An error occured while saving a temporary file.  Please restart brailleblaster");
	}
	
	private boolean vaildTextElement(String text){
		int length = text.length();
		
		for(int i = 0; i < length; i++){
			if(text.charAt(i) != '\n' && text.charAt(i) != '\t')
				return true;
		}
		
		return false;
	}
	
	public void toggleAttributeEditor(){
		if(!sm.tableIsVisible()){
			treeView.adjustLayout(false);
			if(list.size() == 0){
				sm.displayTable(null);
			}
			else {
				sm.displayTable(list.getCurrent());
			}
			setTabList();
		}
		else {
			treeView.adjustLayout(true);
			sm.hideTable();
			setTabList();
		}
	}
	
	public void closeUntitledTab(){
		document.deleteDOM();
		if(!currentConfig.equals(BBIni.getDefaultConfigFile())){
			currentConfig = BBIni.getDefaultConfigFile();
			document.resetBBDocument(currentConfig);
			styles.resetStyleTable(currentConfig);
			sm.getStyleTable().resetTable(currentConfig);
		}
		treeView.removeListeners();
		treeView.clearTree();
		getText().removeListeners();
		getBraille().removeListeners();
		list.clearList();	
	}
	
	//if tree has focus when opening a document and closing an untitled document, the trees selection must be reset
	public void checkTreeFocus(){
		if(treeView.tree.isFocusControl() && treeView.tree.getSelectionCount() == 0){
			treeView.tree.setSelection(treeView.getRoot());
		}
	}
	
	public void setCurrentConfig(String config){
		if(workingFilePath != null)
			currentConfig = config;
	}
	
	public void checkForUpdatedViews(){
		if(getText().hasChanged)
			getText().update(this);
	}
	
	public void toggleBrailleFont(){
		FontManager.toggleBrailleFont(wp, this);
	}
		
	public StyledText getTextView(){
		return getText().view;
	}
	
	public StyledText getBrailleView(){
		return getBraille().view;
	}
	
	public Display getDisplay(){
		return wp.getShell().getDisplay();
	}
	
	public WPManager getWPManager(){
		return wp;
	}
	
	public Group getGroup(){
		return group;
	}
	
	public String getDocumentName(){
		return documentName;
	}
	
	public String getWorkingPath(){
		return this.workingFilePath;
	}
	
	public BBDocument getDocument(){
		return document;
	}
	
	public String getCurrentConfig(){
		return currentConfig;
	}
	
	public BBSemanticsTable getStyleTable(){
		return styles;
	}

	public TextView getText() {
		return text;
	}

	public BrailleView getBraille() {
		return braille;
	}

	public boolean isSimBrailleDisplayed() {
		return simBrailleDisplayed;
	}

	public void setSimBrailleDisplayed(boolean simBrailleDisplayed) {
		this.simBrailleDisplayed = simBrailleDisplayed;
	}

	public boolean isMetaContent() {
		return metaContent;
	}

	public void setMetaContent(boolean metaContent) {
		this.metaContent = metaContent;
	}
}