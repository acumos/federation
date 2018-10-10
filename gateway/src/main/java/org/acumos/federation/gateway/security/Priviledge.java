/* 
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
package org.acumos.federation.gateway.security;

import org.springframework.security.core.GrantedAuthority;

/**
 * An enumeratoin of federatiom access priviledges. Organized around the
 * functional sets of the federation and local API.
 */
public enum Priviledge implements GrantedAuthority {

	/**
	 * Granted to a peer to access catalog items (solutions) and related information; coarse at this point, all
	 * (list/read/download) or nothing. The peer must be active.
	 */
	CATALOG_ACCESS,

	/**
	 * Granted to a peer to request information about the peers registered in the local Acumos system. The peer
   * must be active.
	 */
	PEERS_ACCESS,

	/**
	 * The right to submit a subscription request. This is granted to ANY (only if so
	 * enabled system wide).
	 */
	REGISTRATION_ACCESS,

	/**
	 * The right to submit a ping request. Granted to active and inactive peers.
	 */
	PING_ACCESS,

	/**
	 * granted to local Acumos components to request information from peers (local interface access).
	 */
	PEER_ACCESS;

	Priviledge() {
	}

	@Override
	public String getAuthority() {
		return name();
	}
}