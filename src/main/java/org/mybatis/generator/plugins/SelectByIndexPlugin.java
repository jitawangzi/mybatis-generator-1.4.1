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

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.mybatis.generator.api.FullyQualifiedTable;
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
import org.mybatis.generator.api.dom.xml.VisitableElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

/**
 * 根据表的索引，生成select方法
 */
public class SelectByIndexPlugin extends PluginAdapter {

	public SelectByIndexPlugin() {
		super();
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	// 初始化，查询并设置表的索引数据到IntrospectedTable
	@Override
	public void initialized(IntrospectedTable introspectedTable) {
		try {
			Connection connection = context.getConnection();
			DatabaseMetaData metaData = connection.getMetaData();
			Map<String, List<IntrospectedColumn>> indexMap = new HashMap<>(5);
			FullyQualifiedTable fullyQualifiedTable = introspectedTable.getFullyQualifiedTable();
			ResultSet rs = metaData.getIndexInfo(fullyQualifiedTable.getIntrospectedCatalog(), fullyQualifiedTable
					.getIntrospectedSchema(), fullyQualifiedTable.getIntrospectedTableName(), false, false);
			while (rs.next()) {
				// @see https://docs.oracle.com/javase/6/docs/api/java/sql/DatabaseMetaData.html#getColumns(java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String)
				String indexName = rs.getString("INDEX_NAME");
				if ("PRIMARY".equals(indexName)) {
					continue;
				}
				Optional<IntrospectedColumn> columnOptional = introspectedTable.getColumn(rs.getString("COLUMN_NAME"));
				if (!columnOptional.isPresent()) {
					continue;
				}
				if (indexMap.containsKey(indexName)) {
					indexMap.get(indexName).add(columnOptional.get());
				} else {
					List<IntrospectedColumn> cols = new ArrayList<>();
					cols.add(columnOptional.get());
					indexMap.put(indexName, cols);
				}
			}
			introspectedTable.setAttribute("indexs", indexMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Mapper 接口中生成方法
	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		@SuppressWarnings("unchecked")
		Map<String, List<IntrospectedColumn>> indexMap = (Map<String, List<IntrospectedColumn>>) introspectedTable.getAttribute(
				"indexs");
		for (Entry<String, List<IntrospectedColumn>> entry : indexMap.entrySet()) {
			String indexName = entry.getKey();
			String methodName = getMethodName(indexName);
			// 方法生成
			Method method = new Method(methodName);
			method.setVisibility(JavaVisibility.PUBLIC);
			String returnType = "List<" + introspectedTable.getFullyQualifiedTable().getDomainObjectName() + ">";
			FullyQualifiedJavaType fqjt = new FullyQualifiedJavaType(returnType);
			method.setReturnType(fqjt);
			method.setAbstract(true);
			List<IntrospectedColumn> columns = entry.getValue();
			List<Parameter> parameters = columns.stream().map(it -> new Parameter(it.getFullyQualifiedJavaType(), it.getJavaProperty(),
					"@Param(\"" + it.getJavaProperty() + "\")")).collect(Collectors.toList());
			for (Parameter para : parameters) {
				method.addParameter(para);
			}
			// 注释生成
			context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);
			interfaze.addMethod(method);
//			interfaze.addStaticImport("org.apache.ibatis.annotations.Param");
			Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
			importedTypes.add(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Param")); //$NON-NLS-1$
			importedTypes.add(new FullyQualifiedJavaType("java.util.List")); //$NON-NLS-1$
			interfaze.addImportedTypes(importedTypes);

		}
		return super.clientGenerated(interfaze, introspectedTable);
	}
	// mapper.xml中生成select方法
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {

		@SuppressWarnings("unchecked")
		Map<String, List<IntrospectedColumn>> indexMap = (Map<String, List<IntrospectedColumn>>) introspectedTable.getAttribute(
				"indexs");
		for (Entry<String, List<IntrospectedColumn>> entry : indexMap.entrySet()) {
			String indexName = entry.getKey();
			String methodName = getMethodName(indexName);
			List<IntrospectedColumn> columns = entry.getValue();

			// 生成查询语句
			XmlElement select = new XmlElement("select");
			context.getCommentGenerator().addComment(select);
			// 添加ID
			select.addAttribute(new Attribute("id", methodName));
			// 添加返回类型
			if (introspectedTable.hasBLOBColumns()) {
				select.addAttribute(new Attribute("resultMap", //$NON-NLS-1$
						introspectedTable.getResultMapWithBLOBsId()));
			} else {
				select.addAttribute(new Attribute("resultMap", //$NON-NLS-1$
						introspectedTable.getBaseResultMapId()));
			}

			// 添加参数类型
			String parameterType;
			if (columns.size() == 1) {
				parameterType = columns.get(0).getFullyQualifiedJavaType().getFullyQualifiedName();
			} else {
				// PK fields are in the base class. If more than on PK
				// field, then they are coming in a map.
				parameterType = "map"; //$NON-NLS-1$
			}
			select.addAttribute(new Attribute("parameterType", parameterType));
			select.addElement(new TextElement("select"));

			StringBuilder sb = new StringBuilder();
			if (stringHasValue(introspectedTable.getSelectByExampleQueryId())) {
				sb.append('\'');
				sb.append(introspectedTable.getSelectByExampleQueryId());
				sb.append("' as QUERYID,");
			}
			select.addElement(new TextElement(sb.toString()));

			XmlElement base = new XmlElement("include"); //$NON-NLS-1$
			base.addAttribute(new Attribute("refid", //$NON-NLS-1$
					introspectedTable.getBaseColumnListId()));

			select.addElement(base);

			if (introspectedTable.hasBLOBColumns()) {
				select.addElement(new TextElement(",")); //$NON-NLS-1$

				XmlElement blob = new XmlElement("include"); //$NON-NLS-1$
				blob.addAttribute(new Attribute("refid", //$NON-NLS-1$
						introspectedTable.getBlobColumnListId()));

				select.addElement(blob);
			}
			// 添加from tableName
			sb.setLength(0);
			sb.append("from ");
			sb.append(introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime());
			select.addElement(new TextElement(sb.toString()));
			// 添加where语句
			for (VisitableElement where : generateWheres(columns)) {
				select.addElement(where);
			}

			addElementWithBestPosition(document.getRootElement(), select);
		}
		return true;
	}

	private String getMethodName(String indexName) {
		return "selectBy" + makeIndex(indexName);
	}
	private String makeIndex(String name) {
		String indexName = name.replace("idx_", "");
		return upperIndex(indexName);
	}
	private String upperIndex(String name) {
		String[] split = name.split("_");
		String ret = "";
		for (int i = 0; i < split.length; i++) {
			char[] arr = split[i].toCharArray();
			arr[0] = Character.toUpperCase(arr[0]);
			ret += new String(arr);
		}
		return ret.length() == 0 ? name : ret;
	}

	private List<VisitableElement> generateWheres(List<IntrospectedColumn> uniqueKey) {
		List<VisitableElement> answer = new ArrayList<>();
		// 添加where语句
		boolean and = false;
		StringBuilder sb = new StringBuilder();
		for (IntrospectedColumn introspectedColumn : uniqueKey) {
			sb.setLength(0);
			if (and) {
				sb.append("  and "); //$NON-NLS-1$
			} else {
				sb.append("where "); //$NON-NLS-1$
				and = true;
			}

			sb.append(MyBatis3FormattingUtilities.getAliasedEscapedColumnName(introspectedColumn));
			sb.append(" = "); //$NON-NLS-1$
			sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
			answer.add(new TextElement(sb.toString()));
		}

		return answer;
	}

	/**
	 * 在最佳位置添加节点
	 *
	 * @param rootElement
	 * @param element
	 */
	public void addElementWithBestPosition(XmlElement rootElement, XmlElement element) {
		// sql 元素都放在sql后面
		if (element.getName().equals("sql")) {
			int index = 0;
			for (VisitableElement ele : rootElement.getElements()) {
				if (ele instanceof XmlElement && ((XmlElement) ele).getName().equals("sql")) {
					index++;
				}
			}
			rootElement.addElement(index, element);
		} else {
			// 根据id 排序
			String id = getIdFromElement(element);
			if (id == null) {
				rootElement.addElement(element);
			} else {
				List<VisitableElement> elements = rootElement.getElements();
				int index = -1;
				for (int i = 0; i < elements.size(); i++) {
					VisitableElement ele = elements.get(i);
					if (ele instanceof XmlElement) {
						String eleId = getIdFromElement((XmlElement) ele);
						if (eleId != null) {
							if (eleId.startsWith(id)) {
								if (index == -1) {
									index = i;
								}
							} else if (id.startsWith(eleId)) {
								index = i + 1;
							}
						}
					}
				}

				if (index == -1 || index >= elements.size()) {
					rootElement.addElement(element);
				} else {
					elements.add(index, element);
				}
			}
		}
	}
	/**
	 * 找出节点ID值
	 *
	 * @param element
	 * @return
	 */
	private String getIdFromElement(XmlElement element) {
		for (Attribute attribute : element.getAttributes()) {
			if (attribute.getName().equals("id")) {
				return attribute.getValue();
			}
		}
		return null;
	}

}
