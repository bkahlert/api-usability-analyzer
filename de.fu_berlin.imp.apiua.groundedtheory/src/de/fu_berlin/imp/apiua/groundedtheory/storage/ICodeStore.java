package de.fu_berlin.imp.apiua.groundedtheory.storage;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

import com.bkahlert.nebula.utils.colors.RGB;

import de.fu_berlin.imp.apiua.core.model.URI;
import de.fu_berlin.imp.apiua.groundedtheory.model.ICode;
import de.fu_berlin.imp.apiua.groundedtheory.model.IEpisode;
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

}