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
 * offset实现的分页查询插件
 * 2024年11月8日 12:00:58
 * @author SYQ
 */
public class GetBatchOffsetPlugin extends PluginAdapter {

	private static final String METHOD_NAME = "getBatchOffset";

	public GetBatchOffsetPlugin() {
		super();
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		// 方法生成
//		List<PlayerData> getBatch(@Param("offset") int offset, @Param("limit") int limit);

		Method method = new Method(METHOD_NAME);
		method.setVisibility(JavaVisibility.PUBLIC);
		String returnType = "List<" + introspectedTable.getFullyQualifiedTable().getDomainObjectName() + ">";
		FullyQualifiedJavaType fqjt = new FullyQualifiedJavaType(returnType);
		method.setReturnType(fqjt);
		method.setAbstract(true);

		// 方法参数
		Parameter para1 = new Parameter(FullyQualifiedJavaType.getIntInstance(), "offset", "@Param(\"offset\")");
		Parameter para2 = new Parameter(FullyQualifiedJavaType.getIntInstance(), "limit", "@Param(\"limit\")");
		method.addParameter(para1);
		method.addParameter(para2);
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
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		String orderBy = " ORDER BY ";
		for (int i = 0; i < primaryKeyColumns.size(); i++) {
			IntrospectedColumn introspectedColumn = primaryKeyColumns.get(i);
			orderBy += introspectedColumn.getActualColumnName() + " ASC";
			if (i != primaryKeyColumns.size() - 1) {
				orderBy += ",";
			}
		}
		select.addElement(new TextElement(
				"SELECT * FROM " + introspectedTable.getFullyQualifiedTableNameAtRuntime() + orderBy + " LIMIT #{limit} OFFSET #{offset}"));
		document.getRootElement().addElement(select);
		return true;
	}


}
