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
package org.mybatis.generator.plugins;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.config.PropertyRegistry;

/**
 * 当配置为rootClass，也就是父类时，字段设置为protected修饰符，不用private，方便子类直接访问
 */
public class RootClassProtectedFieldPlugin extends PluginAdapter {

	private Set<String> rootClassSet = new HashSet<String>();

	public RootClassProtectedFieldPlugin() {
		super();
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public void initialized(IntrospectedTable introspectedTable) {
		List<IntrospectedTable> introspectedTables = context.getIntrospectedTables();
		for (IntrospectedTable introspectedTable2 : introspectedTables) {
			String rootClass = introspectedTable2.getTableConfigurationProperty(PropertyRegistry.ANY_ROOT_CLASS);
			if (rootClass != null) {
				rootClassSet.add(rootClass);
			}
		}

	}

	@Override
	public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		if (rootClassSet.contains(topLevelClass.getType().getFullyQualifiedName())) {
			field.setVisibility(JavaVisibility.PROTECTED);
		}
		return true;
	}



}
