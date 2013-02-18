package de.fu_berlin.imp.seqan.usability_analyzer.diff.timeline;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import com.bkahlert.devel.nebula.viewer.timeline.provider.atomic.ITimelineBandLabelProvider;
import com.bkahlert.devel.nebula.viewer.timeline.provider.atomic.ITimelineContentProvider;
import com.bkahlert.devel.nebula.viewer.timeline.provider.atomic.ITimelineEventLabelProvider;
import com.bkahlert.devel.nebula.widgets.timeline.TimelineHelper;

import de.fu_berlin.imp.seqan.usability_analyzer.core.model.ID;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.TimeZoneDateRange;
import de.fu_berlin.imp.seqan.usability_analyzer.diff.Activator;
import de.fu_berlin.imp.seqan.usability_analyzer.diff.model.Diff;
import de.fu_berlin.imp.seqan.usability_analyzer.diff.model.DiffList;
import de.fu_berlin.imp.seqan.usability_analyzer.diff.model.DiffRecord;
import de.fu_berlin.imp.seqan.usability_analyzer.diff.model.IDiff;
import de.fu_berlin.imp.seqan.usability_analyzer.diff.ui.DiffLabelProvider;
import de.fu_berlin.imp.seqan.usability_analyzer.timeline.extensionProviders.ITimelineBandProvider;

public class DiffTimelineBandProvider implements ITimelineBandProvider {

	private enum BANDS {
		DIFF_BAND, DIFFRECORD_BAND
	}

	@Override
	public ITimelineContentProvider getContentProvider() {
		return new ITimelineContentProvider() {

			private Object input = null;

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				this.input = newInput;
			}

			@Override
			public boolean isValid(Object key) {
				if (key instanceof ID) {
					return Activator.getDefault().getDiffDataContainer()
							.getIDs().contains(key);
				}
				return false;
			}

			@Override
			public Object[] getBands(IProgressMonitor monitor) {
				return new Object[] { BANDS.DIFF_BAND, BANDS.DIFFRECORD_BAND };
			}

			@Override
			public Object[] getEvents(Object band, IProgressMonitor monitor) {
				SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
				if (!(band instanceof BANDS)) {
					subMonitor.done();
					return new Object[0];
				}

				final DiffList diffList = (this.input instanceof ID) ? de.fu_berlin.imp.seqan.usability_analyzer.diff.Activator
						.getDefault().getDiffDataContainer()
						.getDiffFiles((ID) this.input, subMonitor.newChild(1))
						: null;

				switch ((BANDS) band) {
				case DIFF_BAND:
					IDiff[] diffs = diffList.toArray(new IDiff[0]);
					monitor.worked(1);
					return diffs;
				case DIFFRECORD_BAND:
					List<DiffRecord> diffRecords = new ArrayList<DiffRecord>();
					for (Diff diff : diffList) {
						for (DiffRecord diffRecord : diff.getDiffFileRecords()) {
							diffRecords.add(diffRecord);
						}
					}
					monitor.worked(1);
					return diffRecords.toArray();
				}

				return new Object[0];
			}
		};
	}

	@Override
	public ITimelineBandLabelProvider getBandLabelProvider() {
		return new ITimelineBandLabelProvider() {

			@Override
			public String getTitle(Object band) {
				if (band instanceof BANDS) {
					switch ((BANDS) band) {
					case DIFF_BAND:
						return "Development";
					case DIFFRECORD_BAND:
						return "Sources";
					}
				}
				return null;
			}

			@Override
			public Boolean isShowInOverviewBands(Object band) {
				if (band instanceof BANDS) {
					switch ((BANDS) band) {
					case DIFF_BAND:
						return true;
					case DIFFRECORD_BAND:
						return true;
					}
				}
				return null;
			}

			@Override
			public Float getRatio(Object band) {
				if (band instanceof BANDS) {
					switch ((BANDS) band) {
					case DIFF_BAND:
						return 0.15f;
					case DIFFRECORD_BAND:
						return null;
					}
				}
				return null;
			}
		};
	}

	@Override
	public ITimelineEventLabelProvider getEventLabelProvider() {
		return new ITimelineEventLabelProvider() {

			private DiffLabelProvider diffLabelProvider = new DiffLabelProvider();

			@Override
			public String getTitle(Object event) {
				if (event instanceof IDiff) {
					IDiff diff = (IDiff) event;
					return "Iteration #" + (diff.getRevision() + 1);
				} else if (event instanceof DiffRecord) {
					DiffRecord diffRecord = (DiffRecord) event;
					return diffRecord.getFilename();
				}
				return "";
			}

			@Override
			public URI getIcon(Object event) {
				Image image = diffLabelProvider.getImage(event);
				if (image != null)
					return TimelineHelper.createUriFromImage(image);
				return null;
			}

			@Override
			public URI getImage(Object event) {
				return null;
			}

			@Override
			public Calendar getStart(Object event) {
				if (event instanceof IDiff) {
					IDiff diff = (IDiff) event;
					TimeZoneDateRange dateRange = diff.getDateRange();
					return dateRange.getStartDate() != null ? dateRange
							.getStartDate().getCalendar() : null;
				} else if (event instanceof DiffRecord) {
					DiffRecord diffRecord = (DiffRecord) event;
					TimeZoneDateRange dateRange = diffRecord.getDateRange();
					return dateRange.getStartDate() != null ? dateRange
							.getStartDate().getCalendar() : null;
				}
				return null;
			}

			@Override
			public Calendar getEnd(Object event) {
				if (event instanceof IDiff) {
					IDiff diff = (IDiff) event;
					TimeZoneDateRange dateRange = diff.getDateRange();
					return dateRange.getEndDate() != null ? dateRange
							.getEndDate().getCalendar() : null;
				} else if (event instanceof DiffRecord) {
					DiffRecord diffRecord = (DiffRecord) event;
					TimeZoneDateRange dateRange = diffRecord.getDateRange();
					return dateRange.getEndDate() != null ? dateRange
							.getEndDate().getCalendar() : null;
				}
				return null;
			}

			@Override
			public String getColor(Object event) {
				return null;
			}

			@Override
			public boolean isResizable(Object event) {
				return false;
			}

			@Override
			public String[] getClassNames(Object event) {
				if (event instanceof IDiff) {
					return new String[] { Diff.class.getSimpleName() };
				} else if (event instanceof DiffRecord) {
					return new String[] { DiffRecord.class.getSimpleName() };
				}
				return new String[0];
			}
		};
	}
}
