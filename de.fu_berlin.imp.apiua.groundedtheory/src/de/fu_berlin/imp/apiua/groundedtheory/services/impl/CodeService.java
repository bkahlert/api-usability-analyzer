package de.fu_berlin.imp.apiua.groundedtheory.services.impl;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IDisposable;
import org.osgi.service.component.ComponentContext;

import com.bkahlert.nebula.utils.colors.RGB;

import de.fu_berlin.imp.apiua.core.model.IdentifierFactory;
import de.fu_berlin.imp.apiua.core.model.URI;
import de.fu_berlin.imp.apiua.core.model.identifier.IIdentifier;
import de.fu_berlin.imp.apiua.core.services.IImportanceService;
import de.fu_berlin.imp.apiua.core.services.IImportanceService.IImportanceInterceptor;
import de.fu_berlin.imp.apiua.core.services.IImportanceService.Importance;
import de.fu_berlin.imp.apiua.core.util.NoNullSet;
import de.fu_berlin.imp.apiua.groundedtheory.LocatorService;
import de.fu_berlin.imp.apiua.groundedtheory.model.IAxialCodingModel;
import de.fu_berlin.imp.apiua.groundedtheory.model.ICode;
import de.fu_berlin.imp.apiua.groundedtheory.model.IEpisode;
import de.fu_berlin.imp.apiua.groundedtheory.model.JointJSAxialCodingModel;
import de.fu_berlin.imp.apiua.groundedtheory.services.CodeServiceException;
import de.fu_berlin.imp.apiua.groundedtheory.services.ICodeService;
import de.fu_berlin.imp.apiua.groundedtheory.services.ICodeServiceListener;
import de.fu_berlin.imp.apiua.groundedtheory.storage.ICodeInstance;
import de.fu_berlin.imp.apiua.groundedtheory.storage.ICodeStore;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeDoesNotExistException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeHasChildCodesException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeInstanceDoesNotExistException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeStoreFullException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeStoreReadException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.exceptions.CodeStoreWriteException;
import de.fu_berlin.imp.apiua.groundedtheory.storage.impl.CodeStoreFactory;
import de.fu_berlin.imp.apiua.groundedtheory.storage.impl.DuplicateCodeInstanceException;

public class CodeService implements ICodeService, IDisposable {

	private static final Logger LOGGER = Logger.getLogger(CodeService.class);

	private IImportanceService importanceService = null;
	private final IImportanceInterceptor importanceInterceptor = new IImportanceInterceptor() {
		@Override
		public void gettingImportance(Map<URI, Importance> uris) {
			for (Entry<URI, Importance> importance : uris.entrySet()) {
				try {
					for (ICode code : CodeService.this.getCodes(importance
							.getKey())) {
						Importance codeImportance = CodeService.this.importanceService
								.getImportance(code.getUri());
						if (codeImportance == Importance.HIGH) {
							importance.setValue(Importance.HIGH);
						}
					}
				} catch (CodeServiceException e) {
					LOGGER.error(e);
				}
			}
		}
	};

	@SuppressWarnings("unused")
	private ComponentContext context;
	private final ICodeStore codeStore;
	private final CodeServiceListenerNotifier codeServiceListenerNotifier;

	public CodeService() throws IOException {
		this(new CodeStoreFactory().getCodeStore());
	}

	public CodeService(ICodeStore codeStore) throws IOException {
		Assert.isNotNull(codeStore);
		this.codeStore = codeStore;
		this.codeServiceListenerNotifier = new CodeServiceListenerNotifier();
		try {
			this.importanceService = (IImportanceService) PlatformUI
					.getWorkbench().getService(IImportanceService.class);
			this.importanceService
					.addImportanceInterceptor(this.importanceInterceptor);
		} catch (NoClassDefFoundError e) {
			LOGGER.error(e);
		}
	}

	@Override
	public void dispose() {
		if (this.importanceService != null) {
			this.importanceService
					.removeImportanceInterceptor(this.importanceInterceptor);
		}
	}

	@Override
	public void addCodeServiceListener(ICodeServiceListener codeServiceListener) {
		this.codeServiceListenerNotifier
				.addCodeServiceListener(codeServiceListener);
	}

	@Override
	public void removeCodeServiceListener(
			ICodeServiceListener codeServiceListener) {
		this.codeServiceListenerNotifier
				.removeCodeServiceListener(codeServiceListener);
	}

	@Override
	public ICodeStore getCodeStore() {
		return this.codeStore;
	}

	@Override
	public ICode createCode(String caption, RGB color)
			throws CodeServiceException {
		ICode code;
		try {
			code = this.codeStore.createCode(caption, color);
		} catch (CodeStoreFullException e) {
			throw new CodeServiceException(e);
		}
		this.codeServiceListenerNotifier.codesCreated(Arrays.asList(code));
		return code;
	}

	@Override
	public ICode getCode(long id) {
		return this.codeStore.getCode(id);
	}

	@Override
	public List<ICode> getCodes(URI uri) throws CodeServiceException {
		LinkedList<ICode> codes = new LinkedList<ICode>();
		for (ICodeInstance codeInstance : this.codeStore.getInstances()) {
			if (codeInstance.getId().equals(uri)) {
				codes.add(codeInstance.getCode());
			}
		}
		return codes;
	}

	@Override
	public ICode addCode(String codeCaption, RGB color, URI uri)
			throws CodeServiceException {
		ICode code = this.createCode(codeCaption, color);
		this.addCode(code, uri);
		return code;
	}

	@Override
	public URI addCode(ICode code, final URI uri) throws CodeServiceException {
		return this.addCodes(Arrays.asList(code), Arrays.asList(uri))[0];
	}

	@Override
	public URI[] addCodes(List<ICode> codes, List<URI> uris)
			throws CodeServiceException {
		try {
			for (ICode code : codes) {
				if (!this.codeStore.codeExists(code)) {
					this.codeStore.addAndSaveCode(code);
					this.codeServiceListenerNotifier.codesCreated(codes);
				}
			}
			ICodeInstance[] codeInstances = this.codeStore.createCodeInstances(
					codes.toArray(new ICode[0]), uris.toArray(new URI[0]));
			this.codeStore.addAndSaveCodeInstances(codeInstances);
			this.codeServiceListenerNotifier.codesAssigned(codes, uris);
			URI[] codeInstanceUris = new URI[codeInstances.length];
			for (int i = 0; i < codeInstanceUris.length; i++) {
				codeInstanceUris[i] = codeInstances[i].getUri();
			}
			return codeInstanceUris;
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		} catch (CodeStoreReadException e) {
			throw new CodeServiceException(e);
		} catch (DuplicateCodeInstanceException e) {
			throw new CodeServiceException(e);
		} catch (InvalidParameterException e) {
			throw new CodeServiceException(e);
		} catch (CodeStoreFullException e) {
			throw new CodeServiceException(e);
		}
	}

	@Override
	public Set<URI> getCodedIDs() {
		Set<URI> codedIDs = new HashSet<URI>();
		for (ICodeInstance instance : this.codeStore.getInstances()) {
			URI id = instance.getId();
			if (!codedIDs.contains(id)) {
				codedIDs.add(id);
			}
		}
		return codedIDs;
	}

	@Override
	public List<ICodeInstance> getInstances() {
		ArrayList<ICodeInstance> codeInstances = new ArrayList<ICodeInstance>();
		for (ICodeInstance codeInstance : this.codeStore.getInstances()) {
			codeInstances.add(codeInstance);
		}
		return codeInstances;
	}

	@Override
	public List<ICodeInstance> getInstances(IIdentifier identifier) {
		ArrayList<ICodeInstance> codeInstances = new ArrayList<ICodeInstance>();
		for (ICodeInstance codeInstance : this.codeStore.getInstances()) {
			String[] uriParts = codeInstance.getId().toString().split("/");
			if (identifier.equals(IdentifierFactory.createFrom(uriParts[3]))) {
				codeInstances.add(codeInstance);
			}
		}
		return codeInstances;
	}

	@Override
	public List<ICodeInstance> getInstances(ICode code) {
		ArrayList<ICodeInstance> codeInstances = new ArrayList<ICodeInstance>();
		for (ICodeInstance codeInstance : this.codeStore.getInstances()) {
			if (codeInstance.getCode().equals(code)) {
				codeInstances.add(codeInstance);
			}
		}
		return codeInstances;
	}

	@Override
	public Collection<? extends ICodeInstance> getAllInstances(ICode code) {
		List<ICodeInstance> instances = this.getInstances(code);
		for (ICode subCode : this.getSubCodes(code)) {
			instances.addAll(this.getAllInstances(subCode));
		}
		return instances;
	}

	@Override
	public void putInstances(ICode code, List<URI> uris) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renameCode(ICode code, String newCaption)
			throws CodeServiceException {
		String oldCaption = code.getCaption();
		try {
			code.setCaption(newCaption);
			this.codeStore.save();
			this.codeServiceListenerNotifier.codeRenamed(code, oldCaption,
					newCaption);
		} catch (Exception e) {
			throw new CodeServiceException(e);
		}
	}

	@Override
	public void recolorCode(ICode code, RGB newColor)
			throws CodeServiceException {
		RGB oldColor = code.getColor();
		try {
			code.setColor(newColor);
			this.codeStore.save();
			this.codeServiceListenerNotifier.codeRecolored(code, oldColor,
					newColor);
		} catch (Exception e) {
			throw new CodeServiceException(e);
		}
	}

	@Override
	public List<ICode> getTopLevelCodes() {
		return this.codeStore.getTopLevelCodes();
	}

	@Override
	public ICode getParent(ICode code) {
		return this.codeStore.getParent(code);
	}

	@Override
	public void setParent(ICode code, ICode parentCode)
			throws CodeServiceException {
		ICode oldParentCode;
		try {
			oldParentCode = this.codeStore.setParent(code, parentCode);
			this.codeServiceListenerNotifier.codeMoved(code, oldParentCode,
					parentCode);
		} catch (CodeDoesNotExistException e) {
			throw new CodeServiceException(e);
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		}
	}

	@Override
	public List<ICode> getChildren(ICode code) {
		return this.codeStore.getChildren(code);
	}

	@Override
	public List<ICode> getSubCodes(ICode code) {
		return this.codeStore.getSubCodes(code);
	}

	@Override
	public void removeCodes(List<ICode> codes, final URI uri)
			throws CodeServiceException {
		if (codes.size() == 0) {
			return;
		}
		try {
			List<ICode> removedCodes = new LinkedList<ICode>();
			for (ICodeInstance codeInstance : this.codeStore.getInstances()) {
				if (codes.contains(codeInstance.getCode())
						&& codeInstance.getId().equals(uri)) {
					this.codeStore.deleteCodeInstance(codeInstance);
					removedCodes.add(codeInstance.getCode());
				}
			}
			if (removedCodes.size() == 0) {
				throw new CodeInstanceDoesNotExistException();
			}

			this.codeServiceListenerNotifier.codesRemoved(removedCodes,
					Arrays.asList(uri));
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		} catch (CodeInstanceDoesNotExistException e) {
			throw new CodeServiceException(e);
		}
	}

	// TODO check if removed is also fired to allow viewers to only listen to
	// removals (so they don't have to refresh their viewers although they are
	// not affected
	@Override
	public void deleteCode(ICode code) throws CodeServiceException {
		try {
			this.codeStore.removeAndSaveCode(code);
			this.codeServiceListenerNotifier.codeDeleted(code);
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		} catch (CodeDoesNotExistException e) {
			throw new CodeServiceException(e);
		} catch (CodeHasChildCodesException e) {
			throw new CodeServiceException(e);
		}
	}

	@Override
	public void deleteCode(ICode code, boolean forceDelete)
			throws CodeServiceException {
		try {
			this.codeStore.removeAndSaveCode(code, forceDelete);
			this.codeServiceListenerNotifier.codeDeleted(code);
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		} catch (CodeDoesNotExistException e) {
			throw new CodeServiceException(e);
		} catch (CodeHasChildCodesException e) {
			throw new CodeServiceException(e);
		}
	}

	@Override
	public void deleteCodeInstance(ICodeInstance codeInstance)
			throws CodeServiceException {
		try {
			this.codeStore.deleteCodeInstance(codeInstance);
			ICode code = codeInstance.getCode();
			this.codeServiceListenerNotifier.codesRemoved(Arrays.asList(code),
					Arrays.asList(codeInstance.getId()));
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		} catch (CodeInstanceDoesNotExistException e) {
			throw new CodeServiceException(e);
		}
	}

	@Override
	public String loadMemo(URI uri) {
		return this.codeStore.getMemo(uri);
	}

	@Override
	public void setMemo(URI uri, String html) throws CodeServiceException {
		String oldHtml = this.codeStore.getMemo(uri);

		if (oldHtml == null || oldHtml.trim().isEmpty()) {
			oldHtml = "";
		}
		if (html == null || html.trim().isEmpty()) {
			html = "";
		}
		if (oldHtml.equals(html)) {
			return;
		}

		try {
			this.codeStore.setMemo(uri, html);
			if (oldHtml.equals("") && !html.equals("")) {
				this.codeServiceListenerNotifier.memoAdded(uri, html);
			} else if (!oldHtml.equals("") && !html.equals("")) {
				this.codeServiceListenerNotifier.memoModified(uri, html);
			} else if (!oldHtml.equals("") && html.equals("")) {
				this.codeServiceListenerNotifier.memoRemoved(uri, html);
			} else {
				throw new CodeStoreWriteException("STATE ERROR");
			}
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		}
	}

	@Override
	public boolean isMemo(URI uri) {
		String html = this.codeStore.getMemo(uri);
		return html != null && !html.trim().isEmpty();
	}

	@Override
	public List<IIdentifier> getEpisodedIdentifiers() {
		List<IIdentifier> identifiers = new LinkedList<IIdentifier>();
		for (IEpisode episode : this.codeStore.getEpisodes()) {
			IIdentifier identifier = episode.getIdentifier();
			if (!identifiers.contains(identifier)) {
				identifiers.add(identifier);
			}
		}
		return identifiers;
	}

	@Override
	public Set<IEpisode> getEpisodes(IIdentifier identifier) {
		Set<IEpisode> episodes = new NoNullSet<IEpisode>();
		for (IEpisode episode : this.codeStore.getEpisodes()) {
			if (episode.getIdentifier().equals(identifier)) {
				episodes.add(episode);
			}
		}
		return episodes;
	}

	@Override
	public void addEpisodeAndSave(IEpisode episode) throws CodeServiceException {
		Set<IEpisode> episodes = this.codeStore.getEpisodes();
		if (!episodes.contains(episode)) {
			episodes.add(episode);
			try {
				this.codeStore.save();
				this.codeServiceListenerNotifier.episodeAdded(episode);
			} catch (CodeStoreWriteException e) {
				throw new CodeServiceException(e);
			}
		} else {
			throw new EpisodeAlreadyExistsException(episode);
		}
	}

	@Override
	public void replaceEpisodeAndSave(IEpisode oldEpisode, IEpisode newEpisode)
			throws CodeServiceException {
		if (oldEpisode == null || newEpisode == null) {
			throw new CodeServiceException(new IllegalArgumentException(
					"Arguments must not be null"));
		}
		Set<IEpisode> episodes = this.codeStore.getEpisodes();
		if (episodes.contains(oldEpisode)) {
			if (LocatorService.INSTANCE != null) {
				LocatorService.INSTANCE.uncache(oldEpisode.getUri());
			}
			episodes.remove(oldEpisode);
			episodes.add(newEpisode);

			this.reattachAndSave(oldEpisode.getUri(), newEpisode.getUri());

			this.codeServiceListenerNotifier.episodeReplaced(oldEpisode,
					newEpisode);
		} else {
			throw new EpisodeDoesNotExistException(oldEpisode);
		}
	}

	@Override
	public void deleteEpisodeAndSave(List<IEpisode> episodesToDelete)
			throws CodeServiceException {
		Set<IEpisode> episodes = this.codeStore.getEpisodes();
		Set<IEpisode> deletedEpisodes = new NoNullSet<IEpisode>();
		for (IEpisode episodeToDelete : episodesToDelete) {
			if (episodes.contains(episodeToDelete)) {
				if (LocatorService.INSTANCE != null) {
					LocatorService.INSTANCE.uncache(episodeToDelete.getUri());
				}
				episodes.remove(episodeToDelete);
				this.removeCodes(this.getCodes(episodeToDelete.getUri()),
						episodeToDelete.getUri());
				deletedEpisodes.add(episodeToDelete);
			}
		}

		try {
			this.codeStore.save();
			this.codeServiceListenerNotifier.episodesDeleted(deletedEpisodes);
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		}

		@SuppressWarnings("unchecked")
		Collection<IEpisode> notDeletedEpisodes = CollectionUtils.disjunction(
				episodesToDelete, deletedEpisodes);
		if (notDeletedEpisodes.size() > 0) {
			throw new EpisodeDoesNotExistException(notDeletedEpisodes);
		}
	}

	private static final String AXIAL_CODING_MODEL_TYPE = "acm";

	@Override
	public List<URI> getAxialCodingModels() throws CodeStoreReadException {
		return this.codeStore.getRaw(AXIAL_CODING_MODEL_TYPE);
	}

	@Override
	public IAxialCodingModel getAxialCodingModel(URI uri)
			throws CodeStoreReadException {
		String json = this.codeStore.getRaw(AXIAL_CODING_MODEL_TYPE, uri);
		if (json != null) {
			return new JointJSAxialCodingModel(uri, json);
		}
		return null;
	}

	@Override
	public void addAxialCodingModel(IAxialCodingModel axialCodingModel)
			throws CodeStoreWriteException {
		Assert.isNotNull(axialCodingModel.getUri());
		LocatorService.INSTANCE.uncache(axialCodingModel.getUri());
		try {
			boolean update = this
					.getAxialCodingModel(axialCodingModel.getUri()) != null;
			this.codeStore.setRaw(AXIAL_CODING_MODEL_TYPE,
					axialCodingModel.getUri(), axialCodingModel.serialize());
			if (update) {
				this.codeServiceListenerNotifier
						.axialCodingModelUpdated(axialCodingModel.getUri());
			} else {
				this.codeServiceListenerNotifier
						.axialCodingModelAdded(axialCodingModel.getUri());
			}
		} catch (CodeStoreReadException e) {
			throw new CodeStoreWriteException(e);
		}
	}

	@Override
	public void removeAxialCodingModel(URI uri) throws CodeStoreWriteException {
		Assert.isNotNull(uri);
		LocatorService.INSTANCE.uncache(uri);
		this.codeStore.setRaw(AXIAL_CODING_MODEL_TYPE, uri, null);
		this.codeServiceListenerNotifier.axialCodingModelRemoved(uri);
	}

	@Override
	public void reattachAndSave(URI src, URI dest) throws CodeServiceException {
		if (src == null || dest == null) {
			throw new CodeServiceException(new IllegalArgumentException(
					"Arguments must not be null"));
		}

		if (LocatorService.INSTANCE != null) {
			LocatorService.INSTANCE.uncache(src);
		}

		List<ICode> codes = this.getCodes(src);
		this.removeCodes(codes, src);
		this.addCodes(codes, new LinkedList<URI>(Arrays.asList(dest)));

		String memo = this.loadMemo(src);
		this.setMemo(src, null);
		this.setMemo(dest, memo);
		try {
			this.codeStore.save();
		} catch (CodeStoreWriteException e) {
			throw new CodeServiceException(e);
		}
	}
}