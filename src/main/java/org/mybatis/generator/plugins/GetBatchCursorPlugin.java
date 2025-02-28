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
 * 游标实现的分页查询插件
 * 2025年2月28日 16:48:24
 * @author SYQ
 */
public class GetBatchCursorPlugin extends PluginAdapter {

	private static final String METHOD_NAME = "getBatchCursor";

	public GetBatchCursorPlugin() {
		super();
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		// 方法生成

		Method method = new Method(METHOD_NAME);
		method.setVisibility(JavaVisibility.PUBLIC);
		String returnType = "List<" + introspectedTable.getFullyQualifiedTable().getDomainObjectName() + ">";
		FullyQualifiedJavaType fqjt = new FullyQualifiedJavaType(returnType);
		method.setReturnType(fqjt);
		method.setAbstract(true);

		// 添加方法参数
		List<IntrospectedColumn> primaryKeys = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeys.size() == 1) {
			method.addParameter(new Parameter(primaryKeys.get(0).getFullyQualifiedJavaType(), "lastId", "@Param(\"lastId\")"));
		} else {
			// 复合主键使用Map参数
			for (IntrospectedColumn column : primaryKeys) {
				String paramName = "last" + capitalize(column.getJavaProperty());
				method.addParameter(new Parameter(column.getFullyQualifiedJavaType(), paramName, "@Param(\"" + paramName + "\")"));
			}
		}
		method.addParameter(new Parameter(FullyQualifiedJavaType.getIntInstance(), "limit", "@Param(\"limit\")"));

		// 注释生成
		context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);
		interfaze.addMethod(method);
		Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
		importedTypes.add(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Param")); //$NON-NLS-1$
		importedTypes.add(new FullyQualifiedJavaType("java.util.List")); //$NON-NLS-1$
		interfaze.addImportedTypes(importedTypes);

		return super.clientGenerated(interfaze, introspectedTable);
	}

	// mapper.xml中生成selectAll方法
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeys = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeys.isEmpty()) {
			return false;
		}
		// 生成查询语句
		XmlElement select = new XmlElement("select");
		context.getCommentGenerator().addComment(select);
		// 添加ID
		select.addAttribute(new Attribute("id", METHOD_NAME));
		// 添加返回类型
		if (introspectedTable.hasBLOBColumns()) {
			select.addAttribute(new Attribute("resultMap", //$NON-NLS-1$
					introspectedTable.getResultMapWithBLOBsId()));
		} else {
			select.addAttribute(new Attribute("resultMap", //$NON-NLS-1$
					introspectedTable.getBaseResultMapId()));
		}

		// 添加参数类型
		if (introspectedTable.getPrimaryKeyColumns().size() > 1) {
			select.addAttribute(new Attribute("parameterType", "map"));
		}

		// 构建ORDER BY子句
		StringBuilder orderByClause = new StringBuilder(" ORDER BY ");
		for (int i = 0; i < primaryKeys.size(); i++) {
			IntrospectedColumn column = primaryKeys.get(i);
			orderByClause.append(column.getActualColumnName()).append(" ASC");
			if (i < primaryKeys.size() - 1) {
				orderByClause.append(", ");
			}
		}

		// 构建WHERE子句
		XmlElement whereElement = buildWhereClause(primaryKeys);

		// 构建SQL语句
		select.addElement(new TextElement("SELECT * FROM " + introspectedTable.getFullyQualifiedTableNameAtRuntime()));
		select.addElement(whereElement);
		select.addElement(new TextElement(orderByClause.toString()));
		select.addElement(new TextElement("LIMIT #{limit}"));

		document.getRootElement().addElement(select);
		return true;
	}

	private XmlElement buildWhereClause(List<IntrospectedColumn> primaryKeys) {
		XmlElement where = new XmlElement("where");
		StringBuilder sb = new StringBuilder();

		if (primaryKeys.size() == 1) {
			// 单主键处理
			IntrospectedColumn column = primaryKeys.get(0);
			sb.append(column.getActualColumnName()).append(" > #{lastId}");
		} else {
			// 复合主键处理（使用元组比较）
			sb.append("(");
			for (int i = 0; i < primaryKeys.size(); i++) {
				sb.append(primaryKeys.get(i).getActualColumnName());
				if (i < primaryKeys.size() - 1) {
					sb.append(", ");
				}
			}
			sb.append(") > (");
			for (IntrospectedColumn column : primaryKeys) {
				String paramName = "last" + capitalize(column.getJavaProperty());
				sb.append("#{").append(paramName).append("}");
			}
			sb.append(")");
		}

		where.addElement(new TextElement(sb.toString()));
		return where;
	}

	private String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

}
