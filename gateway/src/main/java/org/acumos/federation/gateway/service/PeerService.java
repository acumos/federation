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

package org.acumos.federation.gateway.service;

import java.util.List;

import org.acumos.cds.domain.MLPPeer;

/**
 * Defines the interface of a service providing local peer information.
 *
 */
public interface PeerService {

	/**
	 * Provides the information for the 'self' peer.
	 * Needs to avoid the 'chicken,egg' problem: one needs a context to access
	 * peers, including the 'self' peer.
	 * Strictly an internal service call.
	 *
	 * @return peer information for the local Acumos system
	 */
	public MLPPeer getSelf();

	/**
	 * Provide the list of locally registered peers to one of our peers
	 * It is the responsability of the implementation to decide which peer
	 * information to expose.
	 *
	 * @param theContext the execution context
	 * @return lits ot peers for the local acumoms system
	 */
	public List<MLPPeer> getPeers(ServiceContext theContext);

	/**
	 * Default peer info access on half of the local Acumos system
	 *
	 * @return list of peers for the local acumoms system
	 */	
	public default List<MLPPeer> getPeers() {
		return getPeers(ServiceContext.selfService());
	}
	
	/**
	 * Lookup peer info by subject name. Not sure that this call should be offered
	 * based on a context, it isi strictly a local call.
	 *
	 * @param theSubjectName peer registered subject name
	 * @param theContext the execution context
	 * @return list of peers with the given subject name. Should contain only
	 *				one entry.
	 */
	public List<MLPPeer> getPeerBySubjectName(
													String theSubjectName, ServiceContext theContext);

	/**
	 * Lookup peer info by subject name. Call on behalf of local Acumos system.
   * 
	 * @param theSubjectName peer registered subject name
	 * @return list of peers with the given subject name. Should contain only
	 *				 one entry.
	 */	
	public default List<MLPPeer> getPeerBySubjectName(String theSubjectName) {
		return getPeerBySubjectName(theSubjectName, ServiceContext.selfService());
	}
	
	/**
	 * Retrieve peer information based on peer identifier.
	 * @param thePeerId peer identifier 
	 * @param theContext the execution context
	 * @return peer information
	 */
	public MLPPeer getPeerById(String thePeerId, ServiceContext theContext) {
	}

	/**
	 * Retrieve peer information based on peer identifier. Call on bahalf of local
	 * Acumos system.
	 * @param thePeerId peer identifier 
	 * @return peer information
	 */
	public default MLPPeer getPeerById(String thePeerId) {
		return getPeerById(thePeerId, ServiceContext.selfService());
	}
	
	/**
	 * Optional operation allowing the gateway to provision a peer in some
	 * initial state as part of a in-band peer handshake mechanism.
	 * The whole handshake procedure is to be completed elsewhere (portal);
	 * We do not pass a context as this operation is performed with
	 * frespect to 'self' (to reconsider).
	 *
	 * @param thePeer new peer info to be submitted to the platform
	 * @throws ServiceException if anything goes wrong during the check/provisioning process
	 */
	public void subscribePeer(MLPPeer thePeer) throws ServiceException;
	
	/**
	 * Optional operation allowing the gateway to update a peer and mark it for
	 * removal as part of a in-band peer handshake mechanism.
	 *
	 * @param thePeer MLPPeer New Peer info to be submitted to the platform
	 * @throws ServiceException if anything goes wrong during the check/provisioning process
	 */
	public void unsubscribePeer(MLPPeer thePeer) throws ServiceException;
	
	
}
