package de.fu_berlin.imp.apiua.entity.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.fu_berlin.imp.apiua.core.model.HasIdentifier;
import de.fu_berlin.imp.apiua.core.model.ILocatable;
import de.fu_berlin.imp.apiua.core.model.TimeZoneDate;
import de.fu_berlin.imp.apiua.core.model.TimeZoneDateRange;
import de.fu_berlin.imp.apiua.core.model.URI;
import de.fu_berlin.imp.apiua.core.model.identifier.Fingerprint;
import de.fu_berlin.imp.apiua.core.model.identifier.ID;
import de.fu_berlin.imp.apiua.core.model.identifier.IIdentifier;
import de.fu_berlin.imp.apiua.core.model.identifier.Token;
import de.fu_berlin.imp.apiua.core.services.IWorkSessionEntity;
import de.fu_berlin.imp.apiua.core.ui.viewer.filters.HasDateRange;
import de.fu_berlin.imp.apiua.diff.Activator;
import de.fu_berlin.imp.apiua.diff.model.DiffContainer;
import de.fu_berlin.imp.apiua.doclog.model.DoclogDataContainer;
import de.fu_berlin.imp.apiua.entity.NoInternalIdentifierException;
import de.fu_berlin.imp.apiua.entity.gt.EntityLocatorProvider;
import de.fu_berlin.imp.apiua.entity.mapping.Mapper;
import de.fu_berlin.imp.apiua.stats.model.CMakeCacheFile;
import de.fu_berlin.imp.apiua.stats.model.StatsFile;
import de.fu_berlin.imp.apiua.survey.model.csv.CSVSurveyRecord;

public class Entity implements HasDateRange, ILocatable, IWorkSessionEntity,
		HasIdentifier {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(Entity.class);

	private static final long serialVersionUID = 1969965491590110977L;

	private URI uri = null;
	private ID id;
	private Fingerprint fingerprint;
	private CSVSurveyRecord cSVSurveyRecord;

	private StatsFile statsFile;
	private CMakeCacheFile cMakeCacheFile;

	private final de.fu_berlin.imp.apiua.entity.mapping.Mapper mapper;

	/* cached fields for performance reasons */
	private TimeZoneDate earliestEntryDate;
	private TimeZoneDate latestEntryDate;

	public Entity(Mapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public URI getUri() {
		if (this.uri == null) {
			try {
				this.uri = new URI("apiua://"
						+ EntityLocatorProvider.ENTITY_NAMESPACE + "/"
						+ this.getInternalId());
			} catch (Exception e) {
				throw new RuntimeException("Error calculating " + URI.class
						+ " for " + Entity.class, e);
			}
		}
		return this.uri;
	}

	@Override
	public IIdentifier getIdentifier() {
		if (this.getId() != null) {
			return this.getId();
		} else if (this.getFingerprints().size() > 0) {
			return this.getFingerprints().get(0);
		} else if (this.getToken() != null) {
			return this.getToken();
		} else {
			throw new RuntimeException(new NoInternalIdentifierException(this));
		}
	}

	public String getInternalId() throws NoInternalIdentifierException {
		ID id = this.getId();
		if (id != null) {
			return id.toString();
		}

		Token token = this.getToken();
		if (token != null) {
			return token.toString();
		}

		List<Fingerprint> fingerprints = this.getFingerprints();
		if (fingerprints != null && fingerprints.size() > 0) {
			return StringUtils.join(fingerprints, ",");
		}

		throw new NoInternalIdentifierException(this);
	}

	public boolean isValid() {
		try {
			this.getInternalId();
		} catch (NoInternalIdentifierException e) {
			return false;
		}
		return true;
	}

	public void setId(ID id) {
		if (this.id != null && this.id.equals(id)) {
			return;
		}

		DiffContainer diffFileDirectory = Activator.getDefault()
				.getDiffDataContainer();
		TimeZoneDateRange diffFilesDateRange = diffFileDirectory
				.getDateRange(id);
		if (diffFilesDateRange != null) {
			this.updateEarliestEntryDate(diffFilesDateRange.getStartDate());
			this.updateLatestEntryDate(diffFilesDateRange.getEndDate());
		}

		DoclogDataContainer doclogFileDirectory = de.fu_berlin.imp.apiua.doclog.Activator
				.getDefault().getDoclogContainer();
		TimeZoneDateRange doclogFileDateRange = doclogFileDirectory
				.getDateRange(id);
		if (doclogFileDateRange != null) {
			this.updateEarliestEntryDate(doclogFileDateRange.getStartDate());
			this.updateLatestEntryDate(doclogFileDateRange.getStartDate());
		}

		this.id = id;
	}

	public ID getId() {
		return this.id;
	}

	public void setFingerprint(Fingerprint fingerprint) {
		this.fingerprint = fingerprint;

		DoclogDataContainer doclogFileDirectory = de.fu_berlin.imp.apiua.doclog.Activator
				.getDefault().getDoclogContainer();
		TimeZoneDateRange doclogFileDateRange = doclogFileDirectory
				.getDateRange(fingerprint);
		this.updateEarliestEntryDate(doclogFileDateRange.getStartDate());
		this.updateLatestEntryDate(doclogFileDateRange.getStartDate());
	}

	public List<Fingerprint> getFingerprints() {
		List<Fingerprint> fingerprints = new ArrayList<Fingerprint>();
		ID id = this.getId();
		if (id != null) {
			fingerprints.addAll(this.mapper.getFingerprints(id));
		} else {
			fingerprints.add(this.fingerprint);
		}
		return fingerprints;
	}

	public Token getToken() {
		if (this.cSVSurveyRecord != null) {
			return this.cSVSurveyRecord.getToken();
		} else {
			return null;
		}
	}

	public CSVSurveyRecord getSurveyRecord() {
		return this.cSVSurveyRecord;
	}

	public void setSurveyRecord(CSVSurveyRecord cSVSurveyRecord) {
		if (cSVSurveyRecord != null) {
			this.updateEarliestEntryDate(cSVSurveyRecord.getDate());
			this.updateLatestEntryDate(cSVSurveyRecord.getDate());
		}
		this.cSVSurveyRecord = cSVSurveyRecord;
	}

	public StatsFile getStatsFile() {
		return this.statsFile;
	}

	public void setStatsFile(StatsFile statsFile) {
		this.statsFile = statsFile;
	}

	public CMakeCacheFile getCMakeCacheFile() {
		return this.cMakeCacheFile;
	}

	public void setCMakeCacheFile(CMakeCacheFile cMakeCacheFile) {
		this.cMakeCacheFile = cMakeCacheFile;
	}

	private void updateEarliestEntryDate(TimeZoneDate date) {
		if (date == null) {
			return;
		}
		if (this.earliestEntryDate == null) {
			this.earliestEntryDate = date;
		} else if (this.earliestEntryDate.getTime() > date.getTime()) {
			this.earliestEntryDate = date;
		}
	}

	private void updateLatestEntryDate(TimeZoneDate date) {
		if (date == null) {
			return;
		}
		if (this.latestEntryDate == null) {
			this.latestEntryDate = date;
		} else if (this.latestEntryDate.getTime() < date.getTime()) {
			this.latestEntryDate = date;
		}
	}

	public TimeZoneDate getEarliestEntryDate() {
		return this.earliestEntryDate;
	}

	public TimeZoneDate getLatestEntryDate() {
		return this.latestEntryDate;
	}

	@Override
	public TimeZoneDateRange getDateRange() {
		return new TimeZoneDateRange(this.earliestEntryDate,
				this.latestEntryDate);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.mapper == null) ? 0 : this.mapper.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		Entity other = (Entity) obj;
		if (this.getInternalId() == null) {
			if (other.getInternalId() != null) {
				return false;
			}
		} else if (!this.getInternalId().equals(other.getInternalId())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return Entity.class.getSimpleName() + ": " + this.getInternalId();
	}

}
