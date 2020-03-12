/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2020 Nordix Foundation
 * ===================================================================================
 * This Acumos software file is distributed by Nordix Foundation
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
package org.acumos.federation.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import org.acumos.federation.client.data.ModelData;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

/**
 * API for sending model data to logstash
 *
 * Provides a service that will send model data to logstash
 */
public interface DeployedModelService {
	/**
	 * Send the params to model
	 *
	 * @param ingressUrl Url of the DeployedModel
	 * @param params Model params payload
	 *
	 */
	public void updateModelParams(String ingressUrl, JsonNode params) throws RestClientException;

	public String updateParamsforAllDeployments(ModelData payload) throws IOException;
}
