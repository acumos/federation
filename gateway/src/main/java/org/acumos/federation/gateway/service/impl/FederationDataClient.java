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

package org.acumos.federation.gateway.service.impl;

import java.net.URI;
import java.util.List;

import org.acumos.cds.CCDSConstants;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.apache.http.client.HttpClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Enhance the CDS API with federation specific api
 */
public class FederationDataClient extends AbstractClient {

	public FederationDataClient(String theTarget, HttpClient theClient) {
		super(theTarget, theClient);
	}

	public List<MLPPeerSubscription> getPeerSubscriptions(String peerId) throws HttpStatusCodeException {
		URI uri = buildUri(new String[] { CCDSConstants.PEER_PATH, peerId, CCDSConstants.SUBSCRIPTION_PATH }, null,
				null);

		logger.info(EELFLoggerDelegate.debugLogger, "getPeerSubscriptions: uri " + uri);
		/*
		 * ResponseEntity<RestPageResponse<MLPPeerSubscription>> response =
		 * this.restTemplate.exchange( uri, HttpMethod.GET, null, new
		 * ParameterizedTypeReference<RestPageResponse<MLPPeerSubscription>>() {});
		 */
		ResponseEntity<List<MLPPeerSubscription>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
				new ParameterizedTypeReference<List<MLPPeerSubscription>>() {
				});
		return response.getBody();
	}

	public Iterable<MLPArtifact> getSolutionRevisionArtifacts(String solutionId, String revisionId)
			throws HttpStatusCodeException {
		URI uri = buildUri(new String[] { CCDSConstants.SOLUTION_PATH, solutionId, CCDSConstants.REVISION_PATH,
				revisionId, CCDSConstants.ARTIFACT_PATH }, null, null);
		logger.debug("getSolutionRevisionArtifacts: uri {}", uri);
		ResponseEntity<Iterable<MLPArtifact>> response = this.restTemplate.exchange(uri, HttpMethod.GET, null,
				new ParameterizedTypeReference<Iterable<MLPArtifact>>() {
				});
		return response.getBody();
	}

}
