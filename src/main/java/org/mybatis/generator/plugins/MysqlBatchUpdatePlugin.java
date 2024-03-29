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

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.ListUtilities;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.plugins.util.MBGUtil;
import org.mybatis.generator.plugins.util.MethodGeneratorTool;
import org.mybatis.generator.plugins.util.SqlMapperGeneratorTool;

public class MysqlBatchUpdatePlugin extends PluginAdapter {

    private final static String BATCH_UPDATE = "updateBatch";

    private final static String PARAMETER_NAME = "recordList";


    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public boolean clientGenerated(Interface interfaze,  IntrospectedTable introspectedTable) {

        if (introspectedTable.getTargetRuntime().equals(IntrospectedTable.TargetRuntime.MYBATIS3)) {
            MethodGeneratorTool.defaultBatchInsertOrUpdateMethodGen(MethodGeneratorTool.UPDATE, interfaze, introspectedTable, context);
        }
        return super.clientGenerated(interfaze, introspectedTable);
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        if (introspectedTable.getTargetRuntime().equals(IntrospectedTable.TargetRuntime.MYBATIS3)) {
            addSqlMapper(document, introspectedTable);
        }
        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }

    public void addSqlMapper(Document document, IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
//        List<IntrospectedColumn> columnList = introspectedTable.getAllColumns();

		List<IntrospectedColumn> columnList = ListUtilities
				.removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns());
		columnList.removeAll(introspectedTable.getPrimaryKeyColumns());

        //primaryKey的JDBC名字
        List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		String primaryKeyName = primaryKeyColumns.get(0).getActualColumnName();

        //primaryKey的JAVA变量
//        String primaryKeyParameterClause = MyBatis3FormattingUtilities.getParameterClause(introspectedTable.getPrimaryKeyColumns().get(0), "item.");
        String primaryKeyParameterClause = MBGUtil.primaryKeyParameterClause(introspectedTable);

        //primaryKey的JAVA名字
        String primaryKeyJavaName = primaryKeyColumns.get(0).getJavaProperty();

        XmlElement updateXmlElement = SqlMapperGeneratorTool.baseElementGenerator(SqlMapperGeneratorTool.UPDATE,
                BATCH_UPDATE,
                FullyQualifiedJavaType.getNewListInstance());

		context.getCommentGenerator().addComment(updateXmlElement);

        updateXmlElement.addElement(new TextElement(String.format("update %s ", tableName)));

        XmlElement trimElement = SqlMapperGeneratorTool.baseTrimElement("set", null, ",");

        for (int i = 0; i < columnList.size(); i++) {

            IntrospectedColumn introspectedColumn = columnList.get(i);

        	// 过滤不生成update的列
			if (context.isNotUpdateColumn(introspectedColumn)) {
				continue;
			}

            String columnName = introspectedColumn.getActualColumnName();

            String columnJavaTypeName = introspectedColumn.getJavaProperty("item.");

            String parameterClause = MyBatis3FormattingUtilities.getParameterClause(introspectedColumn, "item.");


            if (introspectedColumn.isIdentity()) {
                continue;
            }

            String ifSql = String.format("when %s then %s", primaryKeyParameterClause, parameterClause);
            XmlElement ifElement = SqlMapperGeneratorTool.baseIfJudgeElementGen(columnJavaTypeName, ifSql, false);

            String ifNullSql = String.format("when %s then %s", primaryKeyParameterClause, tableName + "." + columnName);
            XmlElement ifNullElement = SqlMapperGeneratorTool.baseIfJudgeElementGen(columnJavaTypeName, ifNullSql, true);


            XmlElement foreachElement = SqlMapperGeneratorTool.baseForeachElementGenerator(PARAMETER_NAME, "item", "index", null);
//            foreachElement.addElement(ifElement);
//            foreachElement.addElement(ifNullElement);
            foreachElement.addElement(new TextElement(ifSql));

            XmlElement caseTrimElement = SqlMapperGeneratorTool.baseTrimElement(columnName + " =case ", "end,", null);
            caseTrimElement.addElement(foreachElement);

            trimElement.addElement(caseTrimElement);
        }

        updateXmlElement.addElement(trimElement);

        XmlElement foreachElement = SqlMapperGeneratorTool.baseForeachElementGenerator(PARAMETER_NAME,
                "item",
                "index",
                ",");
        foreachElement.addElement(new TextElement( MBGUtil.primaryKeyParameterWhere(introspectedTable)));

        updateXmlElement.addElement(new TextElement(String.format("where %s in(", MBGUtil.primaryKey(introspectedTable))));

        updateXmlElement.addElement(foreachElement);

        updateXmlElement.addElement(new TextElement(")"));

        document.getRootElement().addElement(updateXmlElement);
    }
}

