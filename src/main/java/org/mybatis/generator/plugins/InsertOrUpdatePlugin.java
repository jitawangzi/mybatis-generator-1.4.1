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
	 * 生成 Java 接口方法
	 */
	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		Method method = new Method("insertOrUpdate");
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setReturnType(FullyQualifiedJavaType.getIntInstance());

		// 计算参数类型
		FullyQualifiedJavaType parameterType = introspectedTable.getRules().calculateAllFieldsClass();
		method.addParameter(new Parameter(parameterType, "record"));

		context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);
		interfaze.addMethod(method);
		return true;
	}

	/**
	 * 生成 XML SQL 语句
	 */
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		XmlElement root = document.getRootElement();

		// 1. 创建 XML 节点 <insert id="insertOrUpdate" ...>
		XmlElement answer = new XmlElement("insert");
		answer.addAttribute(new Attribute("id", "insertOrUpdate"));
		FullyQualifiedJavaType parameterType = introspectedTable.getRules().calculateAllFieldsClass();
		answer.addAttribute(new Attribute("parameterType", parameterType.getFullyQualifiedName()));

		context.getCommentGenerator().addComment(answer);

		// 2. 准备所有列
		List<IntrospectedColumn> allColumns = introspectedTable.getAllColumns();
		List<IntrospectedColumn> pkColumns = introspectedTable.getPrimaryKeyColumns();

		// 3. 构建 insert into (...) 部分
		StringBuilder insertClause = new StringBuilder();
		insertClause.append("insert into ");
		insertClause.append(introspectedTable.getFullyQualifiedTableNameAtRuntime());
		insertClause.append(" (");

		// 4. 构建 values (...) 部分
		StringBuilder valuesClause = new StringBuilder();
		valuesClause.append("values (");

		Iterator<IntrospectedColumn> iter = allColumns.iterator();
		while (iter.hasNext()) {
			IntrospectedColumn col = iter.next();

			insertClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(col));
			valuesClause.append(MyBatis3FormattingUtilities.getParameterClause(col));

			if (iter.hasNext()) {
				insertClause.append(", ");
				valuesClause.append(", ");
			}
		}
		insertClause.append(")");
		valuesClause.append(")");

		// 添加 insert 和 values 文本节点
		answer.addElement(new TextElement(insertClause.toString()));
		answer.addElement(new TextElement(valuesClause.toString()));

		// 5. 【核心修复】手动计算需要更新的列（排除主键）
		// 不再使用 getNonPrimaryKeyColumns()，防止因驱动差异导致返回空列表
		List<IntrospectedColumn> columnsToUpdate = new ArrayList<>();

		for (IntrospectedColumn col : allColumns) {
			boolean isPk = false;
			for (IntrospectedColumn pk : pkColumns) {
				// 忽略大小写对比列名，确保万无一失
				if (col.getActualColumnName().equalsIgnoreCase(pk.getActualColumnName())) {
					isPk = true;
					break;
				}
			}
			// 只要不是主键，就加入更新列表
			if (!isPk) {
				columnsToUpdate.add(col);
			}
		}

		// 6. 构建 on duplicate key update ... 部分
		if (!columnsToUpdate.isEmpty()) {
			StringBuilder updateClause = new StringBuilder();
			updateClause.append("on duplicate key update ");

			Iterator<IntrospectedColumn> updateIter = columnsToUpdate.iterator();
			while (updateIter.hasNext()) {
				IntrospectedColumn col = updateIter.next();

				updateClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(col));
				updateClause.append(" = ");
				updateClause.append(MyBatis3FormattingUtilities.getParameterClause(col));

				if (updateIter.hasNext()) {
					updateClause.append(", ");
				}
			}
			// 添加 update 文本节点
			answer.addElement(new TextElement(updateClause.toString()));
		}

		root.addElement(answer);
		return true;
	}
}