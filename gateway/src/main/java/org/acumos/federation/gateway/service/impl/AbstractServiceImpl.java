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

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.security.Peer;
import org.acumos.federation.gateway.service.ServiceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** */
public abstract class AbstractServiceImpl {

	@Autowired
	protected Clients clients;
	@Autowired
	protected ApplicationContext appCtx;

	public ICommonDataServiceRestClient getClient() {
		return clients.getCDSClient();
	}

	public ServiceContext selfService() {
		return ServiceContext.forPeer((Peer)appCtx.getBean("self"));		
	}
}
