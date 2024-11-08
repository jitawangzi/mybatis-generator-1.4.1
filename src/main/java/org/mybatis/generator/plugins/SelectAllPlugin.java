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

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 *查询出一个表的所有数据
 * 2024年11月8日 11:34:26
 * @author SYQ
 */
public class SelectAllPlugin extends PluginAdapter {

	private static final String METHOD_NAME = "selectAll";

	public SelectAllPlugin() {
		super();
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	// Mapper 接口中生成selectAll方法
	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		// 方法生成
		Method method = new Method(METHOD_NAME);
		method.setVisibility(JavaVisibility.PUBLIC);
		String returnType = "List<" + introspectedTable.getFullyQualifiedTable().getDomainObjectName() + ">";
		FullyQualifiedJavaType fqjt = new FullyQualifiedJavaType(returnType);
		method.setReturnType(fqjt);
		method.setAbstract(true);
		// 注释生成
		context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);
		interfaze.addMethod(method);
		Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
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

		select.addElement(new TextElement("SELECT * FROM " + introspectedTable.getFullyQualifiedTableNameAtRuntime()));
		document.getRootElement().addElement(select);
		return true;
	}


}
