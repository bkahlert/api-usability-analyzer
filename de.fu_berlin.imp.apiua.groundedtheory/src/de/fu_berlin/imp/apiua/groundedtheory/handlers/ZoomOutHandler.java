package de.fu_berlin.imp.apiua.groundedtheory.handlers;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import de.fu_berlin.imp.apiua.groundedtheory.views.AxialCodingView;

public class ZoomOutHandler extends AbstractHandler {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(ZoomOutHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchPart activePart = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getPartService().getActivePart();
		if (activePart instanceof AxialCodingView) {
			AxialCodingView view = (AxialCodingView) activePart;
			view.zoomOutFocussedACM();
		}

		return null;
	}
}
