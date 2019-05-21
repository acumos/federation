/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.client.config;

import lombok.Data;

/**
 * Configuration for a TLS (https) client.
 */

@Data
public class TlsConfig	{
	/**
	 * Key store file name for 2-way authentication.
	 * If null, client side certificates are disabled.
	 *
	 * @param keyStore The file name.
	 * @return The file name.
	 */
	private String keyStore;
	/**
	 * The type of the key store file.
	 * If null, client side certificates are disabled.
	 *
	 * @param keyStoreType The type.
	 * @return The type.
	 */
	private String keyStoreType = "JKS";
	/**
	 * The password for the key store file.
	 * If null, client side certificates are disabled.
	 *
	 * @param keyStorePassword The password.
	 * @return The password.
	 */
	private String keyStorePassword;
	/**
	 * The alias of the entry in the key store to use.
	 *
	 * @param keyAlias The alias name.
	 * @return The alias name.
	 */
	private String keyAlias;
	/**
	 * Trust store file name for validating server certificate.
	 * If null, the default trust store is used.
	 *
	 * @param trustStore The file name.
	 * @return The file name.
	 */
	private String trustStore;
	/**
	 * The type of the trust store file.
	 * If null, the default trust store is used.
	 *
	 * @param trustStoreType The type.
	 * @return The type.
	 */
	private String trustStoreType = "JKS";
	/**
	 * The password for the trust store file.
	 * If null, the default trust store is used.
	 *
	 * @param trustStorePassword The password.
	 * @return The password.
	 */
	private String trustStorePassword;
}
