package de.fu_berlin.imp.apiua.groundedtheory.viewer;

import de.fu_berlin.imp.apiua.core.model.URI;

/**
 * Used to enrich an URI with meta information used for display purposes only.
 *
 * @author bkahlert
 *
 */
public class ViewerURI extends URI {

	private static final long serialVersionUID = 5238025920625242523L;

	public static final ViewerURI NO_CODES_URI = new ViewerURI(new URI(
			"nocodes"), null);

	public static final ViewerURI NO_RELATIONS_URI = new ViewerURI(new URI(
			"norelations"), null);

	public static final ViewerURI NO_PHENOMENONS_URI = new ViewerURI(new URI(
			"nophenomenons"), null);

	public static enum State {
		PARENT, NO_CODES, NO_RELATIONS;
	}

	private final State state;

	public ViewerURI(URI uri, State state) {
		super(uri.getRawURI());
		this.state = state;
	}

	public State getState() {
		return this.state;
	}

}
