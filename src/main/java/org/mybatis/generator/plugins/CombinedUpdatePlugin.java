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
