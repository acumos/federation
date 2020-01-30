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

package org.acumos.federation.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiOperation;
import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.data.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.acumos.federation.gateway.ServiceConfig;

import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;

/**
 * ModelDataController for sending logging data sent from
 * models - this controller has two purposes handling logs for 
 * - AI model parameter updates
 * - Model usage logging
 * 
 */
@Controller
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ModelDataController {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // NOTE: rest template with default settings
    // this is to call local logstash service
    private RestTemplate logStashHttpInput = new RestTemplate();

    @Autowired
	private ServiceConfig logstashConfig;

    /**
     * Receives model data payload from {@link PeerModelDataController}
     * 
     * Logstash must be configured with the http input plugin
     * to allow posting of the inbound payload. Example logstash configuration with
     * http input plugin:
     * 
     * <pre>{@code
        input
        http {
            port => 5043
        }
     * }</pre>
     * 
     * Then port 5043 could be used / exposed and must be set using the logstash configuration
     * 
     * <pre>
     *  logstash.url=http://[LOGSTASH-HOST]:5043
     * </pre>
     *
     * @param payload  model data payload 
     * The payload must have a model.solutionId 
     * Example payload sending 3 parameters A,B,C to peer id 
     * <pre>{@code
     *   {
            "@version": "1",
            "host": "filebeat-200211-233649-czzcs",
            "source": "/var/log/params/model_param_update.log",
            "prospector": {
            "type": "log"
            },
            "@timestamp": "2020-02-17T21:21:09.338Z",
            "tags": [
                "acumos-model-param-logs",
                "beats_input_raw_event"
            ],
            "offset": 1492397,
            "beat": {
                "hostname": "filebeat-200211-233649-czzcs",
                "version": "6.0.1",
                "name": "filebeat-200211-233649-czzcs"
            },
            "model": {
                "userId": "12345678-abcd-90ab-cdef-1234567890ab",
                "revisionId": "1c0a4ea4-e822-4fb3-bef1-11f92958c315",
                "solutionId": "149ea34c-44fc-4329-8189-52d3ae523a15",
                "subscriberId": "50045f9a-3662-4123-8007-f6246608e4ab",
            },
            "value": {
                "B": "121",
                "C": "270",
                "A": "601"
            }
        }
     * }</pre>
     * @param theHttpResponse HttpServletResponse
     * @return success message in JSON format
     * 
     */
    @CrossOrigin
    @Secured(Security.ROLE_PEER)
    @ApiOperation(value = "Invoked by Peer Acumos to post model data to elastic search service .", response = JsonNode.class)
    @PostMapping(FederationClient.MODEL_DATA)
    @ResponseBody
    public JsonResponse<JsonNode> modelData(
            @RequestBody JsonNode payload,
            HttpServletResponse theHttpResponse) {

        LOG.debug(FederationClient.MODEL_DATA);
        JsonResponse<JsonNode> response = null;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JsonNode> entity = new HttpEntity<JsonNode>(payload, headers);
        LOG.debug("Model parameters:" + payload);
        String logStashUrl = logstashConfig.getUrl();
        LOG.debug("logStashUrl {}", logStashUrl);
        if (logStashUrl != null) {
            try {
                ResponseEntity<String> respEntity =
                        this.logStashHttpInput.exchange(
                                logStashUrl, HttpMethod.POST, entity, String.class);
                if (respEntity != null) {
                    String output = respEntity.getBody();
                    LOG.debug("{}: {}", "modelData - posted to logstash", output);
                }
            } catch (RestClientResponseException ex) {
                LOG.error("Cannot post to Log Stash {}", ex);
                response.setMessage("Cannot post to Logstash");
                theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return response;
            }
        }
        response = new JsonResponse<JsonNode>();
        response.setMessage("modelData - posted to logstash");
        response.setContent(payload);
        theHttpResponse.setStatus(HttpServletResponse.SC_OK);
        return response;
    }

}