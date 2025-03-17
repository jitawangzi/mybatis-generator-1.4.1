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
import java.util.stream.Collectors;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.VisitableElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * 逻辑删除插件，未完成，应该不需要了。
 * 2025年3月17日 15:34:03
 * @author SYQ
 */
public class LogicalDeletePlugin extends PluginAdapter {

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	private boolean hasLogicalDeleteColumn(IntrospectedTable introspectedTable) {
		return introspectedTable.getBaseColumns().stream().anyMatch(c -> "is_deleted".equalsIgnoreCase(c.getActualColumnName()));
	}

	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		if (!hasLogicalDeleteColumn(introspectedTable)) {
			return true;
		}
		// ====================== 修复 1：插入 SQL 片段到 <mapper> 内部 ======================
		XmlElement rootElement = document.getRootElement();

		// 添加 isDeletedNotRemoved
		XmlElement isDeletedNotRemoved = new XmlElement("sql");
		isDeletedNotRemoved.addAttribute(new Attribute("id", "isDeletedNotRemoved"));
		isDeletedNotRemoved.addElement(new TextElement("AND is_deleted = 0"));
		rootElement.addElement(0, isDeletedNotRemoved); // 插入到第一个位置

		// 添加 setIsDeleted
		XmlElement setIsDeleted = new XmlElement("sql");
		setIsDeleted.addAttribute(new Attribute("id", "setIsDeleted"));
		setIsDeleted.addElement(new TextElement("is_deleted = 1"));
		rootElement.addElement(1, setIsDeleted); // 插入到第二个位置

		// ====================== 修复 2：替换 DELETE 为 UPDATE ======================
		List<XmlElement> deleteElements = rootElement.getElements()
				.stream()
				.filter(e -> e instanceof XmlElement)
				.map(e -> (XmlElement) e)
				.filter(e -> "delete".equals(e.getName()))
				.collect(Collectors.toList());

		for (XmlElement deleteElement : deleteElements) {
			replaceDeleteWithUpdate(deleteElement);
		}

		// ====================== 修复 3：增强 SELECT 语句 ======================
		rootElement.getElements()
				.stream()
				.filter(e -> e instanceof XmlElement)
				.map(e -> (XmlElement) e)
				.filter(e -> "select".equals(e.getName()))
				.forEach(this::addIsDeletedCondition);

		// 处理自定义方法（如 getBatchOffset、selectAll 等）
		document.getRootElement()
				.getElements()
				.stream()
				.filter(e -> e instanceof XmlElement)
				.map(e -> (XmlElement) e)
				.filter(e -> "select".equals(e.getName()))
				.forEach(e -> {
					String id = e.getAttributes()
							.stream()
							.filter(a -> "id".equals(a.getName()))
							.findFirst()
							.map(Attribute::getValue)
							.orElse("");

					// 为特定方法添加 is_deleted = 0 条件
					if (id.matches("getBatchOffset|selectAll|selectByPlayerId|selectByPrimaryKey")) {
						addIsDeletedCondition(e);
					}
				});

		return true;
	}

	private void replaceDeleteWithUpdate(XmlElement deleteElement) {
		// 修改为 UPDATE 语句
		deleteElement.setName("update");

		// 修改操作 ID（例如 deleteByExample -> logicalDeleteByExample）
		deleteElement.getAttributes().stream().filter(a -> "id".equals(a.getName())).findFirst().ifPresent(a -> {
			Attribute newAttr = new Attribute(a.getName(), a.getValue().replace("deleteBy", "logicalDeleteBy"));
			deleteElement.getAttributes().remove(a);
			deleteElement.getAttributes().add(newAttr);
		});

		// 构建 SET 子句
		XmlElement setElement = new XmlElement("set");
		XmlElement includeSet = new XmlElement("include");
		includeSet.addAttribute(new Attribute("refid", "setIsDeleted"));
		setElement.addElement(includeSet);

		// 保留原有 WHERE 子句
		List<VisitableElement> whereClauses = deleteElement.getElements()
				.stream()
				.filter(e -> e instanceof XmlElement)
				.filter(e -> "where".equals(((XmlElement) e).getName()))
				.collect(Collectors.toList());

		// 清空并重新组装元素
		deleteElement.getElements().clear();
		deleteElement.addElement(setElement);
		deleteElement.getElements().addAll(whereClauses);
	}

	private void addIsDeletedCondition(XmlElement selectElement) {
		// 在 WHERE 条件中添加 AND is_deleted = 0
		selectElement.getElements()
				.stream()
				.filter(e -> e instanceof XmlElement)
				.map(e -> (XmlElement) e)
				.filter(e -> "where".equals(e.getName()))
				.findFirst()
				.ifPresent(whereElement -> {
					XmlElement include = new XmlElement("include");
					include.addAttribute(new Attribute("refid", "isDeletedNotRemoved"));
					whereElement.addElement(new TextElement("AND"));
					whereElement.addElement(include);
				});
	}

	@Override
	public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		if (!hasLogicalDeleteColumn(introspectedTable)) {
			return true;
		}

		// 插入 is_deleted 列
		XmlElement isDeletedIf = new XmlElement("if");
		isDeletedIf.addAttribute(new Attribute("test", "isDeleted != null"));
		isDeletedIf.addElement(new TextElement("is_deleted,"));
		element.addElement(isDeletedIf);

		// 插入 is_deleted 值
		XmlElement isDeletedValueIf = new XmlElement("if");
		isDeletedValueIf.addAttribute(new Attribute("test", "isDeleted != null"));
		isDeletedValueIf.addElement(new TextElement("#{isDeleted,jdbcType=TINYINT},"));

		element.getElements()
				.stream()
				.filter(e -> e instanceof XmlElement)
				.map(e -> (XmlElement) e)
				.filter(e -> "trim".equals(e.getName()))
				.filter(e -> e.getAttributes().stream().anyMatch(a -> "suffix".equals(a.getName()) && "VALUES".equals(a.getValue())))
				.findFirst()
				.ifPresent(trimElement -> trimElement.addElement(isDeletedValueIf));

		return true;
	}
}
