/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2020 Nordix Foundation
 * ===================================================================================
 * This Acumos software file is distributed by Nordix Foundation
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
package org.acumos.federation.client.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.databind.JsonNode;


/**
 * Document enhanced with file name.
 */
@Data
@ToString(callSuper=true, includeFieldNames=true)
public class ModelData {
  /**
     * Example value
     * 
     * <pre>
     *  {@code
          "value": {
          "B": "121",
          "C": "270",
          "A": "601"
        }
     * }
     * </pre>
     * 
     *
     * @param value Open ended json object with key value pairs
     * @return Param values open ended
     */
  private JsonNode value;

  private String[] tags;

  @JsonProperty("@timestamp")
  private String timestamp;

  private ModelInfo model;

}
