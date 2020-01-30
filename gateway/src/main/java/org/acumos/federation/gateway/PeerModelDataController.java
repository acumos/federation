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
import org.acumos.cds.domain.MLPPeer;
import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.data.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;

/**
 * Provides an interface for the local Acumos peer to send model data out to the respective remote peer.
 * Limited to sharing model data information with remote peer.
 */
@Controller
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PeerModelDataController {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private Clients clients;

    @Autowired
    private PeerService peerService;

    /**
     * Forwards incoming log message to respective remote peer.
     *
     * @param theHttpResponse HttpServletResponse
     * @param payload         model data payload
     * @return success message in JSON format
     * TODO - what Priviledge access to provide
     */
    @PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).PEER_ACCESS)")
    @ApiOperation(value = "Invoked by local Acumos to post incoming model data to respective remote peer Acumos instance .", response = JsonNode.class)
    @PostMapping(FederationClient.PEER_MODEL_DATA)
    @ResponseBody
    public JsonResponse<JsonNode> peerModelData(
            HttpServletResponse theHttpResponse,
            @RequestBody JsonNode payload) {

        log.debug(FederationClient.PEER_MODEL_DATA);
        String solutionId = payload.get("solutionId").asText();
        String peerId = clients.getCDSClient().getSolution(solutionId).getSourceId();
        try {

            // check if thePeerId matches to the
            // Ignore request if for local peer i.e. peerId same as local peer
            //
            MLPPeer self = peerService.getSelf(peerId);
            if (self.getPeerId().equals(peerId)) {
                this.getSuccessResponse(theHttpResponse, payload, "ignore logging to self-peer");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JsonNode> entity = new HttpEntity<>(payload, headers);

            return callPeer(theHttpResponse, peerId, peer -> peer.modelData(entity));

        } catch (Exception ex) {
            // TODO
            // failed posting to remote peer - could be any reason - remote peer not available/network issue/or something else
            // shall we post it to local CDS ?? and a background service can keep checking for any failed posts to remote peer and
            // try at regular intervals to repost to respective remote peer

            // for now - return success message
            return this.getSuccessResponse(theHttpResponse, payload, "failed posting to remote peer " + peerId);
        }

    }

    private JsonResponse<JsonNode> getSuccessResponse(
            HttpServletResponse theHttpResponse,
            @RequestBody JsonNode payload,
            String message) {

        JsonResponse<JsonNode> response = new JsonResponse();

        response.setMessage("modelData - " + message);
        response.setContent(payload);
        theHttpResponse.setStatus(HttpServletResponse.SC_OK);
        return response;
    }

    private <T> JsonResponse<T> callPeer(HttpServletResponse response, String peerId, Function<FederationClient, T> fcn) {
        MLPPeer peer = peerService.getPeer(peerId);
        JsonResponse<T> ret = new JsonResponse();
        if (peer == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ret.setMessage(String.format("No peer with id %s found.", peerId));
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            ret.setContent(fcn.apply(clients.getFederationClient(peer.getApiUrl())));
        }
        return ret;
    }
}
