/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
 * ===================================================================================
 * This Acumos software file is distributed by AT&T and Tech Mahindra
 * under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ===============LICENSE_END=========================================================
 */

/**
 * 
 */
package org.acumos.federation.gateway.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Collections;
import java.util.stream.Collectors;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;

import org.acumos.federation.gateway.util.Utils;
import org.acumos.federation.gateway.util.Errors;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;

import org.acumos.nexus.client.NexusArtifactClient;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageResponse;
import org.acumos.cds.transport.RestPageRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.client.HttpStatusCodeException;

import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.Artifact;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * CDS based implementation of the CatalogService.
 *
 */
@Service
public class CatalogServiceImpl extends AbstractServiceImpl
																implements CatalogService {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private Environment env;

	private Map<String, Object> baseSolutionSelector,
	/*private List<Predicate<MLPSolutionRevision>>*/	baseSolutionRevisionSelector;

	@PostConstruct
	public void initService() {
		baseSolutionSelector = new HashMap<String, Object>();
		// Fetch all active solutions
		baseSolutionSelector.put(Solution.Fields.active, true);
		// Fetch allowed only for Public models
		baseSolutionSelector.put(Solution.Fields.accessTypeCode, AccessTypeCode.PB.toString());
		// Validation status should be passed locally
		baseSolutionSelector.put(Solution.Fields.validationStatusCode, ValidationStatusCode.PS.toString());

		baseSolutionRevisionSelector = new HashMap<String, Object>();
		// Fetch allowed only for Public revisions
		baseSolutionRevisionSelector.put(SolutionRevision.Fields.accessTypeCode, AccessTypeCode.PB.toString());
		// Validation status should be passed locally
		baseSolutionRevisionSelector.put(SolutionRevision.Fields.validationStatusCode, ValidationStatusCode.PS.toString());
	}

	@Override
	/*
	 */
	public List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions with selector {}", theSelector);

		Map<String, Object> selector = new HashMap<String, Object>();
		if (theSelector != null)
			selector.putAll(theSelector);
		//it is essential that this gets done at the end as to force all baseSelector criteria (otherwise a submitted accessTypeCode
		//could overwrite the basic one end expose non public solutions ..).
		selector.putAll(this.baseSolutionSelector);
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions with full selector {}", selector);

		RestPageRequest pageRequest = new RestPageRequest(0, 50);
		RestPageResponse<MLPSolution> pageResponse = null;
		List<MLPSolution> solutions = new ArrayList<MLPSolution>(),
											pageSolutions = null;
		ICommonDataServiceRestClient cdsClient = getClient();
		try {
			do {
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutions page {}", pageResponse);
				if (selector.containsKey(Solution.Fields.modified)) {
					//Use the dedicated api: this is a 'deep' application of the 'modified' criteria as it will look into revisions
					//and artifacts for related information modified since.
					pageResponse =
						cdsClient.findSolutionsByDate(
							(Boolean)selector.get(Solution.Fields.active),
							new String[] {selector.get(Solution.Fields.accessTypeCode).toString()},
							new String[] {selector.get(Solution.Fields.validationStatusCode).toString()},
							new Date((Long)selector.get(Solution.Fields.modified)),
							pageRequest);
			
					//we need to post-process all other selection criteria
					pageSolutions = pageResponse.getContent().stream()
														.filter(solution -> ServiceImpl.isSelectable(solution, theSelector))
														.collect(Collectors.toList());
				}
				else {
					pageResponse =
						cdsClient.findPortalSolutions(selector.containsKey(Solution.Fields.name) ?
																						new String[] {selector.get(Solution.Fields.name).toString()} :
																						null,
																					selector.containsKey(Solution.Fields.description) ?
																						new String[] {selector.get(Solution.Fields.description).toString()} :
																						null,
																					(Boolean)selector.get(Solution.Fields.active),
																					null, //user ids
																					new String[] {selector.get(Solution.Fields.accessTypeCode).toString()},
																					selector.containsKey(Solution.Fields.modelTypeCode) ?
																						new String[] {selector.get(Solution.Fields.modelTypeCode).toString()} :
																						null,
																					new String[] {selector.get(Solution.Fields.validationStatusCode).toString()},
																					selector.containsKey(Solution.Fields.tags) ?
																						new String[] {selector.get(Solution.Fields.tags).toString()} :
																						null,
																					null,	//authorKeywords
																					null, //publisherKeywords
																					pageRequest);
/*
	RestPageResponse<MLPSolution> findPortalSolutions(
																					String[] nameKeywords,
																					String[] descriptionKeywords,
																					boolean active,
																					String[] userIds,
																					String[] accessTypeCodes,
																					String[] modelTypeCodes,
																					String[] validationStatusCodes,
																					String[] tags,
																					String[] authorKeywords,
																					String[] publisherKeywords,
			RestPageRequest pageRequest);
*/
					pageSolutions = pageResponse.getContent();
				}
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutions page response {}", pageResponse);
		
				pageRequest.setPage(pageResponse.getNumber() + 1);
				solutions.addAll(pageSolutions);
			}
			while (!pageResponse.isLast());
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return Collections.EMPTY_LIST;
			else {
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutions failed {}: {}", restx, restx.getResponseBodyAsString());
				throw new ServiceException("Failed to retrieve solutions", restx);
			}
		}

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions: solutions count {}", solutions.size());
		return solutions;
	}

	@Override
	public Solution getSolution(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolution {}", theSolutionId);
		ICommonDataServiceRestClient cdsClient = getClient();
		try {
			Solution solution = (Solution)cdsClient.getSolution(theSolutionId);
			List<MLPSolutionRevision> revisions = getSolutionRevisions(theSolutionId, theContext.withAttribute(Attributes.cdsClient, cdsClient));

			//we can expose this solution only if we can expose at least one revision
			if (revisions == null || revisions.isEmpty())
				return null;

			solution.setRevisions(revisions);
			return solution;
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution information", restx);
		}
	}

	@Override
	public List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisions");
		try {
			List<MLPSolutionRevision> revisions = getClient(theContext).getSolutionRevisions(theSolutionId);
			//make sure we only expose revisions according to the filter
			if (revisions != null) {
				revisions = 
					revisions.stream()
									 .filter(revision -> baseSolutionRevisionSelector.entrySet().stream()
																				.allMatch(s -> { try {
																													return PropertyUtils.getProperty(revision, s.getKey()).equals(s.getValue());
																												 } catch (Exception x) { return false; }
																											 })
													)
									 .collect(Collectors.toList());
			}
			return revisions;
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision information", restx);
		}
	}


	@Override
	public SolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevision");
		ICommonDataServiceRestClient cdsClient = getClient();
		try {
			SolutionRevision revision =
					(SolutionRevision)cdsClient.getSolutionRevision(theSolutionId, theRevisionId);
			revision.setArtifacts(cdsClient.getSolutionRevisionArtifacts(theSolutionId, theRevisionId));
			return revision;
		}	
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision information", restx);
		}
	}

	@Override
	public List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts");
		try {
			return getClient().getSolutionRevisionArtifacts(theSolutionId, theRevisionId);
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution information", restx);
		}
	}

	/**
	 * @return catalog artifact representation
	 * @throws ServiceException if failing to retrieve artifact information or retrieve content 
	 */
	@Override
	public Artifact getSolutionRevisionArtifact(String theArtifactId, ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifact");
		try {
			return (Artifact)getClient().getArtifact(theArtifactId);
		}	
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision artifact information", restx);
		}
	}

}
