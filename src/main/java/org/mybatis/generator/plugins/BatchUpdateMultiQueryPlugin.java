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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * 未完成。 批量更新插件，使用;分隔多个语句的方式,先不实现了，shardingsphere不支持
 * 2024年3月28日 上午11:07:37
 * @author SYQ
 */

public class BatchUpdateMultiQueryPlugin extends PluginAdapter {
	@Override
	public boolean validate(List<String> list) {
		return true;
	}

	/**
	 * 修改Mapper类
	 */
	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.isEmpty()) {
			return true;
		}
		addMethod(interfaze, introspectedTable);
		return true;
	}

	private void addMethod(Interface interfaze, IntrospectedTable introspectedTable) {
		// 设置需要import的类
		Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
		importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
		importedTypes.add(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));
		FullyQualifiedJavaType ibsreturnType = FullyQualifiedJavaType.getIntInstance();
		Method method = new Method("updateBatchMultiQuery");
		// 1.设置方法可见性
		method.setVisibility(JavaVisibility.PUBLIC);

		method.setAbstract(true);
		context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);

		// 2.设置返回值类型 int类型
		method.setReturnType(ibsreturnType);

		// 4.设置参数列表
		FullyQualifiedJavaType paramType = FullyQualifiedJavaType.getNewListInstance();
		FullyQualifiedJavaType paramListType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
		paramType.addTypeArgument(paramListType);
		method.addParameter(new Parameter(paramType, "records"));

		if (context.getPlugins().clientDeleteByPrimaryKeyMethodGenerated(method, interfaze, introspectedTable)) {
			interfaze.addImportedTypes(importedTypes);
			interfaze.addMethod(method);
		}
	}

	/**
	 * 修改Mapper.xml
	 */
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.isEmpty()) {
			return true;
		}
		addXml(document, introspectedTable);
		return true;
	}

	private void addXml(Document document, IntrospectedTable introspectedTable) {
		// <insert ...
		XmlElement element = new XmlElement("update");
		context.getCommentGenerator().addComment(element);

		element.addAttribute(new Attribute("id", "updateBatch"));
		element.addAttribute(new Attribute("parameterType", "java.util.List"));
		List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
		XmlElement foreachElement = new XmlElement("foreach");
		foreachElement.addAttribute(new Attribute("collection", "list"));
		foreachElement.addAttribute(new Attribute("index", "index"));
		foreachElement.addAttribute(new Attribute("item", "item"));
		foreachElement.addAttribute(new Attribute("open", "("));
		foreachElement.addAttribute(new Attribute("separator", ","));
		foreachElement.addAttribute(new Attribute("close", ")"));
		foreachElement.addElement(new TextElement(primaryKeyValue(introspectedTable)));
		element.addElement(new TextElement(
				"delete from " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime() + " where "));
		element.addElement(new TextElement(primaryKey(introspectedTable) + " in "));
		element.addElement(foreachElement);
		document.getRootElement().addElement(element);
	}

	private String primaryKey(IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.size() == 1) {
			return primaryKeyColumns.get(0).getActualColumnName();
		} else if (primaryKeyColumns.size() > 1) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			for (int i = 0; i < primaryKeyColumns.size(); i++) {
				sb.append(primaryKeyColumns.get(i).getActualColumnName());
				if (i != primaryKeyColumns.size() - 1) {
					sb.append(",");
				}
			}
			sb.append(")");
			return sb.toString();
		}
		return null;
	}

	private String primaryKeyValue(IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.size() == 1) {
			return "#{item." + primaryKeyColumns.get(0).getJavaProperty() + "}";
		} else if (primaryKeyColumns.size() > 1) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			for (int i = 0; i < primaryKeyColumns.size(); i++) {
				sb.append("#{item." + primaryKeyColumns.get(i).getJavaProperty() + "}");
				if (i != primaryKeyColumns.size() - 1) {
					sb.append(",");
				}
			}
			sb.append(")");
			return sb.toString();
		}
		return null;
	}

}
