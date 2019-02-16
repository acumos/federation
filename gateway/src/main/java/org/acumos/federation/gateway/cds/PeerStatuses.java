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
package org.acumos.federation.gateway.cds;


import org.acumos.cds.CodeNameType;

/**
 */
public class PeerStatuses extends CodeNames<PeerStatus> {

	public static PeerStatus Active = forCode("AC");
	public static PeerStatus Inactive = forCode("IN");
	public static PeerStatus Requested = forCode("RQ");
	public static PeerStatus Renounced = forCode("RN");
	public static PeerStatus Declined = forCode("DC");
	public static PeerStatus Unknown = forCode("UK");

	/* There is no purpose in creating more than one instance of this class so one might want to explicitly make it a singleton
	 */
	public PeerStatuses() {
	}

	public static PeerStatus forCode(String theCode) {
		return CodeNames.forCode(theCode, PeerStatus.class);
	}
}


