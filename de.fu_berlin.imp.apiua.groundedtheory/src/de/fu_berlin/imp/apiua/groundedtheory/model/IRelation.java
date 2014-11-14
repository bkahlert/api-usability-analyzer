package de.fu_berlin.imp.apiua.groundedtheory.model;

import de.fu_berlin.imp.apiua.core.model.ILocatable;
import de.fu_berlin.imp.apiua.core.model.TimeZoneDate;
import de.fu_berlin.imp.apiua.core.model.URI;
import de.fu_berlin.imp.apiua.groundedtheory.model.JointJSRelation.IEndpoint;

/**
 * Instances of this interface symbolize links between two {@link IEndpoint}s.
 *
 * @author bkahlert
 *
 */
public interface IRelation extends ILocatable {

	public URI getFrom();

	public URI getTo();

	public String getTitle();

	public TimeZoneDate getCreation();

}
