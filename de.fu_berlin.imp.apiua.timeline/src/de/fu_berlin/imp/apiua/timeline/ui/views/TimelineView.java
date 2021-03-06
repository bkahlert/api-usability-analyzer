package de.fu_berlin.imp.apiua.timeline.ui.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.bkahlert.nebula.datetime.CalendarRange;
import com.bkahlert.nebula.utils.ExecUtils;
import com.bkahlert.nebula.utils.NamedJob;
import com.bkahlert.nebula.utils.selection.ArrayUtils;
import com.bkahlert.nebula.viewer.timeline.impl.TimelineGroupViewer;
import com.bkahlert.nebula.viewer.timeline.provider.atomic.ITimelineLabelProvider;
import com.bkahlert.nebula.viewer.timeline.provider.complex.IBandGroupProvider;
import com.bkahlert.nebula.viewer.timeline.provider.complex.ITimelineProvider;
import com.bkahlert.nebula.viewer.timeline.provider.complex.ITimelineProviderFactory;
import com.bkahlert.nebula.viewer.timeline.provider.complex.impl.BandGroupProvider;
import com.bkahlert.nebula.viewer.timeline.provider.complex.impl.TimelineProvider;
import com.bkahlert.nebula.widgets.timeline.ITimeline;
import com.bkahlert.nebula.widgets.timeline.ITimelineFactory;
import com.bkahlert.nebula.widgets.timelinegroup.impl.TimelineGroup;

import de.fu_berlin.imp.apiua.core.model.identifier.IIdentifier;
import de.fu_berlin.imp.apiua.core.services.IHighlightService;
import de.fu_berlin.imp.apiua.core.services.IHighlightServiceListener;
import de.fu_berlin.imp.apiua.core.services.IWorkSession;
import de.fu_berlin.imp.apiua.core.services.IWorkSessionListener;
import de.fu_berlin.imp.apiua.core.services.IWorkSessionService;
import de.fu_berlin.imp.apiua.timeline.Activator;
import de.fu_berlin.imp.apiua.timeline.extensionProviders.ITimelineBandProvider;
import de.fu_berlin.imp.apiua.timeline.preferences.SUATimelinePreferenceUtil;
import de.fu_berlin.imp.apiua.timeline.ui.widgets.InformationPresentingTimeline;
import de.fu_berlin.imp.apiua.timeline.ui.widgets.TimelineLabelProvider;

public class TimelineView extends ViewPart {

	public static final String ID = "de.fu_berlin.imp.apiua.timeline.ui.views.TimelineView";
	public static final Logger LOGGER = Logger.getLogger(TimelineView.class);

	private NamedJob timelineLoader = null;

	private final IWorkSessionService workSessionService;
	private final IWorkSessionListener workSessionListener = new IWorkSessionListener() {

		private Set<IIdentifier> filterValidIdentifiers(
				Set<IIdentifier> identifiers) {
			Set<IIdentifier> filteredKeys = new HashSet<IIdentifier>();
			identifierLoop: for (IIdentifier key : identifiers) {
				for (ITimelineBandProvider<IIdentifier> timelineBandProvider : Activator
						.<IIdentifier> getRegisteredTimelineBandProviders()) {
					if (!timelineBandProvider.getContentProvider().isValid(key)) {
						continue identifierLoop;
					}
				}
				filteredKeys.add(key);
			}
			return filteredKeys;
		}

		@Override
		public void workSessionStarted(IWorkSession workSession) {
			final Set<IIdentifier> keys = new HashSet<IIdentifier>();
			keys.addAll(ArrayUtils.getAdaptableObjects(
					workSession.getEntities(), IIdentifier.class));
			final Set<IIdentifier> filteredKeys = this
					.filterValidIdentifiers(keys);

			TimelineView.this.open(filteredKeys);
			ExecUtils.asyncExec(new Runnable() {
				@Override
				public void run() {
					TimelineView.this.setPartName(StringUtils.join(
							filteredKeys, ", "));
				}
			});
		}
	};

	private final IHighlightService highlightService;
	private final IHighlightServiceListener highlightServiceListener = new IHighlightServiceListener() {
		@Override
		public void highlight(Object sender, CalendarRange[] ranges,
				boolean moveInsideViewport) {
			if (TimelineView.this.openedIdentifiers == null
					|| TimelineView.this.openedIdentifiers.size() == 0) {
				return;
			}
			Map<IIdentifier, CalendarRange[]> groupedRanges = new HashMap<IIdentifier, CalendarRange[]>();
			for (IIdentifier loadedIdentifier : TimelineView.this.openedIdentifiers) {
				groupedRanges.put(loadedIdentifier, ranges);
			}
			this.highlight(sender, groupedRanges, moveInsideViewport);
		}

		@Override
		public void highlight(Object sender,
				final Map<IIdentifier, CalendarRange[]> groupedRanges,
				final boolean moveInsideViewport) {
			if (sender == TimelineView.this) {
				return;
			}

			final Map<IIdentifier, Calendar> centeredDates = new HashMap<IIdentifier, Calendar>();
			for (IIdentifier key : groupedRanges.keySet()) {
				Calendar centeredDate = CalendarRange.calculateOuterDateRange(
						groupedRanges.get(key)).getStartDate();
				centeredDates.put(key, centeredDate);
			}

			if (groupedRanges.keySet().size() == 0) {
				return;
			}

			if (TimelineView.this.timelineLoader != null) {
				TimelineView.this.timelineLoader.cancel();
			}

			if (TimelineView.this.timelineGroupViewer != null) {
				TimelineView.this.timelineLoader = new NamedJob(
						TimelineView.class, "Updating "
								+ ITimeline.class.getSimpleName()) {
					@Override
					protected IStatus runNamed(IProgressMonitor monitor) {
						SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
						if (!groupedRanges.isEmpty()) {
							if (moveInsideViewport) {
								TimelineView.this.timelineGroupViewer
										.setCenterVisibleDate(centeredDates,
												subMonitor.newChild(1));
								TimelineView.this.timelineGroupViewer
										.highlight(groupedRanges,
												subMonitor.newChild(1));
							} else {
								TimelineView.this.timelineGroupViewer
										.highlight(groupedRanges,
												subMonitor.newChild(2));
							}
						}
						subMonitor.done();
						return Status.OK_STATUS;
					}
				};
			}
			TimelineView.this.timelineLoader.schedule();
		}
	};

	private TimelineGroup<InformationPresentingTimeline, IIdentifier> timelineGroup;
	private TimelineGroupViewer<InformationPresentingTimeline, IIdentifier> timelineGroupViewer;

	private Set<IIdentifier> openedIdentifiers = null;

	public TimelineView() {
		this.workSessionService = (IWorkSessionService) PlatformUI
				.getWorkbench().getService(IWorkSessionService.class);
		if (this.workSessionService == null) {
			LOGGER.warn("Could not get "
					+ IWorkSessionService.class.getSimpleName());
		}

		this.highlightService = (IHighlightService) PlatformUI.getWorkbench()
				.getService(IHighlightService.class);
		if (this.highlightService == null) {
			LOGGER.warn("Could not get "
					+ IHighlightService.class.getSimpleName());
		}
	}

	/**
	 * Initializes and opens {@link ITimeline}s using an {@link TimelineGroup}.
	 * <p>
	 * Existing {@link DecoratableTimeline}s are recycled. New
	 * {@link DecoratableTimeline} s will be created if necessary. If free
	 * {@link DecoratableTimeline}s stay unused they will be disposed.
	 * 
	 * @param identifiers
	 */
	public void open(final Set<IIdentifier> identifiers) {
		if (this.timelineLoader != null) {
			this.timelineLoader.cancel();
		}

		this.openedIdentifiers = identifiers;

		this.saveStates();

		if (this.timelineGroupViewer != null) {
			this.timelineLoader = new NamedJob(TimelineView.class, "Loading "
					+ ITimeline.class.getSimpleName()) {
				@Override
				protected IStatus runNamed(IProgressMonitor monitor) {
					TimelineView.this.timelineGroupViewer.setInput(identifiers);
					TimelineView.this.timelineGroupViewer.refresh(monitor);
					monitor.done();
					return Status.OK_STATUS;
				}
			};
			this.timelineLoader.schedule();
		}
	}

	private void saveStates() {
		// must be called synchronously; switching to another thread
		// would allow Eclipse to close completely before we
		// accessed the widgets
		try {
			ExecUtils.syncExec(new Runnable() {
				@Override
				public void run() {
					if (TimelineView.this.timelineGroup != null
							&& !TimelineView.this.timelineGroup.isDisposed()) {
						final Set<IIdentifier> inputs = TimelineView.this.timelineGroup
								.getTimelineKeys();
						for (final IIdentifier identifier : inputs) {
							try {
								ITimeline timeline = TimelineView.this.timelineGroup
										.getTimeline(identifier);
								Calendar centerVisibleDate = timeline
										.getCenterVisibleDate().get();
								Integer zoomIndex = timeline.getZoomIndex()
										.get();

								SUATimelinePreferenceUtil util = new SUATimelinePreferenceUtil();
								if (centerVisibleDate != null) {
									util.setCenterStartDate(identifier,
											centerVisibleDate);
								}
								if (zoomIndex != null) {
									util.setZoomIndex(identifier, zoomIndex);
								}
							} catch (Exception e) {
								LOGGER.error("Error saving state of "
										+ ITimeline.class.getSimpleName() + " "
										+ identifier, e);
							}
						}
					}
				}
			});
		} catch (Exception e) {
			LOGGER.error("Error saving timeline states", e);
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (this.workSessionService != null) {
			this.workSessionService
					.addWorkSessionListener(this.workSessionListener);
		}
		if (this.highlightService != null) {
			this.highlightService
					.addHighlightServiceListener(this.highlightServiceListener);
		}
	}

	@Override
	public void dispose() {
		if (this.highlightService != null) {
			this.highlightService
					.removeHighlightServiceListener(this.highlightServiceListener);
		}
		if (this.workSessionService != null) {
			this.workSessionService
					.removeWorkSessionListener(this.workSessionListener);
		}
		super.dispose();
	}

	private static class TimelineProviderFactory
			implements
			ITimelineProviderFactory<InformationPresentingTimeline, IIdentifier> {
		@Override
		public ITimelineProvider<InformationPresentingTimeline, IIdentifier> createTimelineProvider() {

			ITimelineProvider<InformationPresentingTimeline, IIdentifier> timelineProvider;
			ITimelineLabelProvider<InformationPresentingTimeline> timelineLabelProvider = new TimelineLabelProvider<InformationPresentingTimeline>();

			List<IBandGroupProvider<IIdentifier>> bandGroupProviders = new ArrayList<IBandGroupProvider<IIdentifier>>();
			for (ITimelineBandProvider<IIdentifier> bandProvider : Activator
					.<IIdentifier> getRegisteredTimelineBandProviders()) {
				bandGroupProviders.add(new BandGroupProvider<IIdentifier>(
						bandProvider.getContentProvider(), bandProvider
								.getBandLabelProvider(), bandProvider
								.getEventLabelProvider()));
			}
			timelineProvider = new TimelineProvider<InformationPresentingTimeline, IIdentifier>(
					timelineLabelProvider, bandGroupProviders);
			return timelineProvider;
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		ITimelineFactory<InformationPresentingTimeline> timelineFactory = new ITimelineFactory<InformationPresentingTimeline>() {
			@Override
			public InformationPresentingTimeline createTimeline(
					Composite parent, int style) {
				return new InformationPresentingTimeline(parent, style);
			}
		};

		TimelineProviderFactory timelineProviderFactory = new TimelineProviderFactory();
		this.timelineGroup = new TimelineGroup<InformationPresentingTimeline, IIdentifier>(
				parent, SWT.NONE, timelineFactory);
		this.timelineGroup.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				TimelineView.this.saveStates();
			}
		});

		MenuManager menuManager = new MenuManager("#PopupMenu");
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new Separator(
						IWorkbenchActionConstants.MB_ADDITIONS));
			}
		});
		Menu menu = menuManager.createContextMenu(this.timelineGroup);
		this.timelineGroup.setMenu(menu);

		this.timelineGroupViewer = new TimelineGroupViewer<InformationPresentingTimeline, IIdentifier>(
				this.timelineGroup, timelineProviderFactory);

		this.getSite().registerContextMenu(menuManager,
				this.timelineGroupViewer);
		this.getSite().setSelectionProvider(this.timelineGroupViewer);
	}

	@Override
	public void setFocus() {
		if (this.timelineGroup != null) {
			this.timelineGroup.setFocus();
		}
	}

	/**
	 * Returns the {@link InformationPresentingTimeline} currently displaying
	 * content associated with the given {@link IIdentifier}.
	 * 
	 * @param key
	 * @return
	 * 
	 * @thread.ui must be run in the UI thread
	 */
	public InformationPresentingTimeline getTimeline(IIdentifier key) {
		if (this.timelineGroup != null && !this.timelineGroup.isDisposed()) {
			return this.timelineGroup.getTimeline(key);
		}
		return null;
	}

}
