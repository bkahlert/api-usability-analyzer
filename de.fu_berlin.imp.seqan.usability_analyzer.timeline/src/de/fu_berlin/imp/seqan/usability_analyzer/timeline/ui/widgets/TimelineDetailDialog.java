package de.fu_berlin.imp.seqan.usability_analyzer.timeline.ui.widgets;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

import com.bkahlert.devel.nebula.widgets.RoundedComposite;
import com.bkahlert.devel.nebula.widgets.SimpleIllustratedComposite;
import com.bkahlert.devel.nebula.widgets.SimpleIllustratedComposite.IllustratedText;
import com.bkahlert.devel.nebula.widgets.timeline.ITimeline;

import de.fu_berlin.imp.seqan.usability_analyzer.core.model.TimeZoneDate;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.TimeZoneDateRange;
import de.fu_berlin.imp.seqan.usability_analyzer.core.ui.viewer.filters.HasDateRange;
import de.fu_berlin.imp.seqan.usability_analyzer.core.util.SWTUtil;
import de.fu_berlin.imp.seqan.usability_analyzer.timeline.Activator;
import de.fu_berlin.imp.seqan.usability_analyzer.timeline.extensionProviders.DefaultTimelineEventDetailProvider;
import de.fu_berlin.imp.seqan.usability_analyzer.timeline.extensionProviders.ITimelineEventDetailProvider;

public class TimelineDetailDialog extends Dialog {

	private static final Logger LOGGER = Logger
			.getLogger(TimelineDetailDialog.class.getSimpleName());

	private static final int borderWidth = 5;

	private static class UnknownTimelineEventDetailProvider extends
			DefaultTimelineEventDetailProvider<Object> implements
			ITimelineEventDetailProvider<Object> {
		@Override
		public Class<Object> getType() {
			return Object.class;
		}

		@Override
		public List<IllustratedText> getMetaInformation(Object data) {
			return Arrays.asList(new IllustratedText("UNKNOWN"));
		}
	}

	private ITimelineEventDetailProvider<Object> defaultTimelineEventDetailProvider = new UnknownTimelineEventDetailProvider();
	private ITimeline timeline;
	private Object event;

	private Composite composite;

	private Composite metaComposite;
	private Composite detailComposite;
	private Composite customComposite;

	public TimelineDetailDialog(Shell parentShell, ITimeline timeline) {
		super(parentShell);
		// TODO nicht modal bekommen durch auskommentieren; allerdings gerät
		// dann das Fenster beim gleichzeitigen Laden eines DiffCompareEditors
		// in den Hintergrund
		// this.setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK);
		this.timeline = timeline;
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		return new Point(100, 70);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		this.composite = (Composite) super.createDialogArea(parent);
		this.composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = borderWidth;
		gridLayout.marginHeight = borderWidth;
		gridLayout.horizontalSpacing = borderWidth;
		this.composite.setLayout(gridLayout);

		this.metaComposite = new RoundedComposite(this.composite, SWT.BORDER);
		this.metaComposite.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM,
				false, false, 1, 1));
		this.metaComposite.setBackground(SWTResourceManager
				.getColor(SWT.COLOR_LIST_BACKGROUND));
		this.metaComposite.setLayout(RowLayoutFactory.fillDefaults()
				.margins(7, 3).type(SWT.VERTICAL).spacing(3).create());

		this.customComposite = new Composite(this.composite, SWT.NONE);
		this.customComposite.setLayoutData(GridDataFactory.swtDefaults()
				.span(1, 2).create());
		this.customComposite.setLayout(new FillLayout());
		this.customComposite.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TimelineDetailDialog.this.close();
			}
		});

		this.detailComposite = new RoundedComposite(this.composite, SWT.BORDER);
		this.detailComposite.setBackground(SWTResourceManager
				.getColor(SWT.COLOR_LIST_BACKGROUND));
		this.detailComposite.setLayout(new GridLayout(2, false));
		this.detailComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				false, false, 1, 1));

		this.composite.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_RIGHT) {
					TimelineDetailDialog.this.nextScreenshot();
				} else if (e.keyCode == SWT.ARROW_LEFT) {
					TimelineDetailDialog.this.prevScreenshot();
				} else if (e.keyCode == SWT.ESC || e.keyCode == SWT.CR) {
					TimelineDetailDialog.this.close();
				}
			}
		});

		return this.composite;
	}

	public void load(Object event) {
		if (event == null) {
			return;
		}

		this.event = event;

		ITimelineEventDetailProvider<Object> responsibleTimelineEventDetailProvider = null;
		for (ITimelineEventDetailProvider<Object> timelineEventDetailProvider : Activator
				.getRegisteredTimelineEventDetailProviders()) {
			if (timelineEventDetailProvider.getType().isInstance(this.event)) {
				responsibleTimelineEventDetailProvider = timelineEventDetailProvider;
				break;
			}
		}

		if (responsibleTimelineEventDetailProvider == null) {
			LOGGER.warn("Could not find any compatible "
					+ ITimelineEventDetailProvider.class.getSimpleName()
					+ " for " + this.event);
			responsibleTimelineEventDetailProvider = this.defaultTimelineEventDetailProvider;
		}

		List<IllustratedText> metaInformation = responsibleTimelineEventDetailProvider
				.getMetaInformation(event);
		List<Entry<String, String>> detailInformation = responsibleTimelineEventDetailProvider
				.getDetailInformation(event);
		Color backgroundColor = responsibleTimelineEventDetailProvider
				.getBackground(event, this.timeline);

		this.composite.setBackground(backgroundColor);
		this.loadMetaInformation(metaInformation);
		this.loadDetailInformation(detailInformation);

		SWTUtil.clearControl(this.customComposite);
		responsibleTimelineEventDetailProvider.fillCustomComposite(
				this.customComposite, event, this.timeline);
		this.customComposite.layout();

		((Composite) this.getContents()).layout();
		Shell shell = this.getShell();
		shell.pack();
	}

	/**
	 * Centers the timeline so that is shows the start of the given end.
	 * <p>
	 * If the event has no start the end is used.
	 * <p>
	 * If the event is null or has no start nor end date, nothing happens.
	 * 
	 * @param event
	 */
	public void centerOnEvent(Object event) {
		if (event instanceof HasDateRange) {
			TimeZoneDateRange dateRange = ((HasDateRange) event).getDateRange();
			TimeZoneDate center = dateRange != null ? dateRange.getStartDate() != null ? dateRange
					.getStartDate() : dateRange.getEndDate()
					: null;
			if (center != null) {
				this.timeline.setCenterVisibleDate(center.getCalendar());
			}
		}
	}

	public void loadMetaInformation(List<IllustratedText> metaInformation) {
		SWTUtil.clearControl(this.metaComposite);
		for (IllustratedText metaEntry : metaInformation) {
			SimpleIllustratedComposite metaCompositeEntry = new SimpleIllustratedComposite(
					this.metaComposite, SWT.CENTER | SWT.BOLD);
			metaCompositeEntry
					.setBackground(this.metaComposite.getBackground());
			metaCompositeEntry.setSpacing(3);
			metaCompositeEntry.setContent(metaEntry);
		}
		this.metaComposite.layout();
	}

	public void loadDetailInformation(
			List<Entry<String, String>> detailInformation) {
		SWTUtil.clearControl(this.detailComposite);
		for (Entry<String, String> detailEntry : detailInformation) {
			Label keyLabel = new Label(this.detailComposite, SWT.NONE);
			keyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			keyLabel.setText(detailEntry.getKey());

			Label valueLabel = new Label(this.detailComposite, SWT.NONE);
			valueLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false,
					false));
			valueLabel.setText(detailEntry.getValue());
		}
		this.detailComposite.layout();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		this.createButton(parent, IDialogConstants.BACK_ID,
				IDialogConstants.BACK_LABEL, false).addSelectionListener(
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						TimelineDetailDialog.this.prevScreenshot();
					}
				});
		this.createButton(parent, IDialogConstants.NEXT_ID,
				IDialogConstants.NEXT_LABEL, false).addSelectionListener(
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						TimelineDetailDialog.this.nextScreenshot();
					}
				});
		// TODO
		// createButton(parent, IDialogConstants.OPEN_ID, "Open URL", false)
		// .addSelectionListener(new SelectionAdapter() {
		// @Override
		// public void widgetSelected(SelectionEvent e) {
		// openURL(currentData.getUrl());
		// }
		// });
		this.createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
	}

	public void nextScreenshot() {
		Object successor = this.timeline.getSuccessor(this.event);
		this.load(successor);
		this.centerOnEvent(this.event);
	}

	public void prevScreenshot() {
		Object predecessor = this.timeline.getPredecessor(this.event);
		this.load(predecessor);
		this.centerOnEvent(this.event);
	}

	public static void openURL(String url) {
		if (url != null) {
			org.eclipse.swt.program.Program.launch(url);
		}
	}

}
