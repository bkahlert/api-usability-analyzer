package de.fu_berlin.imp.seqan.usability_analyzer.doclog.viewer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import de.fu_berlin.imp.seqan.usability_analyzer.core.model.ID;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.TimeZoneDate;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.TimeZoneDateRange;
import de.fu_berlin.imp.seqan.usability_analyzer.core.ui.viewer.SortableTreeViewer;
import de.fu_berlin.imp.seqan.usability_analyzer.core.util.ViewerUtils;
import de.fu_berlin.imp.seqan.usability_analyzer.doclog.model.DoclogAction;
import de.fu_berlin.imp.seqan.usability_analyzer.doclog.model.DoclogFile;
import de.fu_berlin.imp.seqan.usability_analyzer.doclog.model.DoclogRecord;
import de.fu_berlin.imp.seqan.usability_analyzer.doclog.model.DoclogScreenshot.Status;
import de.fu_berlin.imp.seqan.usability_analyzer.doclog.ui.ImageManager;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.services.CodeServiceException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.services.ICodeService;

public class DoclogFilesViewer extends SortableTreeViewer {
	private LocalResourceManager resources;

	public DoclogFilesViewer(Composite parent, int style,
			DateFormat dateFormat, String timeDifferenceFormat) {
		super(parent, style);

		this.resources = new LocalResourceManager(
				JFaceResources.getResources(), parent);
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				resources.dispose();
			}
		});

		this.setContentProvider(new DoclogFilesContentProvider());

		initColumns(dateFormat, timeDifferenceFormat);

		this.sort(0);
	}

	private void initColumns(final DateFormat dateFormat,
			final String timeDifferenceFormat) {

		final ICodeService codeService = (ICodeService) PlatformUI
				.getWorkbench().getService(ICodeService.class);

		this.createColumn("Date", 160).setLabelProvider(
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DoclogFile) {
							DoclogFile doclogFile = (DoclogFile) element;
							if (doclogFile.getID() != null)
								return doclogFile.getID().toString();
							else
								return doclogFile.getFingerprint().toString();
						}
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							TimeZoneDate date = doclogRecord.getDateRange()
									.getStartDate();
							return (date != null) ? date.format(dateFormat)
									: "";
						}
						return "";
					}

					@Override
					public Image getImage(Object element) {
						if (element instanceof DoclogFile) {
							DoclogFile doclogFile = (DoclogFile) element;
							try {
								return (codeService.getCodes(doclogFile).size() > 0) ? (codeService
										.isMemo(doclogFile) ? ImageManager.DOCLOGFILE_CODED_MEMO
										: ImageManager.DOCLOGFILE_CODED)
										: (codeService.isMemo(doclogFile) ? ImageManager.DOCLOGFILE_MEMO
												: ImageManager.DOCLOGFILE);
							} catch (CodeServiceException e) {
								return ImageManager.DOCLOGRECORD;
							}
						}
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							try {
								return (codeService.getCodes(doclogRecord)
										.size() > 0) ? (codeService
										.isMemo(doclogRecord) ? ImageManager.DOCLOGRECORD_CODED_MEMO
										: ImageManager.DOCLOGRECORD_CODED)
										: (codeService.isMemo(doclogRecord) ? ImageManager.DOCLOGRECORD_MEMO
												: ImageManager.DOCLOGRECORD);
							} catch (CodeServiceException e) {
								return ImageManager.DOCLOGRECORD;
							}
						}
						return super.getImage(element);
					}
				});

		this.createColumn("Passed", 90, true, new Comparator<Object>() {
			@Override
			public int compare(Object arg0, Object arg1) {
				Long l1 = (Long) arg0;
				Long l2 = (Long) arg1;
				if (l1 != null)
					return l1.compareTo(l2);
				return 0;
			}
		}, new Class<?>[] { Long.class }).setLabelProvider(
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							Long milliSecondsPassed = doclogRecord
									.getDateRange().getDifference();
							return (milliSecondsPassed != null) ? DurationFormatUtils
									.formatDuration(milliSecondsPassed,
											timeDifferenceFormat, true) : "";
						}
						return "";
					}
				});

		this.createColumn("", 10, false, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1 instanceof DoclogFile && o2 instanceof DoclogFile) {
					DoclogFile doclogFile1 = (DoclogFile) o1;
					DoclogFile doclogFile2 = (DoclogFile) o2;
					Status status1 = doclogFile1.getScreenshotStatus();
					Status status2 = doclogFile2.getScreenshotStatus();
					return Integer.valueOf(status1.ordinal()).compareTo(
							status2.ordinal());
				} else if (o1 instanceof DoclogRecord
						&& o2 instanceof DoclogRecord) {
					DoclogRecord doclogRecord1 = (DoclogRecord) o1;
					DoclogRecord doclogRecord2 = (DoclogRecord) o2;
					Status status1 = doclogRecord1.getScreenshot().getStatus();
					Status status2 = doclogRecord2.getScreenshot().getStatus();
					return Integer.valueOf(status1.ordinal()).compareTo(
							status2.ordinal());
				}
				return 0;
			}
		}, new Class<?>[] { DoclogFile.class, DoclogRecord.class })
				.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return "";
					}

					@Override
					public Color getBackground(Object element) {
						if (element instanceof DoclogFile) {
							Status worstStatus = ((DoclogFile) element)
									.getScreenshotStatus();
							RGB backgroundRgb = worstStatus.getRGB();
							return resources.createColor(backgroundRgb);
						}
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							RGB backgroundRgb = doclogRecord.getScreenshot()
									.getStatus().getRGB();
							return resources.createColor(backgroundRgb);
						}
						return null;
					}
				});

		this.createColumn("URL", 200).setLabelProvider(
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							String url = doclogRecord.getUrl();
							return (url != null) ? url : "";
						}
						return "";
					}
				});

		this.createColumn("Action", 60).setLabelProvider(
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							DoclogAction action = doclogRecord.getAction();
							return (action != null) ? action.toString() : "";
						}
						return "";
					}
				});

		this.createColumn("Param", 50).setLabelProvider(
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							String actionParameter = doclogRecord
									.getActionParameter();
							return (actionParameter != null) ? actionParameter
									: "";
						}
						return "";
					}
				});

		this.createColumn("Width", 40).setLabelProvider(
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							Point windowDimensions = doclogRecord
									.getWindowDimensions();
							return (windowDimensions != null) ? windowDimensions.x
									+ ""
									: "-";
						}
						return "";
					}
				});

		this.createColumn("Height", 40).setLabelProvider(
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DoclogRecord) {
							DoclogRecord doclogRecord = (DoclogRecord) element;
							Point windowDimensions = doclogRecord
									.getWindowDimensions();
							return (windowDimensions != null) ? windowDimensions.y
									+ ""
									: "-";
						}
						return "";
					}
				});

		this.createColumn("X", 40).setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof DoclogRecord) {
					DoclogRecord doclogRecord = (DoclogRecord) element;
					Point scrollPosition = doclogRecord.getScrollPosition();
					return (scrollPosition != null) ? scrollPosition.x + ""
							: "-";
				}
				return "";
			}
		});

		this.createColumn("X", 40).setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof DoclogRecord) {
					DoclogRecord doclogRecord = (DoclogRecord) element;
					Point scrollPosition = doclogRecord.getScrollPosition();
					return (scrollPosition != null) ? scrollPosition.y + ""
							: "-";
				}
				return "";
			}
		});
	}

	/**
	 * Returns the {@link TreePath}s that describe {@link DoclogRecord}
	 * fulfilling the following criteria:
	 * <ol>
	 * <li>{@link DoclocRecord}'s {@link TimeZoneDateRange} intersects one of
	 * the given {@link TimeZoneDateRange}s
	 * </ol>
	 * 
	 * @param treeItems
	 * @param dataRanges
	 * @return
	 */
	public static List<TreePath> getItemsOfIntersectingDataRanges(
			TreeItem[] treeItems, List<TimeZoneDateRange> dataRanges) {
		List<TreePath> treePaths = new ArrayList<TreePath>();
		for (TreeItem treeItem : ViewerUtils.getItemWithDataType(treeItems,
				DoclogRecord.class)) {
			DoclogRecord doclogRecord = (DoclogRecord) treeItem.getData();
			for (TimeZoneDateRange dateRange : dataRanges) {
				if (dateRange.isIntersected(doclogRecord.getDateRange())) {
					treePaths.add(new TreePath(new Object[] { doclogRecord }));
					break;
				}
			}
		}
		return treePaths;
	}

	/**
	 * Returns the {@link TreePath}s that describe {@link DoclogRecord}
	 * fulfilling the following criteria:
	 * <ol>
	 * <li>{@link DoclogRecord} belongs to a {@link DoclogFile} with the given
	 * {@link ID}
	 * <li>{@link DoclocRecord}'s {@link TimeZoneDateRange} intersects one of
	 * the given {@link TimeZoneDateRange}s
	 * </ol>
	 * 
	 * @param treeItems
	 * @param id
	 * @param dataRanges
	 * @return
	 */
	public static List<TreePath> getItemsOfIdIntersectingDataRanges(
			TreeItem[] treeItems, ID id, List<TimeZoneDateRange> dataRanges) {
		List<TreePath> treePaths = new ArrayList<TreePath>();
		for (TreeItem treeItem : ViewerUtils.getItemWithDataType(treeItems,
				DoclogFile.class)) {
			DoclogFile doclogFile = (DoclogFile) treeItem.getData();
			if (id.equals(doclogFile.getID())) {
				List<TreePath> childTreePaths = DoclogFilesViewer
						.getItemsOfIntersectingDataRanges(treeItem.getItems(),
								dataRanges);
				for (TreePath childTreePath : childTreePaths) {
					TreePath treePath = ViewerUtils.merge(new TreePath(
							new Object[] { doclogFile }), childTreePath);
					treePaths.add(treePath);
				}
			}
		}
		return treePaths;
	}
}
