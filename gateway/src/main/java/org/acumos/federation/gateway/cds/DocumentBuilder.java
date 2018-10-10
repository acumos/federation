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
package org.acumos.federation.gateway.cds;

import java.util.Date;

/**
 */
public class DocumentBuilder {

	private Document document;

	protected DocumentBuilder(Document theDocument) {
		this.document = theDocument;
	}

	public Document build() {
		return this.document;
	} 

	public DocumentBuilder withCreatedDate(Date theDate) {
		this.document.setCreated(theDate);
		return this;
	}

	public DocumentBuilder withCreated(long theDate) {
		this.document.setCreated(new Date(theDate));
		return this;
	}

	public DocumentBuilder withModifiedDate(Date theDate) {
		this.document.setModified(theDate);
		return this;
	}

	public DocumentBuilder withModified(long theDate) {
		this.document.setModified(new Date(theDate));
		return this;
	}

	public DocumentBuilder withId(String theDocumentId) {
		this.document.setDocumentId(theDocumentId);
		return this;
	}

	public DocumentBuilder withUser(String theUserId) {
		this.document.setUserId(theUserId);
		return this;
	}

	public DocumentBuilder withVersion(String theVersion) {
		this.document.setVersion(theVersion);
		return this;
	}

	public DocumentBuilder withName(String theName) {
		this.document.setName(theName);
		return this;
	}

	public DocumentBuilder withUri(String theUri) {
		this.document.setUri(theUri);
		return this;
	}
	
	public DocumentBuilder withSize(Integer theSize) {
		this.document.setSize(theSize);
		return this;
	}

}

