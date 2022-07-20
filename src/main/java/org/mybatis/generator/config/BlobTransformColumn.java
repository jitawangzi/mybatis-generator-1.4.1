/*
 *    Copyright ${license.git.copyrightYears} the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.generator.config;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;
import static org.mybatis.generator.internal.util.messages.Messages.getString;

import java.util.List;

public class BlobTransformColumn extends PropertyHolder {

	private String blobColumn;
	private String domainObjectFieldType;
	private String domainObjectFieldName;

	public void validate(List<String> errors, String tableName) {
		if (!stringHasValue(blobColumn)) {
			errors.add(getString("ValidationError.22", //$NON-NLS-1$
					tableName));
		}
	}

	public BlobTransformColumn(String blobColumn, String domainObjectFieldType, String domainObjectFieldName) {
		super();
		this.blobColumn = blobColumn;
		this.domainObjectFieldType = domainObjectFieldType;
		this.domainObjectFieldName = domainObjectFieldName;
	}

	public String getBlobColumn() {
		return blobColumn;
	}

	public void setBlobColumn(String blobColumn) {
		this.blobColumn = blobColumn;
	}

	public String getDomainObjectFieldType() {
		return domainObjectFieldType;
	}

	public void setDomainObjectFieldType(String domainObjectFieldType) {
		this.domainObjectFieldType = domainObjectFieldType;
	}

	public String getDomainObjectFieldName() {
		return domainObjectFieldName;
	}

	public void setDomainObjectFieldName(String domainObjectFieldName) {
		this.domainObjectFieldName = domainObjectFieldName;
	}

}
