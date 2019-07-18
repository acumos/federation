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
package org.acumos.federation.gateway.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.config.FederationInterfaceConfiguration;
import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.acumos.federation.gateway.config.NexusConfiguration;
import org.acumos.federation.gateway.service.ContentService;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = org.acumos.federation.gateway.Application.class,
								webEnvironment = WebEnvironment.RANDOM_PORT,
								properties = {
									"spring.main.allow-bean-definition-overriding=true",
									"federation.instance=gateway",
									"federation.instance.name=test",
									"federation.operator=admin",
									"federation.ssl.key-store=classpath:acumosa.pkcs12",
									"federation.ssl.key-store-password=acumosa",
									"federation.ssl.key-store-type=PKCS12",
									"federation.ssl.key-password = acumosa",
									"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
									"federation.ssl.trust-store-password=acumos",
									"federation.ssl.client-auth=need",
									//fake cds info as teh underlying http client is mocked 
									"cdms.client.url=http://localhost:8000/ccds",
									"cdms.client.username=username",
									"cdms.client.password=password"
								})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PeerGatewayTest {
	private final Logger log = LoggerFactory.getLogger(getClass().getName());

	@MockBean //(name = "local-org.acumos.federation.gateway.config.LocalInterfaceConfiguration")
	private LocalInterfaceConfiguration	localConfig;

	@Mock
	private CloseableHttpClient	localClient;

	@MockBean //(name = "federation-org.acumos.federation.gateway.config.FederationInterfaceConfiguration")
	private FederationInterfaceConfiguration federationConfig;

	@Mock
	private CloseableHttpClient	federationClient;

	@Mock
	private RestTemplate nexusClient;

	@MockBean
	private NexusConfiguration nexusConfig;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ContentService content;

	private MockAnswer peerAnswer = new MockAnswer();	
	private MockAnswer cdsAnswer = new MockAnswer();	
	//initialize with the number of checkpoints
	private CountDownLatch stepLatch = new CountDownLatch(10);

	@Before
	public void initTest() throws IOException {
		MockitoAnnotations.initMocks(this);

		final Consumer<MockResponse> stepTrack =  (r)->stepLatch.countDown();

		//the mocking setup below must be in place before the gateway starts, not part of the test 'per se'.
		cdsAnswer
				.mockResponse(info -> info.getPath().equals("/ccds/peer/search") && info.getQueryParam("self").equals("true"), MockResponse.success("mockCDSPeerSearchSelfResponse.json"))
				.mockResponse(info -> info.getPath().equals("/ccds/catalog/solution"), MockResponse.success("mockCDSPortalNoSolutionsResponse.json"))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/catalog"), MockResponse.success("mockCDSCreateCatalogResponse.json", stepTrack))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/catalog/myCatalog/solution/6793411f-c7a1-4e93-85bc-f91d267541d8"), MockResponse.success("mockCDSAddCatalogSolutionResponse.json", stepTrack))
				.mockResponse(info -> info.getPath().equals("/ccds/peer"), MockResponse.success("mockCDSPeerSearchAllResponse.json"))
				.mockResponse(info -> info.getPath().equals("/ccds/peer/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/sub"), MockResponse.success("mockCDSPeerSubscriptionsResponse.json"))
				.mockResponse(info -> info.getPath().equals("/ccds/peer/sub/1"), MockResponse.success("mockCDSPeerSubscriptionResponse.json")) //this works for GET and PUT ..
				.mockResponse(info -> info.getMethod().equals("GET") && info.getPath().equals("/ccds/solution/6793411f-c7a1-4e93-85bc-f91d267541d8"), MockResponse.success("mockCDSNoSuchThingResponse.json"))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/solution"), MockResponse.success("mockCDSCreateSolutionResponse.json", stepTrack))
				.mockResponse(info -> info.getMethod().equals("PUT") && info.getPath().equals("/ccds/solution/6793411f-c7a1-4e93-85bc-f91d267541d8/pic"), MockResponse.success("mockCDSsaveSolutionPicResponse.json", stepTrack))
				.mockResponse(info -> info.getMethod().equals("GET") && info.getPath().equals("/ccds/solution/6793411f-c7a1-4e93-85bc-f91d267541d8/revision"), MockResponse.success("mockCDSNoSuchSolutionRevisionsResponse.json"))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/solution/6793411f-c7a1-4e93-85bc-f91d267541d8/revision"), MockResponse.success("mockCDSCreateSolutionRevisionResponse.json", stepTrack))
				.mockResponse(info -> info.getMethod().equals("GET") && info.getPath().equals("/ccds/artifact/2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b341"), MockResponse.success("mockCDSNoSuchThingResponse.json"))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/artifact"), MockResponse.success("mockCDSCreateArtifactResponse.json", stepTrack))
				.mockResponse(info -> info.getMethod().equals("GET") && info.getPath().equals("/ccds/document/2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b342"), MockResponse.success("mockCDSNoSuchThingResponse.json"))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/document"), MockResponse.success("mockCDSCreateDocumentResponse.json", stepTrack))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/revision/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/catalog/myCatalog/descr"), MockResponse.success("mockCDSCreateRevisionDescriptionResponse.json", stepTrack))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/revision/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/catalog/myCatalog/document/2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b342"), MockResponse.success("mockCDSCreateRevisionDocumentResponse.json", stepTrack))
				.mockResponse(info -> info.getMethod().equals("POST") && info.getPath().equals("/ccds/revision/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/artifact/2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b341"), MockResponse.success("mockCDSCreateRevisionArtifactResponse.json", stepTrack))
				.mockResponse(info -> info.getPath().equals("/ccds/code/pair/PEER_STATUS"), MockResponse.success("mockCDSPeerStatusResponse.json"))
				.mockResponse(info -> info.getPath().equals("/ccds/code/pair/ARTIFACT_TYPE"), MockResponse.success("mockCDSArtifactTypeResponse.json"))
				.mockResponse(info -> info.getPath().equals("/ccds/access/peer/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/catalog"), MockResponse.success("mockCDSNoSuchSolutionRevisionsResponse.json"))
				.mockResponse(info -> info.getPath().equals("/ccds/catalog/5ebbc521-1642-4d4c-a732-d9e8a6b51f4a/solution/count"), MockResponse.success("mockCDSPortalSolutionCountResponse.json"))
				.mockResponse(info -> info.getPath().equals("/ccds/catalog/e072d118-0875-438b-8c9e-cf1f8ef3d9cb/solution/count"), MockResponse.success("mockCDSPortalSolutionCountResponse.json"))
				.mockResponse(info -> info.getPath().equals("/ccds/catalog/search"), MockResponse.success("mockCDSPortalCatalogsResponse.json"));

		//the CDS client is built to use a RestTemplate that leverages the HttpClient built from the LocalInterfaceConfiguration
		//similar to what is done in ServiceTest
		{
			when(
				this.localClient.execute(
					any(HttpUriRequest.class), any(HttpContext.class)
				)
			).thenAnswer(this.cdsAnswer);

			when(
				this.localConfig.buildClient()
			)
			.thenAnswer(new Answer<HttpClient>() {
				public HttpClient answer(InvocationOnMock theInvocation) {
					return localClient;	
				}
			});
		}

	}

	/**
	 * The gateway behaviour is triggered by the availability of other solutions
	 * in a peer, as provided by the federation client.  
	 */
	@Test
	public void testGateway() {

		try {
			peerAnswer
					.mockResponse(info -> info.getPath().equals("/catalogs"), MockResponse.success("mockPeerGWCatalogsResponse.json"))
					.mockResponse(info -> info.getPath().equals("/solutions"), MockResponse.success("mockPeerSolutionsResponse.json"))
					.mockResponse(info -> info.getPath().startsWith("/solutions/") && !info.getPath().contains("/revisions"), MockResponse.success("mockPeerSolutionResponse.json"))
					.mockResponse(info -> info.getPath().endsWith("/revisions"), MockResponse.success("mockPeerSolutionRevisionsResponse.json"))
					.mockResponse(info -> info.getPath().endsWith("/artifacts"), MockResponse.success("mockPeerSolutionRevisionArtifactsResponse.json"))
					.mockResponse(info -> info.getPath().endsWith("/documents"), MockResponse.success("mockPeerSolutionRevisionDocumentsResponse.json"))
					.mockResponse(info -> info.getPath().endsWith("/download"), MockResponse.success("mockPeerDownload.tgz"))
					.mockResponse(info -> info.getPath().contains("/solutions/") && info.getPath().contains("/revisions/") && !info.getPath().endsWith("/content"), MockResponse.success("mockPeerSolutionRevisionResponse.json"))
					.mockResponse(info -> info.getPath().contains("/artifacts/") && info.getPath().endsWith("/content"), MockResponse.success("mockPeerArtifactContent.txt"))
					.mockResponse(info -> info.getPath().contains("/documents/") && info.getPath().endsWith("/content"), MockResponse.success("mockPeerDocumentContent.txt"));

			when(
				this.federationClient.execute(
					any(HttpUriRequest.class), any(HttpContext.class)
				)
			).thenAnswer(peerAnswer);

			when(
				this.federationConfig.buildClient()
			)
			.thenAnswer(new Answer<HttpClient>() {
				public HttpClient answer(InvocationOnMock theInvocation) {
					return federationClient;	
				}
			});

			when(
				this.federationConfig.getSubjectName()
			)
			.thenAnswer(new Answer<String>() {
				public String answer(InvocationOnMock theInvocation) {
					return "CN=gateway.acumosa.org";	
				}
			});

			when(
				this.nexusConfig.getNexusClient()
			)
			.thenReturn(nexusClient);

			when(
				this.nexusConfig.getGroupId()
			)
			.thenReturn("com.artifact");
			when(
				this.nexusConfig.getNameSeparator()
			)
			.thenReturn(".");
			when(
				this.nexusConfig.getUrl()
			)
			.thenReturn("http://somehost.example.org/");


			when(
				this.nexusClient.exchange(
					any(RequestEntity.class),any(Class.class)
				)
			)
			.thenReturn(new ResponseEntity<byte[]>(new byte[] {}, HttpStatus.OK));

		}
		catch(Exception x) {
			log.error("Failed to setup mock", x);
			fail();
		}

		//let the test wait for a few seconds so that the expected
		//behaviour kicks in
		boolean completed = false;
		try {
			completed = stepLatch.await(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException ix) {
			fail();
		}
		if (!completed) {
			log.error("Failed to complete {} steps left", stepLatch.getCount());
		}
		//if we are here is that all steps that we expected took place
		assertTrue(completed);
	}
}