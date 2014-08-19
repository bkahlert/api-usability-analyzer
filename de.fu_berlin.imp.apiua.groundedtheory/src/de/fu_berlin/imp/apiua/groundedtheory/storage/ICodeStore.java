package de.fu_berlin.imp.apiua.groundedtheory.storage;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

import com.bkahlert.nebula.utils.colors.RGB;

import de.fu_berlin.imp.apiua.core.model.URI;
import de.fu_berlin.imp.apiua.groundedtheory.model.ICode;
import de.fu_berlin.imp.apiua.groundedtheory.model.IEpisode;
import de.fu_berlin.imp.apiua.groundedtheory.model.dimension.IDimension;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeDoesNotExistException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeHasChildCodesException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeInstanceDoesNotExistException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeStoreFullException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeStoreReadException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeStoreWriteException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.impl.DuplicateCodeInstanceException;

public interface ICodeStore {

	public List<ICode> getTopLevelCodes();

	public Set<ICodeInstance> getInstances();

	/**
	 * Returns an existing {@link ICode} based on it's internal id
	 * 
	 * @param id
	 * @return
	 */
	public ICode getCode(long id);

	public ICodeInstance getCodeInstance(long id);

	public ICode[] getCodes();

	public ICode createCode(String caption, RGB color)
			throws CodeStoreFullException;

	public ICodeInstance[] createCodeInstances(ICode[] codes, URI[] uris)
			throws InvalidParameterException, CodeStoreReadException,
			DuplicateCodeInstanceException, CodeStoreFullException;

	public void addAndSaveCode(ICode code) throws CodeStoreWriteException,
			CodeStoreReadException;

	public void addAndSaveCodeInstances(ICodeInstance[] codeInstance)
			throws CodeStoreWriteException, CodeStoreReadException;

	public void removeAndSaveCode(ICode code) throws CodeStoreWriteException,
			CodeHasChildCodesException, CodeDoesNotExistException;

	public void removeAndSaveCode(ICode code, boolean deleteInstances)
			throws CodeStoreWriteException, CodeHasChildCodesException,
			CodeDoesNotExistException;

	public void removeAndSaveCodeInstance(ICodeInstance codeInstance)
			throws CodeStoreWriteException, CodeStoreReadException;

	public File getBackupFile();

	/**
	 * Saves the {@link ICodeStore} and creates a backup.
	 * 
	 * @return
	 * @throws CodeStoreWriteException
	 */
	public void save() throws CodeStoreWriteException;

	public void deleteCodeInstance(ICodeInstance codeInstance)
			throws CodeInstanceDoesNotExistException, CodeStoreWriteException;

	public void deleteCodeInstances(ICode code) throws CodeStoreReadException,
			CodeStoreWriteException;

	public ICode getParent(ICode code);

	public ICode setParent(ICode childNode, ICode newParentNode)
			throws CodeDoesNotExistException, CodeStoreWriteException;

	public List<ICode> getChildren(ICode code);

	public List<ICode> getSubCodes(ICode code);

	public boolean codeExists(ICode code);

	public String getMemo(ICode code);

	public String getMemo(ICodeInstance codeInstance);

	public String getMemo(URI uri);

	public void setMemo(ICode code, String html) throws CodeStoreWriteException;

	public void setMemo(ICodeInstance codeInstance, String html)
			throws CodeStoreWriteException;

	public void setMemo(URI uri, String html) throws CodeStoreWriteException;

	/**
	 * 
	 * @param uri
	 *            only URIs of ICodes expected!
	 * @return
	 */
	public IDimension getDimension(URI uri);

	/**
	 * @param uri
	 *            only URIs of ICodes expected!
	 * @param dimension
	 */
	public void setDimension(URI uri, IDimension dimension);

	/**
	 * 
	 * @param uri
	 *            only URIs of ICodeInstances expected!
	 * @return
	 */
	public String getDimensionValue(URI valueUri, URI dimensionalizedUri);

	/**
	 * 
	 * @param uri
	 *            only URIs of ICodeInstances expected!
	 * @param value
	 */
	public void setDimensionValue(URI valueUri, URI dimensionalizedUri,
			String value);

	/**
	 * Sets data for a given type and {@link URI}.
	 * 
	 * @param type
	 * @param uri
	 * @param content
	 * @throws CodeStoreWriteException
	 */
	public void setRaw(String type, URI uri, String content)
			throws CodeStoreWriteException;

	/**
	 * Returns all {@link URI}s with set data and a common type.
	 * 
	 * @param type
	 * @return
	 * @throws CodeStoreReadException
	 */
	public List<URI> getRaw(String type) throws CodeStoreReadException;

	/**
	 * Returns the set data for a given type and {@link URI}.
	 * 
	 * @param type
	 * @param uri
	 * @return
	 * @throws CodeStoreReadException
	 */
	public String getRaw(String type, URI uri) throws CodeStoreReadException;

	public Set<IEpisode> getEpisodes();

	/**
	 * Returns the {@link URI}s that serve as properties for the given
	 * {@link URI}.
	 * 
	 * @param uri
	 * @return a list of the properties. Changes to the list are
	 *         <strong>not</strong> reflected in the data.
	 */
	public List<URI> getProperties(URI uri);

	/**
	 * Sets the given {@link URI}s as the properties for the given {@link URI}.
	 * 
	 * @param uri
	 * @param properties
	 *            a copy of this list is saved. Further changes to the passed
	 *            list are not reflected.
	 * @throws CodeStoreWriteException
	 *             if a cyclic graph would result
	 */
	public void setProperties(URI uri, List<URI> properties)
			throws CodeStoreWriteException;

}
