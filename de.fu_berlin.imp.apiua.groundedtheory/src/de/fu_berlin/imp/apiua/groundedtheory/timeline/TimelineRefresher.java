package de.fu_berlin.imp.apiua.groundedtheory.timeline;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.Viewer;

import com.bkahlert.nebula.utils.ExecUtils.DelayableThread;
import com.bkahlert.nebula.utils.ViewerUtils;
import com.bkahlert.nebula.utils.colors.RGB;

import de.fu_berlin.imp.apiua.core.model.URI;
import de.fu_berlin.imp.apiua.groundedtheory.model.ICode;
import de.fu_berlin.imp.apiua.groundedtheory.model.IEpisode;
import de.fu_berlin.imp.apiua.groundedtheory.model.IRelation;
import de.fu_berlin.imp.apiua.groundedtheory.model.IRelationInstance;
import de.fu_berlin.imp.apiua.groundedtheory.model.dimension.IDimension;
import de.fu_berlin.imp.apiua.groundedtheory.services.ICodeServiceListener;

/**
 * Instances of this class are responsible to update a timeline viewer so that
 * it always reflects the model correctly.
 *
 * @author bkahlert
 *
 */
// FIXME let this be implemented by the contributing providers
public class TimelineRefresher implements ICodeServiceListener {

	private static class DelayedRefresher extends DelayableThread {
		public DelayedRefresher(final Viewer viewer, long delay) {
			super(new Runnable() {
				@Override
				public void run() {
					ViewerUtils.refresh(viewer);
				}
			}, delay);
		}
	}

	private final Viewer viewer;
	private final long delay;

	private DelayedRefresher delayedRefresher = null;

	/**
	 * Creates an {@link TimelineRefresher} that delays the refresh calls by the
	 * specified delay. This way high frequency refreshments don't affect the
	 * performance negatively.
	 *
	 * @param viewer
	 * @param delay
	 */
	public TimelineRefresher(Viewer viewer, long delay) {
		Assert.isNotNull(viewer);
		this.viewer = viewer;
		this.delay = delay;
	}

	private void scheduleRefresh() {
		if (this.delayedRefresher == null || this.delayedRefresher.isFinished()) {
			this.delayedRefresher = new DelayedRefresher(this.viewer,
					this.delay);
			this.delayedRefresher.start();
		} else {
			this.delayedRefresher.setDelay(this.delay);
		}
	}

	@Override
	public void codesAdded(List<ICode> codes) {
		this.scheduleRefresh();
	}

	@Override
	public void codesAssigned(List<ICode> codes, List<URI> uris) {
		this.scheduleRefresh();
	}

	@Override
	public void codeRenamed(ICode code, String oldCaption, String newCaption) {
		this.scheduleRefresh();
	}

	@Override
	public void codeRecolored(ICode code, RGB oldColor, RGB newColor) {
		this.scheduleRefresh();
	}

	@Override
	public void codesRemoved(List<ICode> removedCodes, List<URI> uris) {
		this.scheduleRefresh();
	}

	@Override
	public void codeMoved(ICode code, ICode oldParentCode, ICode newParentCode) {
		this.scheduleRefresh();
	}

	@Override
	public void codeDeleted(ICode code) {
		this.scheduleRefresh();
	}

	@Override
	public void relationsAdded(Set<IRelation> relations) {
		this.scheduleRefresh();
	}

	@Override
	public void relationsRenamed(Set<IRelation> relations) {
		this.scheduleRefresh();
	}

	@Override
	public void relationsDeleted(Set<IRelation> relations) {
		this.scheduleRefresh();
	}

	@Override
	public void relationInstancesAdded(Set<IRelationInstance> relations) {
		this.scheduleRefresh();
	}

	@Override
	public void relationInstancesDeleted(Set<IRelationInstance> relations) {
		this.scheduleRefresh();
	}

	@Override
	public void memoAdded(URI uri) {
		this.scheduleRefresh();
	}

	@Override
	public void memoModified(URI uri) {
	}

	@Override
	public void memoRemoved(URI uri) {
		this.scheduleRefresh();
	}

	@Override
	public void episodeAdded(IEpisode episode) {
		this.scheduleRefresh();
	}

	@Override
	public void episodeReplaced(IEpisode oldEpisode, IEpisode newEpisode) {
		this.scheduleRefresh();
	}

	@Override
	public void episodesDeleted(Set<IEpisode> deletedEpisodes) {
		this.scheduleRefresh();
	}

	@Override
	public void dimensionChanged(URI uri, IDimension oldDimension,
			IDimension newDimension) {
		this.scheduleRefresh();
	}

	@Override
	public void dimensionValueChanged(URI uri, String oldValue, String value) {
		this.scheduleRefresh();
	}

	@Override
	public void propertiesChanged(URI uri, List<URI> addedProperties,
			List<URI> removedProperties) {
		this.scheduleRefresh();
	}

	@Override
	public void axialCodingModelAdded(URI uri) {
		this.scheduleRefresh();
	}

	@Override
	public void axialCodingModelUpdated(URI uri) {
		this.scheduleRefresh();
	}

	@Override
	public void axialCodingModelRemoved(URI uri) {
		this.scheduleRefresh();
	}

}
