package de.fu_berlin.imp.apiua.core.services.location;

import java.util.List;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;

import de.fu_berlin.imp.apiua.core.model.ILocatable;
import de.fu_berlin.imp.apiua.core.model.URI;
import de.fu_berlin.imp.apiua.core.services.IWorkSession;
import de.fu_berlin.imp.apiua.core.util.Cache;

/**
 * Instances of this class can be part of an {@link IWorkSession}.
 * 
 * @author bkahlert
 * 
 */
public interface ILocatorService {

	/**
	 * Returns the expected dynamic type of the {@link ILocatable} that is
	 * addressed by the given {@link URI}.
	 * <p>
	 * This method is very useful if you only want to know the type the object
	 * behind the {@link URI} will have if resolved with
	 * {@link #resolve(URI, IProgressMonitor)}.
	 * 
	 * @param uri
	 * @param monitor
	 * @return
	 */
	public Class<? extends ILocatable> getType(URI uri);

	/**
	 * Returns the {@link ILocatable} that is addressed by the given {@link URI}
	 * .
	 * <p>
	 * This method is always called in a separate {@link Thread} and is allowed
	 * to be long running.
	 * 
	 * @param uri
	 * @param monitor
	 * @return
	 */
	public Future<ILocatable> resolve(URI uri, IProgressMonitor monitor);

	/**
	 * Returns the {@link ILocatable} that is addressed by the given {@link URI}
	 * iff the {@link ILocatable} is actually of the given type.
	 * <p>
	 * This method is always called in a separate {@link Thread} and is allowed
	 * to be long running.
	 * 
	 * @param uri
	 * @param monitor
	 * @return
	 */
	public <T extends ILocatable> Future<T> resolve(URI uri, Class<T> clazz,
			IProgressMonitor monitor);

	/**
	 * Returns the {@link ILocatable}s that are addressed by the given
	 * {@link URI}s.
	 * <p>
	 * This method is always called in a separate {@link Thread} and is allowed
	 * to be long running.
	 * 
	 * @param uris
	 * @param monitor
	 * @return
	 */
	public Future<ILocatable[]> resolve(URI[] uris, IProgressMonitor monitor);

	/**
	 * Returns the {@link ILocatable}s that are addressed by the given
	 * {@link URI}s.
	 * <p>
	 * This method is always called in a separate {@link Thread} and is allowed
	 * to be long running.
	 * 
	 * @param uris
	 * @param monitor
	 * @return
	 */
	public Future<List<ILocatable>> resolve(List<URI> uris,
			IProgressMonitor monitor);

	/**
	 * Returns the {@link ILocatable}s that are addressed by the given
	 * {@link URI}s and are of the given type.
	 * <p>
	 * This method is always called in a separate {@link Thread} and is allowed
	 * to be long running.
	 * 
	 * @param uris
	 * @param monitor
	 * @return
	 */
	public <T extends ILocatable> Future<List<T>> resolve(List<URI> uris,
			Class<T> clazz, IProgressMonitor monitor);

	/**
	 * Removes an eventually cached resolve attempt.
	 * <p>
	 * This method must be called if a the identified object for a given
	 * {@link URI} changed.
	 * 
	 * @param uri
	 */
	public void uncache(URI uri);

	/**
	 * Removes an eventually cached resolve attempt.
	 * <p>
	 * This method must be called if a the identified object for a given
	 * {@link URI} changed.
	 * 
	 * @param uris
	 */
	public void uncache(URI[] uris);

	/**
	 * Shows the object associated with the given {@link URI} in the active
	 * Eclipse Workbench.
	 * <p>
	 * This method is always called in a separate {@link Thread} and is allowed
	 * to be long running.
	 * 
	 * @param uris
	 * @param open
	 *            if true also opens the instance
	 * @param monitor
	 * @return true if all {@link URI}s could be resolved and displayed in the
	 *         workbench.
	 */
	public Future<Boolean> showInWorkspace(URI uri, boolean open,
			IProgressMonitor monitor);

	/**
	 * Shows the objects associated with the given {@link URI}s in the active
	 * Eclipse Workbench.
	 * <p>
	 * This method is always called in a separate {@link Thread} and is allowed
	 * to be long running.
	 * 
	 * @param uris
	 * @param open
	 *            if true also opens the instance
	 * @param monitor
	 * @return true if all {@link URI}s could be resolved and displayed in the
	 *         workbench.
	 */
	public Future<Boolean> showInWorkspace(URI[] uris, boolean open,
			IProgressMonitor monitor);

	/**
	 * Creates a {@link Cache} that can be used to
	 * {@link #preload(String, List, IProgressMonitor)} certain objects for a
	 * specific purpose. This way preloaded objects will always resolve in a
	 * fast fashion.
	 * 
	 * @param key
	 * @param cacheSize
	 */
	public void createCache(String key, int cacheSize);

	/**
	 * Destroy a previously created {@link Cache}.
	 * 
	 * @param key
	 */
	public void destroyCache(String key);

	/**
	 * Preloads the given {@link URI}s using the cache determined by key.
	 * 
	 * @param key
	 * @param uris
	 * @param monitor
	 */
	public Future<List<ILocatable>> preload(String key, List<URI> uris,
			IProgressMonitor monitor);

}
