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

package org.acumos.federation.gateway.controller;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.ApiOperation;

/**
 * Provides an interface for the local Acumos components to query a peer's catalog.
 * Limited to solution level information. 
 *
 */
@Controller
@RequestMapping(API.Roots.LOCAL)
public class PeerCatalogController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Provides access 
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param thePeerId Peer ID
	 * @return List of Catalogs in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).PEER_ACCESS)")
	@ApiOperation(value = "Invoked by local Acumos to get a list of catalogs available from a peer Acumos instance .", response = MLPCatalog.class, responseContainer = "List")
	@RequestMapping(value = { API.Paths.CATALOGS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPCatalog>> getCatalogs(
	    HttpServletResponse theHttpResponse,
	    @PathVariable("peerId") String thePeerId) {
		log.debug(API.Roots.LOCAL + "" + API.Paths.CATALOGS);
		return callPeer("getCatalogs", theHttpResponse, thePeerId, peer -> peer.getCatalogs());
	}

	/**
	 * Provides access 
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param thePeerId Peer ID
	 * @param theSelector
	 *            Solutions selector
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).PEER_ACCESS)")
	@ApiOperation(value = "Invoked by local Acumos to get a list of solutions available from a peer Acumos instance .", response = MLPSolution.class, responseContainer = "List")
	@RequestMapping(value = { API.Paths.SOLUTIONS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(
	    HttpServletResponse theHttpResponse,
	    @PathVariable("peerId") String thePeerId,
	    @RequestParam(value = API.QueryParameters.SOLUTIONS_SELECTOR, required = false) String theSelector) {
		log.debug(API.Roots.LOCAL + "" + API.Paths.SOLUTIONS);
		return callPeer("getSolutions", theHttpResponse, thePeerId, peer -> peer.getSolutions(theSelector == null? null: Utils.jsonStringToMap(new String(Base64Utils.decodeFromString(theSelector), "UTF-8"))));
	}

	@CrossOrigin
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).PEER_ACCESS)")
	@ApiOperation(value = "Invoked by local Acumos to get detailed solution information from the catalog of a peer acumos Instance.", response = MLPSolution.class)
	@RequestMapping(value = { API.Paths.SOLUTION_DETAILS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolutionDetails(
	    HttpServletResponse theHttpResponse,
	    @PathVariable("peerId") String thePeerId,
	    @PathVariable(value = "solutionId") String theSolutionId) {
		log.debug(API.Roots.LOCAL + "" + API.Paths.SOLUTION_DETAILS);
		return callPeer("getSolution", theHttpResponse, thePeerId, peer -> peer.getSolution(theSolutionId));
	}
}
