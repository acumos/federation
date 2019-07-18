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

import java.util.Collection;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.federation.gateway.cds.PeerStatuses;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Peers constitute the users of the federation gateway.
 */
public class Peer extends User {

	private MLPPeer peerInfo;

	public Peer(MLPPeer thePeerInfo, Role theRole) {
		this(thePeerInfo, theRole.priviledges());
	}

	public Peer(MLPPeer thePeerInfo, Collection<? extends GrantedAuthority> theAuthorities) {
		super(thePeerInfo.getName(), "", true, true, true, true, theAuthorities);
		this.peerInfo = thePeerInfo;
	}

	public MLPPeer getPeerInfo() {
		return this.peerInfo;
	}

	public boolean isActive() {
		return PeerStatuses.Active.equals(PeerStatuses.forCode(this.peerInfo.getStatusCode()));
	}
	
	public boolean isKnown() {
		return this.peerInfo.getPeerId() != null;
	}
}