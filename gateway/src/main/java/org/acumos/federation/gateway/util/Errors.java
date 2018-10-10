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

package org.acumos.federation.gateway.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

/** */
public class Errors {

	private Errors() {
	}

	private static Pattern cdsNotFoundPattern = null;
	static {
		try {
			cdsNotFoundPattern = Pattern.compile("No (.*) with ID (.*)");
		}
		catch (PatternSyntaxException psx) {
			throw new RuntimeException("Invalid error pattern", psx);
		}
	}


	/**
	 * CDS provides a 400 error with a particular error message
	 * 
	 * @param theError
	 *            Error to parse
	 * @return true or false
	 */
	public static boolean isCDSNotFound(HttpStatusCodeException theError) {

		if (theError.getStatusCode() == HttpStatus.BAD_REQUEST) {
			String msg = theError.getResponseBodyAsString();
			if (msg != null) {
				return cdsNotFoundPattern.matcher((String) Utils.jsonStringToMap(msg).getOrDefault("error", ""))
								.matches();
			}
		}
		return false;
	}

}