package org.brailleblaster.printers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.brailleblaster.BBIni;
import org.brailleblaster.abstractClasses.AbstractView;
import org.brailleblaster.util.Notify;
import org.brailleblaster.wordprocessor.BBDocument;
import org.brailleblaster.wordprocessor.FontManager;
import org.brailleblaster.wordprocessor.Message;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class PrintPreview {
	Shell shell;
	BBDocument doc;
	PreviewText previewText;
	
	final int MARGINS = 38;
	final int PAGE_WIDTH = 619;
	final int PAGE_HEIGHT = 825;
	
	private class PreviewText extends AbstractView{
		String text;
		File f;
		
		public PreviewText(Group group){
			super(group, 0, 100, 0, 100);
			this.view.setEditable(false);
		}

		public void setPreviewText(BBDocument doc){
			String tempFilePath = BBIni.getTempFilesPath() + BBIni.getFileSep() + "tempBRF.brf"; 
			doc.createBrlFile(tempFilePath);
			this.f = new File(tempFilePath);
			try {
				Scanner scanner = new Scanner(this.f);
				this.text = scanner.useDelimiter("\\Z").next();
				scanner.close();
				this.view.setText(this.text);
				Font font = FontManager.getFont();
				this.view.setFont(font);
				this.view.setMargins(MARGINS, MARGINS, MARGINS, MARGINS);
				this.view.getShell().open();
			} catch (FileNotFoundException e) {
				new Notify("Print Preview failed to open properly.  Check to ensure your document does not contain errors");
				e.printStackTrace();
				this.f.delete();
				this.view.getShell().dispose();
			}
		}
		
		public void deleteTempFile(){
			this.f.delete();
		}
		
		@Override
		protected void setViewData(Message message) {
			// TODO Auto-generated method stub		
		}

		@Override
		public void resetView() {
			// TODO Auto-generated method stub		
		}
	}
	
	public PrintPreview(Display display, BBDocument doc){
		this.doc = doc;
		
		this.shell = new Shell(display, SWT.SHELL_TRIM);
		this.shell.setLayout(new FormLayout());
		this.shell.setText("Print Preview");
		this.shell.setSize(PAGE_WIDTH, PAGE_HEIGHT);
		
		Group gp = new Group(this.shell, SWT.NONE);
		FormData location = new FormData();
	    location.left = new FormAttachment(0);
	    location.right = new FormAttachment(100);
	    location.top = new FormAttachment (0);
	    location.bottom = new FormAttachment(100);
	    gp.setLayoutData (location);
		gp.setLayout(new FormLayout());
		
		this.previewText = new PreviewText(gp);
		
		previewText.view.addVerifyKeyListener(new VerifyKeyListener(){
			@Override
			public void verifyKey(VerifyEvent e) {
				if(e.stateMask == SWT.MOD1 && e.keyCode == 'q'){
					previewText.deleteTempFile();
					shell.dispose();
					e.doit = false;
				}
			}
		});
		
		this.shell.addListener(SWT.Close, new Listener() { 
			public void handleEvent(Event event) { 
				previewText.deleteTempFile();
				shell.dispose();
	        } 
	     });	
		
		this.previewText.setPreviewText(this.doc);	
	}
}