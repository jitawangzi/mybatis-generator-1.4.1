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
 *获取分页游标查询的 lastId参数
 * 2025年3月17日 09:29:56
 * @author SYQ
 */
public class GetLastIdPlugin extends PluginAdapter {

	private static final String METHOD_NAME = "getLastIdOfBatch";

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeys = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeys.isEmpty() || primaryKeys.size() > 1) {
			return true;
		}

		Method method = new Method(METHOD_NAME);
		method.setVisibility(JavaVisibility.PUBLIC);
		IntrospectedColumn primaryKeyColumn = primaryKeys.get(0);
		method.setReturnType(primaryKeyColumn.getFullyQualifiedJavaType());
		method.setAbstract(true);

		// 添加方法参数
		method.addParameter(new Parameter(primaryKeys.get(0).getFullyQualifiedJavaType(), "lastId", "@Param(\"lastId\")"));
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

	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeys = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeys.isEmpty() || primaryKeys.size() > 1) {
			return true;
		}
		IntrospectedColumn primaryKeyColumn = primaryKeys.get(0);
		// 生成查询语句
		XmlElement select = new XmlElement("select");
		context.getCommentGenerator().addComment(select);
		// 添加ID
		select.addAttribute(new Attribute("id", METHOD_NAME));
		// 添加返回类型
		String identityColumnType = primaryKeyColumn.getFullyQualifiedJavaType().getFullyQualifiedName();
		select.addAttribute(new Attribute("resultType", identityColumnType));

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
		select.addElement(new TextElement(
				"SELECT " + primaryKeyColumn.getActualColumnName() + " FROM " + introspectedTable.getFullyQualifiedTableNameAtRuntime()));
		select.addElement(whereElement);
		select.addElement(new TextElement(orderByClause.toString()));
		select.addElement(new TextElement("LIMIT ${limit - 1}, 1"));

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
			for (int i = 0; i < primaryKeys.size(); i++) {
				IntrospectedColumn column = primaryKeys.get(i);
				String paramName = "last" + capitalize(column.getJavaProperty());
				sb.append("#{").append(paramName).append("}");
				if (i < primaryKeys.size() - 1) {
					sb.append(", ");
				}
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
