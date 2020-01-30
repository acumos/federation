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
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;

/**
 *
 */
@Controller
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ModelDataController {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // NOTE: rest template with default settings
    // this is to call local logstash service
    // TODO: check with Chris for any security requirements
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Receives model data payload and posts it to logstash service.
     *
     * @param payload         model data payload
     * @param theHttpResponse HttpServletResponse
     * @return success message in JSON format
     * <p>
     * TODO - what Priviledge access to provide
     */
    @CrossOrigin
    @PreAuthorize("isActive() && hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).CATALOG_ACCESS)")
    @ApiOperation(value = "Invoked by Peer Acumos to post model data to elastic search service .", response = JsonNode.class)
    @PostMapping(FederationClient.MODEL_DATA)
    @ResponseBody
    public JsonResponse<JsonNode> modelData(
            @RequestBody JsonNode payload,
            HttpServletResponse theHttpResponse) {

        log.debug(FederationClient.MODEL_DATA);
        JsonResponse<JsonNode> response = null;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JsonNode> entity = new HttpEntity<JsonNode>(payload, headers);

        log.info("Model parameters:" + payload);

        String logStashUrl = System.getenv("logstash");
        if (logStashUrl != null) {

            try {
                ResponseEntity<String> respEntity =
                        this.restTemplate.exchange(
                                logStashUrl, HttpMethod.POST, entity, String.class);

                if (respEntity != null) {
                    String output = respEntity.getBody();
                    log.debug("{}: {}", "modelData - posted to logstash", output);
                }

            } catch (Exception ex) {
                // What should we do for failure
                log.error("Cannot post to Log Stash");
            }
        }

        response = new JsonResponse<JsonNode>();
        response.setMessage("modelData - posted to logstash");
        response.setContent(payload);
        theHttpResponse.setStatus(HttpServletResponse.SC_OK);
        return response;
    }

}