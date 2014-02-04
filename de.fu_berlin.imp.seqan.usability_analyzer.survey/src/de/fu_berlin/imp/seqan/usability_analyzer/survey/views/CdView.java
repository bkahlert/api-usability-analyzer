package de.fu_berlin.imp.seqan.usability_analyzer.survey.views;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.bkahlert.devel.nebula.colors.RGB;
import com.bkahlert.devel.nebula.utils.ExecutorUtil;
import com.bkahlert.devel.nebula.widgets.browser.extended.BootstrapEnabledBrowserComposite;
import com.bkahlert.devel.nebula.widgets.browser.extended.ISelector;
import com.bkahlert.devel.nebula.widgets.browser.extended.html.IAnker;
import com.bkahlert.devel.nebula.widgets.browser.listener.AnkerAdapter;
import com.bkahlert.devel.nebula.widgets.browser.listener.IAnkerListener;
import com.bkahlert.nebula.information.ISubjectInformationProvider;
import com.bkahlert.nebula.utils.CompletedFuture;

import de.fu_berlin.imp.seqan.usability_analyzer.core.model.ILocatable;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.data.IBaseDataContainer;
import de.fu_berlin.imp.seqan.usability_analyzer.core.services.DataServiceAdapter;
import de.fu_berlin.imp.seqan.usability_analyzer.core.services.IDataService;
import de.fu_berlin.imp.seqan.usability_analyzer.core.services.IDataServiceListener;
import de.fu_berlin.imp.seqan.usability_analyzer.core.services.IUriPresenterService;
import de.fu_berlin.imp.seqan.usability_analyzer.core.services.location.ILocatorService;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.model.ICode;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.model.IEpisode;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.services.ICodeService;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.services.ICodeServiceListener;
import de.fu_berlin.imp.seqan.usability_analyzer.survey.Activator;
import de.fu_berlin.imp.seqan.usability_analyzer.survey.viewer.CDViewer;
import de.ralfebert.rcputils.menus.ContextMenu;

public class CDView extends ViewPart {

	public static final String ID = "de.fu_berlin.imp.seqan.usability_analyzer.survey.views.CDView";

	public static final Logger LOGGER = Logger.getLogger(CDView.class);

	private final IDataService dataService = (IDataService) PlatformUI
			.getWorkbench().getService(IDataService.class);
	private final IDataServiceListener dataServiceListener = new DataServiceAdapter() {
		@Override
		public void dataDirectoriesLoaded(
				List<? extends IBaseDataContainer> dataContainers) {
			LOGGER.info("Refreshing " + CDView.class.getSimpleName());
			ExecutorUtil.asyncExec(new Runnable() {
				@Override
				public void run() {
					CDView.this.viewer.setInput(Activator.getDefault()
							.getSurveyContainer());
				}
			});
		}
	};

	@SuppressWarnings("unused")
	private final ILocatorService locatorService = (ILocatorService) PlatformUI
			.getWorkbench().getService(ILocatorService.class);

	private final ICodeService codeService = (ICodeService) PlatformUI
			.getWorkbench().getService(ICodeService.class);
	private final ICodeServiceListener codeServiceListener = new ICodeServiceListener() {

		@Override
		public void memoRemoved(URI uri) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void memoModified(URI uri) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void memoAdded(URI uri) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void episodesDeleted(Set<IEpisode> deletedEpisodes) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void episodeReplaced(IEpisode oldEpisode, IEpisode newEpisode) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void episodeAdded(IEpisode episode) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void codesRemoved(List<ICode> removedCodes, List<URI> uris) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void codesAssigned(List<ICode> codes, List<URI> uris) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void codesAdded(List<ICode> codes) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void codeRenamed(ICode code, String oldCaption, String newCaption) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void codeRecolored(ICode code, RGB oldColor, RGB newColor) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void codeMoved(ICode code, ICode oldParentCode,
				ICode newParentCode) {
			CDView.this.viewer.refresh();
		}

		@Override
		public void codeDeleted(ICode code) {
			CDView.this.viewer.refresh();
		}
	};

	private CDViewer viewer = null;
	private BootstrapEnabledBrowserComposite browser = null;

	private final IUriPresenterService informationPresenterService = (IUriPresenterService) PlatformUI
			.getWorkbench().getService(IUriPresenterService.class);

	public CDView() {
		this.dataService.addDataServiceListener(this.dataServiceListener);
		this.codeService.addCodeServiceListener(this.codeServiceListener);
	}

	@Override
	public void dispose() {
		this.informationPresenterService.disable(this.browser);
		this.codeService.removeCodeServiceListener(this.codeServiceListener);
		this.dataService.removeDataServiceListener(this.dataServiceListener);
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		this.browser = new BootstrapEnabledBrowserComposite(parent, SWT.NONE);
		this.browser.deactivateNativeMenu();
		this.browser.setAllowLocationChange(true);
		this.browser.openAboutBlank();

		this.informationPresenterService.enable(this.browser,
				new ISubjectInformationProvider<Control, URI>() {
					private URI hovered = null;

					private final IAnkerListener ankerListener = new AnkerAdapter() {
						@Override
						public void ankerHovered(IAnker anker, boolean entered) {
							URI uri;
							try {
								uri = new URI(anker.getHref());
							} catch (URISyntaxException e1) {
								hovered = null;
								return;
							}

							if (uri.getScheme() != null
									&& !uri.getScheme().contains("-")
									&& entered) {
								try {
									hovered = uri;
								} catch (Exception e) {
									LOGGER.error(e);
								}
							} else {
								hovered = null;
							}
						}
					};

					@Override
					public void register(Control subject) {
						CDView.this.browser
								.addAnkerListener(this.ankerListener);
					}

					@Override
					public void unregister(Control subject) {
						CDView.this.browser
								.removeAnkerListener(this.ankerListener);
					}

					@Override
					public Point getHoverArea() {
						return new Point(20, 10);
					}

					@Override
					public URI getInformation() {
						return this.hovered;
					}
				});

		this.viewer = new CDViewer(this.browser);
		new ContextMenu(this.viewer, this.getSite()) {
			@Override
			protected String getDefaultCommandID() {
				return null;
			}
		};
	}

	@Override
	public void setFocus() {
		this.browser.setFocus();
	}

	/**
	 * Scrolls to the first {@link ILocatable} given.
	 * 
	 * @param URIS
	 * @param callable
	 * @return
	 */
	// TODO auf Viewer setSelection aufrufen
	public Future<ILocatable[]> open(ILocatable[] locatables,
			Callable<ILocatable[]> callable) {
		if (locatables.length > 0) {
			final ILocatable locatable = locatables[0];
			final Future<Boolean> pos = this.browser
					.scrollTo(new ISelector.NameSelector(locatable.getUri()
							.toString()));
			return ExecutorUtil.nonUISyncExec(new Callable<ILocatable[]>() {
				@Override
				public ILocatable[] call() throws Exception {
					pos.get();
					return new ILocatable[] { locatable };
				}
			});
		}
		return new CompletedFuture<ILocatable[]>(new ILocatable[0], null);
	}

}
