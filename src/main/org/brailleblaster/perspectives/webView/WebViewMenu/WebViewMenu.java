package org.brailleblaster.perspectives.webView.WebViewMenu;



import org.brailleblaster.perspectives.Controller;
import org.brailleblaster.perspectives.webView.WebViewController;
import org.brailleblaster.wordprocessor.BBMenu;
import org.brailleblaster.wordprocessor.WPManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * child class of parent class BBMenu which The BBMenu class provides a minimal menu
 * at the top of the SWT shell,it contains several items common across all perspectives 
 * @author smastoureshgh
 * @version 0.0.0
 */
public class WebViewMenu extends BBMenu{
	
	private final int MENU_INDEX = 2;
	WebViewController currentController;
	Menu navigationMenu;
	MenuItem openItem,closeItem,printItem;
	MenuItem brailleView,textView;
	MenuItem navigationRight,navigationLeft;
	MenuItem navItem;
	
	/**
	 * constructor
	 * @param wp : reference to WPManager class
	 * @param controller :reference to my controller class 
	 */
	public WebViewMenu(final WPManager wp, WebViewController idc) {
		super(wp);
		setPerspectiveMenuItem(MENU_INDEX);
		currentController = idc;
		navigationMenu=new Menu(menuBar.getShell(), SWT.DROP_DOWN);
		navItem = new MenuItem(menuBar, SWT.CASCADE);
		navItem.setText("Navigation");
		navItem.setMenu(navigationMenu);
		
		openItem = new MenuItem(fileMenu, SWT.PUSH, 0);
		openItem.setText(lh.localValue("&Open") + "\t" + lh.localValue("Ctrl + O"));
		openItem.setAccelerator(SWT.MOD1 + 'O');
		openItem.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = wp.getFolder().getSelectionIndex();
				if(index == -1){
					wp.addDocumentManager(null);
					currentController = (WebViewController) wp.getList().getFirst(); 
					currentController.fileOpenDialog();
					if(currentController.getWorkingPath() == null){
						currentController.close();
						wp.removeController(currentController);
						currentController = null;
					}
				}
				else {
					currentController.fileOpenDialog();
				}
			}		
		});

		printItem = new MenuItem(fileMenu, SWT.PUSH, 2);
		printItem.setText(lh.localValue("&Print") + "\t" + lh.localValue("Ctrl + P"));
		printItem.setAccelerator(SWT.MOD1 + 'P');
		printItem.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				int count = wp.getFolder().getItemCount();				

				if(count > 0){
					currentController.vb.printPage();
					
				}
						
			}
		});

		closeItem = new MenuItem(fileMenu, SWT.PUSH, 3);
		closeItem.setText(lh.localValue("&Close") + "\t" + lh.localValue("Ctrl + W"));
		closeItem.setAccelerator(SWT.MOD1 + 'W');
		closeItem.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				int count = wp.getFolder().getItemCount();				

				if(count > 0)
					currentController.close();

				//wp.removeController(temp);

				if(wp.getList().size() == 0)
					setCurrent(null);	
			}
		});
		// Add braille view to view menu
		brailleView = new MenuItem(viewMenu, SWT.PUSH, 1);
		brailleView.setText(lh.localValue("&Braille View") + "\t" + lh.localValue("Ctrl + b"));
		brailleView.setAccelerator(SWT.MOD1 + 'b');
		brailleView.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				int count = wp.getFolder().getItemCount();
				if(count > 0){
					currentController.vb.lockBraille=false;
					currentController.vb.showBraille(currentController.vb.index);
					
				}
	
			}
		});
		
		
		// Add braille view to view menu
		textView = new MenuItem(viewMenu, SWT.PUSH, 2);
		textView.setText(lh.localValue("&Text View") + "\t" + lh.localValue("Ctrl + t"));
		textView.setAccelerator(SWT.MOD1 + 't');
		textView.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				int count = wp.getFolder().getItemCount();
				if(count > 0){
					currentController.vb.lockBraille=true;
					currentController.vb.showContents(currentController.vb.index);
					
				}
	
			}
		});
		//Add navigation go to next   page/chapter
		navigationRight = new MenuItem(navigationMenu, SWT.PUSH, 0);
		navigationRight.setText(lh.localValue("&Next") + "\t" + lh.localValue("R"));
		navigationRight.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				currentController.vb.index=currentController.vb.index+1;
				if (currentController.vb.lockBraille==true)
					currentController.vb.showContents(currentController.vb.index);
                else
                	currentController.vb.showBraille(currentController.vb.index);
			
	
			}
		});
		
		
		
		//Add navigation go to previous page/chapter
		navigationLeft = new MenuItem(navigationMenu, SWT.PUSH, 1);
		navigationLeft.setText(lh.localValue("&Previous") + "\t" + lh.localValue("L"));
		navigationLeft.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				currentController.vb.index=currentController.vb.index-1;
				if (currentController.vb.lockBraille==true)
					currentController.vb.showContents(currentController.vb.index);
                else
                	currentController.vb.showBraille(currentController.vb.index);
			
	
			}
		});
		
		
		
		
		
	}

	//public WebViewMenu(WPManager wp, WebViewController controller) {
		//super(wp);
		//setPerspectiveMenuItem(MENU_INDEX);

	//}

	@Override
	public void setCurrent(Controller controller) {
		currentController = (WebViewController)controller;
		
	}

	@Override
	public Controller getCurrent() {
		return currentController;

	}

}
