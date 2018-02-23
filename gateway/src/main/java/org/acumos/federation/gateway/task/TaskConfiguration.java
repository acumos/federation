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

package org.acumos.federation.gateway.task;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import org.apache.http.client.HttpClient;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;


import org.acumos.federation.gateway.config.EELFLoggerDelegate;

/**
 * Provides the beans used to setup the peer subscription tasks.
 */
@Configuration
@EnableAutoConfiguration
//@ConfigurationProperties(prefix = "task", ignoreInvalidFields = true)
public class TaskConfiguration /* implements ApplicationContextAware */ {

	//private EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());

	public TaskConfiguration() {
	}

	/**
	 */
	@Bean
	public PeerSubscriptionTaskScheduler peerSubscriptionTaskScheduler() {
		return new PeerSubscriptionTaskScheduler();
	}

	/**
	 */
	@Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public PeerSubscriptionTask peerSubscriptionTask() {
		return new PeerSubscriptionTask();
	}
}
