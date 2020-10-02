package de.tud.se.c2abs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.ui.CDTUITools;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class C2ABSView extends ViewPart {

	/**
	 * Returns as List all the translation units for the given project.
	 * 
	 * @param cproject
	 *            the current C/C++ project
	 */
	public static List<ITranslationUnit> getProjectTranslationUnits(ICProject cproject) {
		List<ITranslationUnit> tuList = new ArrayList<ITranslationUnit>();

		// get source folders
		try {
			for (ISourceRoot sourceRoot : cproject.getSourceRoots()) {
				// get all elements
				for (ICElement element : sourceRoot.getChildren()) {
					// if it is a container (i.e., a source folder)
					if (element.getElementType() == ICElement.C_CCONTAINER) {
						recursiveContainerTraversal((ICContainer) element, tuList);
					} else {
						ITranslationUnit tu = (ITranslationUnit) element;
						tuList.add(tu);
					}
				}
			}
		} catch (CModelException e) {
			e.printStackTrace();
		}
		return tuList;
	}

	private static void recursiveContainerTraversal(ICContainer container, List<ITranslationUnit> tuList)
			throws CModelException {
		for (ICContainer inContainer : container.getCContainers()) {
			recursiveContainerTraversal(inContainer, tuList);
		}

		for (ITranslationUnit tu : container.getTranslationUnits()) {
			tuList.add(tu);
		}
	}

	private IEditorPart part;
	private ITranslationUnit tu;
	private Text txtMyText;

	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 8;
		container.setLayout(gridLayout);
		// buttons
		Composite buttons = new Composite(container, SWT.NONE);
		gridLayout = new GridLayout(4, true);
		gridLayout.verticalSpacing = 80;
		buttons.setLayout(gridLayout);

		Button translateEditorBtn = new Button(buttons, SWT.PUSH);
		translateEditorBtn.setText("Translate Editor");
		translateEditorBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadEditor(getActiveEditor());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		Button translateAllBtn = new Button(buttons, SWT.PUSH);
		translateAllBtn.setText("Translate All");
		translateAllBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				translateAll();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		Button increaseFontBtn = new Button(buttons, SWT.PUSH);
		increaseFontBtn.setText("Increase Font Size");
		increaseFontBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				increaseFontSize();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		Button decreaseFontBtn = new Button(buttons, SWT.PUSH);
		decreaseFontBtn.setText("Decrease Font Size");
		decreaseFontBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				decreaseFontSize();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});
		
		txtMyText = new Text(container, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		// set up key shortcuts to translate the editor and increase/decrease the text
		// size from the text box
		txtMyText.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.stateMask & SWT.CTRL) != 0) {
					switch (e.keyCode) {
					case 't':
						loadEditor(getActiveEditor());
						break;
					case '+':
						increaseFontSize();
						break;
					case '-':
						decreaseFontSize();
						break;
					}
				}
			}
		});

		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		gridData.horizontalSpan = 6;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;

		txtMyText.setLayoutData(gridData);
	}

	@Override
	public void setFocus() {

	}

	private void translateAll() {
		try {
			for (ICProject cproject : CoreModel.getDefault().getCModel().getCProjects()) {
				for (ITranslationUnit tu : getProjectTranslationUnits(cproject)) {
					final C2ABS c2abs = new C2ABS(tu.getAST(), null);
					final String s = c2abs.parse();
					System.out.println("\n" + tu.getElementName() + " : " + s.startsWith("module"));
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void loadEditor(IEditorPart activeEditor) {
		if (activeEditor == null)
			return;

		try {
			part = activeEditor;

			// Access translation unit of the editor.
			setTranslationUnit((ITranslationUnit) CDTUITools.getEditorInputCElement(part.getEditorInput()));

			final C2ABS c2abs = new C2ABS(tu.getAST(), null);
			txtMyText.setText(c2abs.parse());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	protected IEditorPart getActiveEditor() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	}

	public void setTranslationUnit(ITranslationUnit tu) {
		this.tu = tu;
	}

	private void decreaseFontSize() {
		Font oldFont = txtMyText.getFont();
		FontData oldFontData = oldFont.getFontData()[0];
		FontData newFontData = new FontData(oldFontData.getName(), oldFontData.getHeight() - 4, oldFontData.getStyle());
		Font newFont = new Font(oldFont.getDevice(), newFontData);
		txtMyText.setFont(newFont);
	}

	private void increaseFontSize() {
		Font oldFont = txtMyText.getFont();
		FontData oldFontData = oldFont.getFontData()[0];
		FontData newFontData = new FontData(oldFontData.getName(), oldFontData.getHeight() + 4, oldFontData.getStyle());
		Font newFont = new Font(oldFont.getDevice(), newFontData);
		txtMyText.setFont(newFont);
	}
}
