package de.fu_berlin.imp.apiua.doclog.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.bkahlert.nebula.utils.ExecUtils;

import de.fu_berlin.imp.apiua.core.model.TimeZoneDateRange;
import de.fu_berlin.imp.apiua.core.model.data.IBaseDataContainer;
import de.fu_berlin.imp.apiua.core.model.data.IData;
import de.fu_berlin.imp.apiua.core.model.data.IDataContainer;
import de.fu_berlin.imp.apiua.core.model.data.impl.AggregatedBaseDataContainer;
import de.fu_berlin.imp.apiua.core.model.identifier.IIdentifier;
import de.fu_berlin.imp.apiua.core.model.identifier.Token;
import de.fu_berlin.imp.apiua.doclog.util.DoclogCache;

public class DoclogDataContainer extends AggregatedBaseDataContainer {

	private static final Logger LOGGER = Logger
			.getLogger(DoclogDataContainer.class);

	public static final int DOCLOG_CACHE_SIZE = 10;

	private static Map<IIdentifier, IData> readDoclogFileMappings(
			DoclogDataContainer directory) {
		Map<IIdentifier, IData> rawDataResource = new HashMap<IIdentifier, IData>();
		for (IData doclogDataResource : directory.getDoclogDirectory()
				.getResources()) {
			if (!Doclog.IDENTIFIER_PATTERN
					.matcher(doclogDataResource.getName()).matches()) {
				continue;
			}
			IIdentifier id = Doclog.getIdentifier(doclogDataResource);
			rawDataResource.put(id, doclogDataResource);
		}
		return rawDataResource;
	}

	private Map<IIdentifier, IData> datas;
	private Map<IIdentifier, TimeZoneDateRange> fileDateRanges;
	private Map<IIdentifier, Token> fileToken;

	private final IDataContainer doclogDirectory;
	private final IData mappingFile;
	private final DoclogCache doclogCache;

	public DoclogDataContainer(
			List<? extends IBaseDataContainer> baseDataContainers) {
		super(baseDataContainers);
		this.doclogDirectory = this.getSubContainer("doclog");
		this.mappingFile = this.getResource("mapping.xml");
		this.doclogCache = new DoclogCache(this, DOCLOG_CACHE_SIZE);
	}

	public DoclogDataContainer(IBaseDataContainer dataResourceContainer) {
		this(Arrays.asList(dataResourceContainer));
	}

	public IDataContainer getDoclogDirectory() {
		return this.doclogDirectory;
	}

	public IData getMappingFile() {
		return this.mappingFile;
	}

	public void scan(final SubMonitor monitor) {
		this.datas = readDoclogFileMappings(this);
		this.fileDateRanges = new HashMap<IIdentifier, TimeZoneDateRange>(
				this.datas.size());
		this.fileToken = new HashMap<IIdentifier, Token>(this.datas.size());

		long size = 0;
		for (Object key : this.datas.keySet()) {
			size += this.datas.get(key).getLength();
		}
		monitor.beginTask("Loading " + this.getName(), (int) (size / 1000l));

		// force class loading since DoclogRecord is used in the Callable
		DoclogAction.class.getClass();
		DoclogRecord.class.getClass();
		for (int worked : ExecUtils.nonUIAsyncExecMerged(
				DoclogDataContainer.class,
				"Loading " + StringUtils.join(this.datas.keySet(), ", "),
				this.datas.keySet(),
				new ExecUtils.ParametrizedCallable<IIdentifier, Integer>() {
					@Override
					public Integer call(IIdentifier identifier)
							throws Exception {
						final IData data = DoclogDataContainer.this.datas
								.get(identifier);
						long length = data.getLength();
						if (length == 0l) {
							return null;
						}
						try {
							TimeZoneDateRange dateRange = Doclog
									.getDateRange(data);
							Token token = Doclog.getToken(data);
							synchronized (DoclogDataContainer.this.fileDateRanges) {
								DoclogDataContainer.this.fileDateRanges.put(
										identifier, dateRange);
							}
							synchronized (DoclogDataContainer.this.fileToken) {
								DoclogDataContainer.this.fileToken.put(
										identifier, token);
							}
						} catch (Exception e) {
							LOGGER.error(e);
						}
						return (int) (length / 1000l);
					}
				})) {
			monitor.worked(worked);
		}
		monitor.done();
	}

	/**
	 * Returns a list of all {@link IIdentifier}s occurring in the managed
	 * {@link Doclog}s
	 * 
	 * @return
	 */
	public IIdentifier[] getIdentifiers() {
		return this.datas.keySet().toArray(new IIdentifier[0]);
	}

	/**
	 * Returns a list of the {@link IIdentifier}s that are associated to a
	 * {@link Doclog} containing the specified {@link Token}.
	 * 
	 * @param token
	 * @return
	 */
	public IIdentifier[] getIdentifiers(Token token) {
		LinkedList<IIdentifier> identifiers = new LinkedList<IIdentifier>();
		for (IIdentifier identifier : this.fileToken.keySet()) {
			if (this.fileToken.get(identifier) != null
					&& this.fileToken.get(identifier).equals(token)) {
				identifiers.add(identifier);
			}
		}
		return identifiers.toArray(new IIdentifier[0]);
	}

	public Token getToken(IIdentifier identifier) {
		for (IIdentifier currentIdentifier : this.fileToken.keySet()) {
			if (currentIdentifier.equals(identifier)) {
				return this.fileToken.get(identifier);
			}
		}
		return null;
	}

	public TimeZoneDateRange getDateRange(Object key) {
		return this.fileDateRanges.get(key);
	}

	public IData getFile(IIdentifier identifier) {
		return this.datas.get(identifier);
	}

	public boolean doclogFileLoaded(IIdentifier identifier) {
		return this.doclogCache.getCachedKeys().contains(identifier);
	}

	/**
	 * Returns the {@link Doclog} associated with a given key using an internal
	 * {@link DoclogCache}.
	 * 
	 * @param identifier
	 * @param progressMonitor
	 * @return
	 */
	public Doclog getDoclogFile(IIdentifier identifier,
			IProgressMonitor progressMonitor) {
		return this.doclogCache.getPayload(identifier, progressMonitor);
	}

	/**
	 * Returns the {@link Doclog} associated with a given key.
	 * <p>
	 * In contrast to {@link #getDoclogFile(Object, IProgressMonitor)} this
	 * method always creates the needed objects anew without using any cache.
	 * 
	 * @param identifier
	 * @param progressMonitor
	 * @return
	 */
	public Doclog readDoclogFromSource(IIdentifier identifier,
			IProgressMonitor progressMonitor) {
		progressMonitor.beginTask("Parsing " + Doclog.class.getSimpleName(), 2);
		IData data = this.datas.get(identifier);
		if (data == null) {
			return null;
		}

		TimeZoneDateRange dateRange = this.fileDateRanges.get(identifier);
		Token token = this.fileToken.get(identifier);
		progressMonitor.worked(1);
		Doclog doclog = new Doclog(data, identifier, dateRange, token, null);
		progressMonitor.worked(1);
		progressMonitor.done();
		return doclog;
	}
}
