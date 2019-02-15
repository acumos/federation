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

import org.acumos.cds.CodeNameType;

/**
 * The set of codes representing artifact types.
 */
public class ArtifactTypes extends CodeNames<ArtifactType> {

	//these are the artifact type codes that we expect to find in all deployments.
  public static ArtifactType Blueprint = forCode("BP");
  public static ArtifactType Cdump = forCode("CD");
  public static ArtifactType DockerImage = forCode("DI");
  public static ArtifactType DataSource = forCode("DS");
	public static ArtifactType Metadata = forCode("MD");
//  ModelH2O(ArtifactTypeCode.MH.name()), //
//  ModelImage(ArtifactTypeCode.MI.name()), //
//  ModelR(ArtifactTypeCode.MR.name()), //
//  ModelScikit(ArtifactTypeCode.MS.name()), //
//  ModelTensorflow(ArtifactTypeCode.MT.name()), //
//  ToscaTemplate(ArtifactTypeCode.TE.name()), //
//  ToscaGenerator(ArtifactTypeCode.TG.name()), //
//  ToscaSchema(ArtifactTypeCode.TS.name()), //
//  ToscaTranslate(ArtifactTypeCode.TT.name()), //
//  ProtobufFile(ArtifactTypeCode.PJ.name());


	public CodeNameType getType() {
		return CodeNameType.ARTIFACT_TYPE;
	}

	public static ArtifactType forCode(String theCode) {
		return CodeNames.forCode(theCode, ArtifactType.class);
	}
}


