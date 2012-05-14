package de.fu_berlin.imp.seqan.usability_analyzer.diff.commands;

import org.apache.log4j.Logger;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import com.bkahlert.devel.rcp.selectionUtils.SelectionUtils;

import de.fu_berlin.imp.seqan.usability_analyzer.diff.editors.DiffFileRecordCompareEditorInput;
import de.fu_berlin.imp.seqan.usability_analyzer.diff.model.DiffFileRecord;
import de.fu_berlin.imp.seqan.usability_analyzer.diff.model.DiffFileRecordSegment;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.ui.wizards.WizardUtils;

@SuppressWarnings("restriction")
public class AddCodeInstanceHandler extends AbstractHandler {

	private final Logger log = Logger.getLogger(AddCodeInstanceHandler.class);

	public static void addAnnotation(IMarker marker, ITextSelection selection,
			ITextEditor editor) {
		// The DocumentProvider enables to get the document currently loaded in
		// the editor
		IDocumentProvider idp = editor.getDocumentProvider();
		// This is the document we want to connect to. This is taken from
		// the current editor input.
		IDocument document = idp.getDocument(editor.getEditorInput());
		// The IannotationModel enables to add/remove/change annotation to a
		// Document
		// loaded in an Editor
		IAnnotationModel iamf = idp.getAnnotationModel(editor.getEditorInput());
		// Note: The annotation type id specify that you want to create one of
		// your
		// annotations
		SimpleMarkerAnnotation ma = new SimpleMarkerAnnotation(
				"com.ibm.example.myannotation", marker);
		// Finally add the new annotation to the model
		iamf.connect(document);
		iamf.addAnnotation(ma,
				new Position(selection.getOffset(), selection.getLength()));
		iamf.disconnect(document);
	}

	@SuppressWarnings({ "serial" })
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = SelectionUtils.getSelection();
		if (selection instanceof TextSelection) {
			IEditorPart editor = HandlerUtil.getActiveEditor(event);
			IEditorInput editorInput = editor.getEditorInput();
			if (editorInput instanceof DiffFileRecordCompareEditorInput) {
				DiffFileRecordCompareEditorInput diffFileRecordCompareEditorInput = (DiffFileRecordCompareEditorInput) editorInput;
				DiffFileRecord diffFileRecord = diffFileRecordCompareEditorInput
						.getDiffFileRecord();

				Control focusControl = Display.getCurrent().getFocusControl();
				DiffFileRecord focusDiffFileRecord = null;

				if (focusControl instanceof StyledText) {
					String text = ((StyledText) focusControl).getText();
					String left = diffFileRecord.getPredecessor().getSource();
					String right = diffFileRecord.getSource();
					if (text.equals(left))
						focusDiffFileRecord = diffFileRecord.getPredecessor();
					else if (text.equals(right))
						focusDiffFileRecord = diffFileRecord;
				} else {
					log.error("The control in focus was not of type "
							+ StyledText.class.getSimpleName()
							+ " although this command is only intended to work with one.");
				}

				TextSelection textSeDocument = (TextSelection) selection;
				final int offset = textSeDocument.getOffset();
				final int length = textSeDocument.getLength();
				if (focusDiffFileRecord != null) {
					DiffFileRecordSegment segment = new DiffFileRecordSegment(
							focusDiffFileRecord, offset, length);
					WizardUtils.openAddCodeWizard(segment);
				} else {
					log.error("Could not determine the "
							+ CompareEditor.class.getSimpleName() + " in focus");
				}

				// TODO addAnnotation(new Marker, selection, editor);
			}
		}

		return null;
	}
}