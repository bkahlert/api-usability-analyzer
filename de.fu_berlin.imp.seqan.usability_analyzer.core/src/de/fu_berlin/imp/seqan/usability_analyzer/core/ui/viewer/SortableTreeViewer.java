package de.fu_berlin.imp.seqan.usability_analyzer.core.ui.viewer;

import java.util.Comparator;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;

public class SortableTreeViewer extends TreeViewer {

	private GenericColumnViewerComparator genericColumnViewerComparator = new GenericColumnViewerComparator();

	public SortableTreeViewer(Composite parent, int style) {
		super(parent, style);
		super.setComparator(this.genericColumnViewerComparator);
	}

	@Override
	public void setComparator(ViewerComparator comparator) {
		// we manage the comparator on our owns
	}

	public TreeViewerColumn createColumn(String title, int bound,
			boolean isResizable, boolean isMoveable,
			Comparator<Object> comparator, Class<?>[] comparatorClasses) {
		final TreeViewerColumn viewerColumn = new TreeViewerColumn(this,
				SWT.NONE);
		final TreeColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(isResizable);
		column.setMoveable(isMoveable);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sort(column);
			}
		});

		if (comparator != null)
			this.genericColumnViewerComparator.setComparator(
					getColumnNumber(column), comparator, comparatorClasses);

		return viewerColumn;
	}

	public TreeViewerColumn createColumn(String title, int bound,
			boolean isResizableAndMovable, Comparator<Object> comparator,
			Class<?>[] comparatorClasses) {
		return this.createColumn(title, bound, isResizableAndMovable,
				isResizableAndMovable, comparator, comparatorClasses);
	}

	public TreeViewerColumn createColumn(String title, int bound) {
		return this.createColumn(title, bound, true, true, null, null);
	}

	public TreeColumn getColumn(int colNumber) {
		return this.getTree().getColumn(colNumber);
	}

	public int getColumnNumber(TreeColumn treeColumn) {
		for (int i = 0; i < this.getTree().getColumnCount(); i++) {
			if (this.getTree().getColumn(i) == treeColumn)
				return i;
		}
		return -1;
	}

	public void sort(TreeColumn column) {
		this.genericColumnViewerComparator.setColumn(getColumnNumber(column));
		int dir = this.genericColumnViewerComparator.getDirection();
		SortableTreeViewer.this.getTree().setSortDirection(dir);
		SortableTreeViewer.this.getTree().setSortColumn(column);
		SortableTreeViewer.this.refresh();
	}

	public void sort(int colNumber) {
		this.sort(this.getColumn(colNumber));
	}
}
