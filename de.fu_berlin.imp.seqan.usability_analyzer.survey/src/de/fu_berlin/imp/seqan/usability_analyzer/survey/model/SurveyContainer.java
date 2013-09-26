package de.fu_berlin.imp.seqan.usability_analyzer.survey.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.SubMonitor;

import de.fu_berlin.imp.seqan.usability_analyzer.core.model.data.IBaseDataContainer;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.data.IData;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.data.impl.AggregatedBaseDataContainer;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.identifier.IIdentifier;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.identifier.Token;
import de.fu_berlin.imp.seqan.usability_analyzer.survey.model.csv.CSVSurveyManager;
import de.fu_berlin.imp.seqan.usability_analyzer.survey.model.csv.CSVSurveyRecord;

/**
 * Encapsulates multiple {@link IBaseDataContainer}s containing surveys.
 * <p>
 * Each survey is managed by a {@link XMLSurveyManager}.
 * 
 * @author bkahlert
 * 
 */
public class SurveyContainer extends AggregatedBaseDataContainer {

	private static final Logger LOGGER = Logger
			.getLogger(SurveyContainer.class);

	private List<? extends IBaseDataContainer> baseContainers;

	private List<CSVSurveyManager> cSVSurveyManagers;

	public SurveyContainer(List<? extends IBaseDataContainer> baseContainers) {
		super(baseContainers);
		this.baseContainers = baseContainers;
	}

	public IData getSurveyData(IBaseDataContainer baseDataContainer) {
		IData surveyData = null;
		Map<String, String> props = baseDataContainer.getInfo()
				.getUnknownProperties();
		for (String key : props.keySet()) {
			if (key.equals("surveyLocation")) {
				surveyData = this.getResource(props.get(key));
				break;
			}
		}
		return surveyData;
	}

	public void scan(SubMonitor subMonitor) {
		SubMonitor monitor = SubMonitor.convert(subMonitor,
				this.baseContainers.size());

		List<CSVSurveyManager> cSVSurveyManagers = new ArrayList<CSVSurveyManager>();
		for (IBaseDataContainer baseDataContainer : this.baseContainers) {
			IData surveyData = this.getSurveyData(baseDataContainer);
			if (surveyData == null) {
				LOGGER.error("Could not load survey data");
			} else {
				CSVSurveyManager cSVSurveyManager = new CSVSurveyManager(surveyData);
				cSVSurveyManager.scanRecords(monitor.newChild(1));
				cSVSurveyManagers.add(cSVSurveyManager);
			}
		}

		this.cSVSurveyManagers = cSVSurveyManagers;
		monitor.done();
	}

	/**
	 * TODO aggregate in case of multiple hits
	 * 
	 * @param identifier
	 * @return
	 */
	public CSVSurveyRecord getSurveyRecord(IIdentifier identifier) {
		for (CSVSurveyManager cSVSurveyManager : this.cSVSurveyManagers) {
			CSVSurveyRecord cSVSurveyRecord = cSVSurveyManager
					.getSurveyRecord(identifier);
			if (cSVSurveyRecord != null) {
				return cSVSurveyRecord;
			}
		}
		return null;
	}

	public List<Token> getTokens() {
		List<Token> tokens = new ArrayList<Token>();
		for (CSVSurveyManager cSVSurveyManager : this.cSVSurveyManagers) {
			List<Token> currentTokens = cSVSurveyManager.getTokens();
			if (currentTokens != null) {
				tokens.addAll(currentTokens);
			}
		}
		return tokens;
	}
}
