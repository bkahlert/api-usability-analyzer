package de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.handlers;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.bkahlert.devel.rcp.selectionUtils.retriever.SelectionRetrieverFactory;

import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.model.ICode;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.viewer.CodeViewer;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.views.CodeView;

public class RenameCodeHandler extends AbstractHandler {

	private static final Logger LOGGER = Logger
			.getLogger(RenameCodeHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchPart part = HandlerUtil.getActivePart(event);

		List<ICode> codes = SelectionRetrieverFactory.getSelectionRetriever(
				ICode.class).getSelection();

		if (codes.size() > 0) {
			if (part instanceof CodeView) {
				CodeView codeView = (CodeView) part;
				CodeViewer codeViewer = codeView.getCodeViewer();
				codeViewer.getViewer().editElement(codes.get(0), 0);
			}
		} else {
			LOGGER.warn("Selection did not contain any "
					+ ICode.class.getSimpleName());
		}

		return null;
	}

}
