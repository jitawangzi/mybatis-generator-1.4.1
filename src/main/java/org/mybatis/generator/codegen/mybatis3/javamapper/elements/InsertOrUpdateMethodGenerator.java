/*
 *    Copyright 2006-2024 the original author or authors.
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
package org.mybatis.generator.codegen.mybatis3.javamapper.elements;

import org.mybatis.generator.api.dom.java.Interface;

public class InsertOrUpdateMethodGenerator extends InsertMethodGenerator {

    public InsertOrUpdateMethodGenerator(boolean isSimple) {
        super(isSimple);
    }

    @Override
    public void addInterfaceElements(Interface interfaze) {
		String defaultInsertName = introspectedTable.getInsertStatementId();
		introspectedTable.setInsertStatementId("insertOrUpdate");

		super.addInterfaceElements(interfaze);

		introspectedTable.setInsertStatementId(defaultInsertName);

    }
}
