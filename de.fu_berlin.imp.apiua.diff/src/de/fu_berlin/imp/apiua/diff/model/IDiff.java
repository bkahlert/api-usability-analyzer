package de.fu_berlin.imp.apiua.diff.model;

import java.util.List;

import de.fu_berlin.imp.apiua.core.model.HasIdentifier;
import de.fu_berlin.imp.apiua.core.model.ILocatable;
import de.fu_berlin.imp.apiua.core.model.IOpenable;
import de.fu_berlin.imp.apiua.core.model.data.IData;
import de.fu_berlin.imp.apiua.core.ui.viewer.filters.HasDateRange;
import de.fu_berlin.imp.apiua.diff.model.impl.DiffRecords;

public interface IDiff extends IData, HasIdentifier, HasDateRange, ILocatable,
		IOpenable, ICompilable {

	/**
	 * Returns the hashed path to the directory, the client's source files where
	 * located.
	 * <p>
	 * Can be used to distinguish multiple development environments (e.g.
	 * private laptop and desktop at work).
	 * 
	 * @return
	 */
	public String getLocationHash();

	public IDiff getPrevDiffFile();

	public String getRevision();

	public int getCalculatedRevision();

	public DiffRecords getDiffFileRecords();

	public boolean sourcesExist();

	public List<String> getContent(long contentStart, long contentEnd);

}