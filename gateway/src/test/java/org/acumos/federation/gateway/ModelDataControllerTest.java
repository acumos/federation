// /*-
//  * ===============LICENSE_START=======================================================
//  * Acumos
//  * ===================================================================================
//  * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
//  * ===================================================================================
//  * This Acumos software file is distributed by AT&T and Tech Mahindra
//  * under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * This file is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  * ===============LICENSE_END=========================================================
//  */
// package org.acumos.federation.gateway;

// import org.acumos.cds.client.CommonDataServiceRestClientImpl;
// import org.acumos.cds.client.ICommonDataServiceRestClient;
// import org.acumos.federation.client.ClientBase;
// import org.acumos.federation.client.FederationClient;
// import org.acumos.federation.client.GatewayClient;
// import org.acumos.federation.client.config.ClientConfig;
// import org.acumos.federation.client.test.ClientMocking;
// import org.acumos.securityverification.service.ISecurityVerificationClientService;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.boot.web.server.LocalServerPort;
// import org.springframework.core.ParameterizedTypeReference;
// import org.springframework.http.HttpMethod;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit4.SpringRunner;
// import org.springframework.web.client.HttpClientErrorException.Forbidden;
// import org.springframework.web.client.HttpClientErrorException.NotFound;

// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.TimeUnit;
// import java.util.function.Consumer;

// import static org.acumos.federation.client.test.ClientMocking.getConfig;
// import static org.acumos.federation.client.test.ClientMocking.xq;
// import static org.junit.Assert.*;
// import static org.mockito.Mockito.*;

// @RunWith(SpringRunner.class)
// @ContextConfiguration(classes = GatewayServer.class)
// @SpringBootTest(
//         classes = Application.class,
//         webEnvironment = WebEnvironment.RANDOM_PORT,
//         properties = {
//                 "spring.main.allow-bean-definition-overriding=true",
//                 "local.ssl.key-store=classpath:acumosa.pkcs12",
//                 "local.ssl.key-store-password=acumosa",
//                 "local.ssl.key-store-type=PKCS12",
//                 "local.ssl.trust-store=classpath:acumosTrustStore.jks",
//                 "local.ssl.trust-store-password=acumos",
//                 "nexus.group-id=nxsgrpid",
//                 "nexus.name-separator=,",
//                 "docker.registry-url=someregistry:9999",
//                 "federation.operator=defuserid"
//         }
// )
// public class ModelTrackingControllerTest {
//     @LocalServerPort
//     private int port;

//     @Autowired
//     private ServerConfig local;

//     @MockBean
//     private Clients clients;

//     private CountDownLatch steps;

//     private final Consumer<ClientMocking.RequestInfo> count = x -> this.steps.countDown();

//     private SimulatedDockerClient docker;

//     static ClientConfig anonConfig() {
//         ClientConfig ret = getConfig("bogus");
//         ret.getSsl().setKeyStore(null);
//         ret.setCreds(null);
//         return ret;
//     }

//     private static class RawAnonClient extends ClientBase {
//         public RawAnonClient(String url) throws Exception {
//             super(url, anonConfig(), null, null);
//         }

//         public byte[] get(String uri) {
//             return handle(uri, HttpMethod.GET, new ParameterizedTypeReference<byte[]>() {
//             });
//         }
//     }

//     @Before
//     public void init() throws Exception {
//         ICommonDataServiceRestClient cdsClient = CommonDataServiceRestClientImpl.getInstance("http://cds:999", ClientBase.buildRestTemplate("http://cds:999", new ClientConfig(), null, null));

//         (new ClientMocking())
//                 .on("POST /catalog", "{}", count)
//                 .applyTo(cdsClient);
//         when(clients.getCDSClient()).thenReturn(cdsClient);
//     }

//     @Test
//     public void testConfig() throws Exception {
//         assertEquals("acumosa", local.getSsl().getKeyStorePassword());

//         GatewayClient self \= new GatewayClient("https://localhost:" + port, getConfig("acumosa"));
//         GatewayClient known = new GatewayClient("https://localhost:" + port, getConfig("acumosb"));
//         GatewayClient unknown = new GatewayClient("https://localhost:" + port, getConfig("acumosc"));
//         assertNotNull(self.ping("somepeer"));
//         assertNotNull(self.register("somepeer"));
//         assertNotNull(self.getPeers("somepeer"));
//         try {
//             known.ping("somepeer");
//             fail();
//         } catch (Forbidden ux) {
//             // expected case
//         }
//         try {
//             unknown.ping("somepeer");
//             fail();
//         } catch (Forbidden ux) {
//             // expected case
//         }
//         try {
//             self.ping("unknownpeer");
//             fail();
//         } catch (NotFound nf) {
//             // expected case
//         }
//         assertEquals(2, self.getCatalogs("somepeer").size());
//         assertEquals(1, self.getSolutions("somepeer", "somecatalog").size());
//         assertNotNull(self.getSolution("somepeer", "somesolution"));
//         try {
//             self.triggerPeerSubscription("unknownpeer", 999);
//             fail();
//         } catch (NotFound nf) {
//             // expected case
//         }
//         try {
//             self.triggerPeerSubscription("somepeer", 998);
//             fail();
//         } catch (NotFound nf) {
//             // expected case
//         }
//         try {
//             self.triggerPeerSubscription("somepeer", 997);
//             fail();
//         } catch (NotFound nf) {
//             // expected case
//         }
//         docker.clearImages();
//         docker.addImage("imageid1", "tagA:1", "tagB:2");
//         docker.addImage("imageid2", "tagX:1", "thisimage:thistag");
//         steps = new CountDownLatch(12 + 1);
//         self.triggerPeerSubscription("somepeer", 992);
//         self.triggerPeerSubscription("somepeer", 993);
//         self.triggerPeerSubscription("somepeer", 994);
//         self.triggerPeerSubscription("somepeer", 995);
//         self.triggerPeerSubscription("somepeer", 996);
//         self.triggerPeerSubscription("somepeer", 999);
//         steps.await(2, TimeUnit.SECONDS);
//         assertEquals("Incomplete steps remain", 0, steps.getCount() - 1);
//     }


//     @Test
//     public void testSwagger() throws Exception {
//         RawAnonClient rac = new RawAnonClient("https://localhost:" + port);
//         assertNotNull(rac);
//         rac.get("/swagger-ui.html");
//         rac.get("/v2/api-docs");
//     }
// }
