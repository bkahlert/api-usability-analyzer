package de.fu_berlin.imp.seqan.usability_analyzer.core.handlers;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

import com.bkahlert.devel.rcp.selectionUtils.retriever.SelectionRetrieverFactory;

import de.fu_berlin.imp.seqan.usability_analyzer.core.model.URI;
import de.fu_berlin.imp.seqan.usability_analyzer.core.services.IImportanceService;
import de.fu_berlin.imp.seqan.usability_analyzer.core.services.IImportanceService.Importance;

public class SetImportanceHandler extends AbstractHandler {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger
			.getLogger(SetImportanceHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final List<URI> uris = SelectionRetrieverFactory.getSelectionRetriever(
				URI.class).getSelection();

		Importance importance = Importance
				.valueOf(event
						.getParameter("de.fu_berlin.imp.seqan.usability_analyzer.core.setImportance.importance"));

		IImportanceService importanceService = (IImportanceService) PlatformUI
				.getWorkbench().getService(IImportanceService.class);
		importanceService.setImportance(uris, importance);

		return null;
	}
}