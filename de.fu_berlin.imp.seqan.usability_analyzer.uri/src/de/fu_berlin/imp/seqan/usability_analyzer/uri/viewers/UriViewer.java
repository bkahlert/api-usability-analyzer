package de.fu_berlin.imp.seqan.usability_analyzer.uri.viewers;

import java.net.URI;
import java.text.DateFormat;

import org.apache.log4j.Logger;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

import com.bkahlert.devel.nebula.viewer.SortableTreeViewer;
import com.bkahlert.nebula.utils.DNDUtils;
import com.bkahlert.nebula.utils.DNDUtils.Oracle;
import com.bkahlert.nebula.utils.DistributionUtils.RelativeWidth;

import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.ui.EpisodeRenderer;

public class UriViewer extends SortableTreeViewer {

	private static final Logger LOGGER = Logger.getLogger(UriViewer.class);

	private final LocalResourceManager resources;
	private TreeViewerColumn column;

	public UriViewer(final Composite parent, int style, DateFormat dateFormat) {
		super(parent, style);

		this.resources = new LocalResourceManager(
				JFaceResources.getResources(), parent);
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				UriViewer.this.resources.dispose();
			}
		});

		this.initColumns(dateFormat);

		DNDUtils.addLocalDragSupport(this, new Oracle() {
			@Override
			public boolean allowDND() {
				return UriViewer.this.getControl().getData(
						EpisodeRenderer.CONTROL_DATA_STRING) == null;
			}
		}, URI.class);

		this.sort(0);
	}

	private void initColumns(final DateFormat dateFormat) {
		this.column = this.createColumn("URI", new RelativeWidth(1.0));
		this.column.setLabelProvider(new StyledCellLabelProvider() {
			private final UriLabelProvider uriLabelProvider = new UriLabelProvider();

			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if (element instanceof URI) {
					URI uri = (URI) element;
					try {
						StyledString text = this.uriLabelProvider
								.getStyledText(uri);
						cell.setText(text.getString());
						cell.setStyleRanges(text.getStyleRanges());
						cell.setImage(this.uriLabelProvider.getImage(uri));
					} catch (Exception e) {
						LOGGER.error("Error styling " + uri, e);
					}
				}
			}
		});
	}
}