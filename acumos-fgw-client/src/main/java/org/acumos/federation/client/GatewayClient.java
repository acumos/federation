/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.client;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;


import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPSolution;

import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.data.JsonResponse;

/**
 * Client for the Federation Gateway's local API.
 * @see FederationClient
 */
public class GatewayClient extends ClientBase {
	/**
	 * The URI prefix for specifying what peer the request refers to.
	 */
	public static final String PEER_PFX = "/peer/{peerId}";

	/**
	 * The base URI for triggering subscriptions.
	 */
	public static final String SUBSCRIPTION_URI = "/subscription/{subscriptionId}";

	/**
	 * Create a Gateway Client with the default mapper and resource loader.
	 *
	 * @param target The base URL for the server to be accessed.
	 * @param conf The configuration for certificates and credentials.
	 */
	public GatewayClient(String target, ClientConfig conf) {
		this(target, conf, null, null);
	}

	/**
	 * Create a Gateway Client.
	 * If mapper is null, the default mapper is used.  If loader is
	 * null, a DefaultResourceLoader is created and used.  The loader
	 * is used for accessing the key store and trust store for TLS
	 * certificates.
	 *
	 * @param target The base URL for the server to be accessed.
	 * @param conf The configuration for certificates and credentials.
	 * @param mapper The object mapper.
	 * @param loader The resource loader.
	 */
	public GatewayClient(String target, ClientConfig conf, ObjectMapper mapper, ResourceLoader loader) {
		super(target, conf, mapper, loader);
	}

	/**
	 * Ping the peer Acumos.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @return Information about the peer.
	 */
	public MLPPeer ping(String peerId) {
		return handleResponse(PEER_PFX + FederationClient.PING_URI, new ParameterizedTypeReference<JsonResponse<MLPPeer>>(){}, peerId);
	}

	/**
	 * Ask the peer about its peers.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @return The list of the peer's peers.
	 */
	public List<MLPPeer> getPeers(String peerId) {
		return handleResponse(PEER_PFX + FederationClient.PEERS_URI, new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>(){}, peerId);
	}

	/**
	 * Register with the peer.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @return Information about the peer.
	 */
	public MLPPeer register(String peerId) {
		return handleResponse(PEER_PFX + FederationClient.REGISTER_URI, HttpMethod.POST, new ParameterizedTypeReference<JsonResponse<MLPPeer>>(){}, peerId);
	}

	/**
	 * Ask the peer for a list of catalogs.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @return The list of catalogs (enhanced with their sizes), the peer is willing to share.
	 */
	public List<MLPCatalog> getCatalogs(String peerId) {
		return handleResponse(PEER_PFX + FederationClient.CATALOGS_URI, new ParameterizedTypeReference<JsonResponse<List<MLPCatalog>>>(){}, peerId);
	}

	/**
	 * Ask the peer for a list of solutions.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @param catalogId The ID of the catalog to query.
	 * @return The list of solutions in the peer's catalog.
	 */
	public List<MLPSolution> getSolutions(String peerId, String catalogId) {
		return handleResponse(PEER_PFX + FederationClient.SOLUTIONS_URI + FederationClient.CATID_QUERY, new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>(){}, peerId, catalogId);
	}

	/**
	 * Ask the peer for solution metadata.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @param solutionId The ID of the solution.
	 * @return The solution's metadata, enhanced with its picture and revisions.
	 */
	public MLPSolution getSolution(String peerId, String solutionId) {
		return handleResponse(PEER_PFX + FederationClient.SOLUTION_URI, new ParameterizedTypeReference<JsonResponse<MLPSolution>>(){}, peerId, solutionId);
	}

	/**
	 * Ask the federation gateway to poll a subscription. 
	 *
	 * @param peerId The ID of the peer Acumos being subscribed.
	 * @param subscriptionId The ID of the local Acumos' subscription to the peer.
	 */
	public void triggerPeerSubscription(String peerId, long subscriptionId) {
		handleResponse(PEER_PFX + SUBSCRIPTION_URI, HttpMethod.POST, new ParameterizedTypeReference<JsonResponse<Void>>(){}, peerId, subscriptionId);
	}
}
