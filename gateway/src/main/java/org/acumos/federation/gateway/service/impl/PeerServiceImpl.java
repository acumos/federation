/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;
import org.acumos.federation.gateway.cds.PeerStatuses;
import org.acumos.federation.gateway.config.FederationInterfaceConfiguration;
import org.acumos.federation.gateway.security.Tools;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  CDS based implementation of the hpeer service interface.
 */
@Service
public class PeerServiceImpl extends AbstractServiceImpl implements PeerService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private FederationInterfaceConfiguration fedIfConfig;

	public PeerServiceImpl() {
	}

	/**
	 * Retrieves from CDS the peer record representing this Acumos gateway.
	 * It can tolerate multiple CDS entries marked as 'self', requires a match between the locally configured identity as
	 * it appears in the federation interface configuration and the subjectName attribute of the CDS entry.
	 */
	@Override
	public MLPPeer getSelf() {

		String selfName = null;
		try {
			selfName = Tools.getNameParts(fedIfConfig.getSubjectName(), "CN").get("CN").toString();
		}
		catch(Exception x) {
			log.warn("Cannot obtain 'self' name from interface config " + x);
			return null;
		}
		final String subjectName = selfName;
		log.debug("Expecting 'self' name '{}'", subjectName);

		List<MLPPeer> selfPeers = new ArrayList<>();
		RestPageRequest pageRequest = new RestPageRequest(0, 100);
		RestPageResponse<MLPPeer> pageResponse = null;
		do {
			pageResponse =
				getClient().searchPeers(Collections.singletonMap("self", Boolean.TRUE), false, pageRequest);
			log.debug("Peers representing 'self': " + pageResponse.getContent());

			selfPeers.addAll(
				pageResponse.getContent().stream()
										.filter(peer -> subjectName.equals(peer.getSubjectName()))
										.collect(Collectors.toList()));

			pageRequest.setPage(pageResponse.getNumber() + 1);
		}
		while (!pageResponse.isLast());

		if (selfPeers.size() != 1) {
			log.warn("Number of peers representing 'self', i.e. '{}', not 1. Found {}.", subjectName, selfPeers);
			return null;
		}
		return selfPeers.get(0);
	}

	/**
	 * ToDo:
	 */
	@Override
	public List<MLPPeer> getPeers(ServiceContext theContext) {
		log.debug("getPeers");

		RestPageRequest pageRequest = new RestPageRequest(0, 100);
		RestPageResponse<MLPPeer> pageResponse = null;
		List<MLPPeer> peers = new ArrayList<>();
		ICommonDataServiceRestClient cdsClient = getClient();

		do {
			pageResponse = cdsClient.getPeers(pageRequest);
			peers.addAll(pageResponse.getContent());
		
			pageRequest.setPage(pageResponse.getNumber() + 1);
		}
		while (!pageResponse.isLast());

		return peers;
	}

	@Override
	public List<MLPPeer> getPeerBySubjectName(String theSubjectName, ServiceContext theContext) {
		log.debug("getPeerBySubjectName");
		RestPageResponse<MLPPeer> response = 
			getClient().searchPeers(Collections.singletonMap("subjectName", theSubjectName), false, null);
		if (response.getNumberOfElements() != 1) {
			log.warn("getPeerBySubjectName returned more then one peer: {}", response.getNumberOfElements());
		}
		return response.getContent();
	}

	@Override
	public MLPPeer getPeerById(String thePeerId, ServiceContext theContext) {
		log.debug("getPeerById: {}", thePeerId);
		MLPPeer mlpPeer = getClient().getPeer(thePeerId);
		if (mlpPeer != null) {
			log.info("getPeerById: {}", mlpPeer);
		}
		return mlpPeer;
	}

	@Override
	public void registerPeer(MLPPeer thePeer) throws ServiceException {
		log.debug("registerPeer");

		String subjectName = thePeer.getSubjectName();
		if (subjectName == null)
			throw new ServiceException("No subject name is available");

		ICommonDataServiceRestClient cdsClient = getClient();
		RestPageResponse<MLPPeer> response = 
			cdsClient.searchPeers(Collections.singletonMap("subjectName", subjectName), false, null);

		if (response.getNumberOfElements() > 0) {
			assertPeerRegistration(response.getContent().get(0));
		}

		log.info("registerPeer: new peer with subjectName {}, create CDS record",
				thePeer.getSubjectName());
		//enforce
		thePeer.setStatusCode(PeerStatuses.Requested.getCode());

		try {
			cdsClient.createPeer(thePeer);
		}
		catch (Exception x) {
			throw new ServiceException("Failed to create peer", x);
		}
	}

	@Override
	public void unregisterPeer(MLPPeer thePeer) throws ServiceException {
		log.debug("unregisterPeer");

		String subjectName = thePeer.getSubjectName();
		if (subjectName == null)
			throw new ServiceException("No subject name is available");

		ICommonDataServiceRestClient cdsClient = getClient();
		RestPageResponse<MLPPeer> response = 
			cdsClient.searchPeers(Collections.singletonMap("subjectName", subjectName), false, null);

		if (response.getNumberOfElements() != 1) {
			throw new ServiceException("Search for peer with subjectName '" + subjectName + "' yielded invalid number of items: " + response.getNumberOfElements());
		}

		MLPPeer peer = response.getContent().get(0);
		assertPeerUnregistration(peer);

		//active/inactive peers moved to renounced
		log.info("unregisterPeer: peer with subjectName {}, update CDS record",
				thePeer.getSubjectName());
		thePeer.setStatusCode(PeerStatuses.Renounced.getCode());

		try {
			cdsClient.updatePeer(thePeer);
		}
		catch (Exception x) {
			throw new ServiceException("Failed to update peer", x);
		}
	}

}