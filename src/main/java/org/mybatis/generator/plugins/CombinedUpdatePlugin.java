/*
 *    Copyright 2006-2025 the original author or authors.
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

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.xml.Document;

/**
 * 用于组合自定义更新和动态更新功能。
 * 这个插件将同时使用CustomUpdatePlugin和DynamicUpdatePlugin
 */
public class CombinedUpdatePlugin extends PluginAdapter {

	private CustomUpdatePlugin customUpdatePlugin;
	private DynamicUpdatePlugin dynamicUpdatePlugin;

	@Override
	public boolean validate(List<String> warnings) {
		// 初始化两个子插件
		customUpdatePlugin = new CustomUpdatePlugin();
		customUpdatePlugin.setContext(context);
		customUpdatePlugin.setProperties(properties);

		dynamicUpdatePlugin = new DynamicUpdatePlugin();
		dynamicUpdatePlugin.setContext(context);
		dynamicUpdatePlugin.setProperties(properties);

		return customUpdatePlugin.validate(warnings) && dynamicUpdatePlugin.validate(warnings);
	}

	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		boolean result1 = customUpdatePlugin.sqlMapDocumentGenerated(document, introspectedTable);
		boolean result2 = dynamicUpdatePlugin.sqlMapDocumentGenerated(document, introspectedTable);
		return result1 && result2;
	}

	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		boolean result1 = customUpdatePlugin.clientGenerated(interfaze, introspectedTable);
		boolean result2 = dynamicUpdatePlugin.clientGenerated(interfaze, introspectedTable);
		return result1 && result2;
	}
}
