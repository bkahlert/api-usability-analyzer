package de.fu_berlin.imp.seqan.usability_analyzer.person.extensionProviders;

import de.fu_berlin.imp.seqan.usability_analyzer.core.model.DataSource;

public interface IDataSourceFilterListener {
	public void dataSourceFilterChanged(DataSource dataSource, boolean isOn);
}
