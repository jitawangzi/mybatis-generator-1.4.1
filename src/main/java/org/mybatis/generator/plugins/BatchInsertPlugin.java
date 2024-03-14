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
import org.mybatis.generator.api.dom.OutputUtilities;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

public class BatchInsertPlugin extends PluginAdapter {
	@Override
	public boolean validate(List<String> list) {
		return true;
	}

	/**
	 * 修改Mapper类
	 */
	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		addBatchInsertMethod(interfaze, introspectedTable);
		return true;
	}

	private void addBatchInsertMethod(Interface interfaze, IntrospectedTable introspectedTable) {
		// 设置需要import的类
		Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
		importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
		importedTypes.add(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));
		FullyQualifiedJavaType ibsreturnType = FullyQualifiedJavaType.getIntInstance();
		Method batchInsertMethod = new Method("insertBatch");
		// 1.设置方法可见性
		batchInsertMethod.setVisibility(JavaVisibility.PUBLIC);

		batchInsertMethod.setAbstract(true);
		context.getCommentGenerator().addGeneralMethodComment(batchInsertMethod, introspectedTable);

		// 2.设置返回值类型 int类型
		batchInsertMethod.setReturnType(ibsreturnType);
		// 3.设置方法名
//		batchInsertMethod.setName("batchInsert");
		// 4.设置参数列表
		FullyQualifiedJavaType paramType = FullyQualifiedJavaType.getNewListInstance();
		FullyQualifiedJavaType paramListType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
		paramType.addTypeArgument(paramListType);
		batchInsertMethod.addParameter(new Parameter(paramType, "records"));
		interfaze.addImportedTypes(importedTypes);
		interfaze.addMethod(batchInsertMethod);
	}

	/**
	 * 修改Mapper.xml
	 */
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		addBatchInsertXml(document, introspectedTable);
		return true;
	}

	private void addBatchInsertXml(Document document, IntrospectedTable introspectedTable) {
		XmlElement insertBatchElement = new XmlElement("insert");
		context.getCommentGenerator().addComment(insertBatchElement);

		insertBatchElement.addAttribute(new Attribute("id", "insertBatch"));
		insertBatchElement.addAttribute(new Attribute("parameterType", "java.util.List"));
		XmlElement valueTrimElement = new XmlElement("trim");
		valueTrimElement.addAttribute(new Attribute("prefix", " ("));
		valueTrimElement.addAttribute(new Attribute("suffix", ")"));
		valueTrimElement.addAttribute(new Attribute("suffixOverrides", ","));
		XmlElement columnTrimElement = new XmlElement("trim");
		columnTrimElement.addAttribute(new Attribute("prefix", "("));
		columnTrimElement.addAttribute(new Attribute("suffix", ")"));
		columnTrimElement.addAttribute(new Attribute("suffixOverrides", ","));
		List<IntrospectedColumn> columns = introspectedTable.getAllColumns();

		StringBuilder columnClause = new StringBuilder();
		StringBuilder valuesClause = new StringBuilder();
		boolean isEmpty = false;
		for (int i = 0; i < columns.size(); i++) {
			IntrospectedColumn introspectedColumn = columns.get(i);
			columnClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
			valuesClause.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
			if (i + 1 < columns.size()) {
				columnClause.append(", "); //$NON-NLS-1$
				valuesClause.append(", "); //$NON-NLS-1$
			}
			isEmpty = false;
			// 太长了换行
			if (valuesClause.length() > 80) {
				columnTrimElement.addElement(new TextElement(columnClause.toString()));
				columnClause.setLength(0);
				OutputUtilities.xmlIndent(columnClause, 1);

				valueTrimElement.addElement(new TextElement(valuesClause.toString()));
				valuesClause.setLength(0);
				OutputUtilities.xmlIndent(valuesClause, 1);
				isEmpty = true;
			}
		}
		if (!isEmpty) {
			columnTrimElement.addElement(new TextElement(columnClause.toString()));
			valueTrimElement.addElement(new TextElement(valuesClause.toString()));
		}
		XmlElement foreachElement = new XmlElement("foreach");
		foreachElement.addAttribute(new Attribute("collection", "list"));
		foreachElement.addAttribute(new Attribute("index", "index"));
		foreachElement.addAttribute(new Attribute("item", "item"));
		foreachElement.addAttribute(new Attribute("separator", ","));
		insertBatchElement.addElement(
				new TextElement("insert into " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));
		insertBatchElement.addElement(columnTrimElement);
		insertBatchElement.addElement(new TextElement(" values "));
		foreachElement.addElement(valueTrimElement);
		insertBatchElement.addElement(foreachElement);
		document.getRootElement().addElement(insertBatchElement);
	}
}
