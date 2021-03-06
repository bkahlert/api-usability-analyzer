package de.fu_berlin.imp.apiua.core.services;

public interface IWorkSessionService {

	public void addWorkSessionListener(IWorkSessionListener workSessionListener);

	public void removeWorkSessionListener(
			IWorkSessionListener workSessionListener);

	/**
	 * Initiates a new {@link IWorkSession} with the given
	 * {@link IWorkSessionEntity}.
	 * 
	 * @param entity
	 */
	public void startWorkSession(IWorkSessionEntity entity);

	/**
	 * Initiates a new {@link IWorkSession} with the given
	 * {@link IWorkSessionEntity}s.
	 * 
	 * @param entities
	 */
	public void startWorkSession(IWorkSessionEntity[] entities);

	/**
	 * Return the {@link IWorkSession} the application is currently dealing
	 * with.
	 * 
	 * @return null if no {@link IWorkSession} running.
	 */
	public IWorkSession getCurrentWorkSession();

	/**
	 * Restores the lastly opened {@link IWorkSession} by loading it again.
	 */
	public void restoreLastWorkSession();
}
