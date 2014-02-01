package de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.ui.information;

import java.net.URI;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.ui.PlatformUI;

import com.bkahlert.nebula.information.extender.EditorInformationControlExtender;

import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.services.CodeServiceException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.services.ICodeService;

public class MemoInformationControlExtender extends
		EditorInformationControlExtender<URI> {

	private static final Logger LOGGER = Logger
			.getLogger(MemoInformationControlExtender.class);

	private ICodeService codeService = (ICodeService) PlatformUI.getWorkbench()
			.getService(ICodeService.class);

	public MemoInformationControlExtender() {
		super(GridDataFactory.fillDefaults().grab(true, true).hint(450, 350)
				.minSize(450, 300));
	}

	@Override
	public String getHtml(URI objectToLoad, IProgressMonitor monitor) {
		return MemoInformationControlExtender.this.codeService
				.loadMemo(objectToLoad);
	}

	@Override
	public void setHtml(URI loadedObject, String html, IProgressMonitor monitor) {
		try {
			MemoInformationControlExtender.this.codeService.setMemo(
					loadedObject, html);
		} catch (CodeServiceException e) {
			LOGGER.error("Error saving memo for " + loadedObject, e);
		}
	}
}
