package org.mybatis.generator.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

public class InsertOrUpdatePlugin extends PluginAdapter {

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	/**
	 * 1. 生成 Java 接口方法
	 * 保持 abstract = true，确保生成 interface 方法时不带大括号
	 */
	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		Method method = new Method("insertOrUpdate");
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setReturnType(FullyQualifiedJavaType.getIntInstance());
		method.setAbstract(true); // 关键：生成抽象方法

		FullyQualifiedJavaType parameterType = introspectedTable.getRules().calculateAllFieldsClass();
		method.addParameter(new Parameter(parameterType, "record"));

		context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);
		interfaze.addMethod(method);
		return true;
	}

	/**
	 * 2. 生成 XML SQL
	 * 修复策略：
	 * - 使用列名(ColumnName)对比来排除主键，避免对象引用问题。
	 * - 分段生成 TextElement，确保逻辑清晰，不丢失片段。
	 */
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		XmlElement answer = new XmlElement("insert");
		answer.addAttribute(new Attribute("id", "insertOrUpdate"));

		FullyQualifiedJavaType parameterType = introspectedTable.getRules().calculateAllFieldsClass();
		answer.addAttribute(new Attribute("parameterType", parameterType.getFullyQualifiedName()));

		context.getCommentGenerator().addComment(answer);

		List<IntrospectedColumn> allColumns = introspectedTable.getAllColumns();
		List<IntrospectedColumn> pkColumns = introspectedTable.getPrimaryKeyColumns();

		// --- 第一部分：insert into 表名 (列...) ---
		StringBuilder insertClause = new StringBuilder();
		insertClause.append("insert into ");
		insertClause.append(introspectedTable.getFullyQualifiedTableNameAtRuntime());
		insertClause.append(" (");

		Iterator<IntrospectedColumn> iter = allColumns.iterator();
		while (iter.hasNext()) {
			IntrospectedColumn col = iter.next();
			insertClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(col));
			if (iter.hasNext()) {
				insertClause.append(", ");
			}
		}
		insertClause.append(")");
		answer.addElement(new TextElement(insertClause.toString()));

		// --- 第二部分：values (参数...) ---
		StringBuilder valuesClause = new StringBuilder();
		valuesClause.append("values (");

		iter = allColumns.iterator();
		while (iter.hasNext()) {
			IntrospectedColumn col = iter.next();
			valuesClause.append(MyBatis3FormattingUtilities.getParameterClause(col));
			if (iter.hasNext()) {
				valuesClause.append(", ");
			}
		}
		valuesClause.append(")");
		answer.addElement(new TextElement(valuesClause.toString()));

		// --- 第三部分：on duplicate key update ... ---
		// 1. 筛选出非主键的列
		List<IntrospectedColumn> updateColumns = new ArrayList<>();
		for (IntrospectedColumn col : allColumns) {
			boolean isKey = false;
			for (IntrospectedColumn pk : pkColumns) {
				// 使用实际列名进行比较，这是最安全的做法
				if (col.getActualColumnName().equals(pk.getActualColumnName())) {
					isKey = true;
					break;
				}
			}
			if (!isKey) {
				updateColumns.add(col);
			}
		}

		// 2. 如果有可更新的列，生成 update 语句
		if (!updateColumns.isEmpty()) {
			StringBuilder updateClause = new StringBuilder();
			updateClause.append("on duplicate key update ");

			Iterator<IntrospectedColumn> updateIter = updateColumns.iterator();
			while (updateIter.hasNext()) {
				IntrospectedColumn col = updateIter.next();

				// 格式：column = #{column}
				updateClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(col));
				updateClause.append(" = ");
				updateClause.append(MyBatis3FormattingUtilities.getParameterClause(col));

				if (updateIter.hasNext()) {
					updateClause.append(", ");
				}
			}
			answer.addElement(new TextElement(updateClause.toString()));
		}

		document.getRootElement().addElement(answer);

		return true;
	}
}