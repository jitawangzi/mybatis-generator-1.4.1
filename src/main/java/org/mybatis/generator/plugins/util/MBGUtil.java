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
package org.mybatis.generator.plugins.util;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

/**
 *
 * 2024年3月28日 下午6:12:07
 * @author SYQ
 */
public class MBGUtil {

	/**
	 * 多字段主键时的主键生成
	 * @param introspectedTable
	 * @return
	 */
	public static String primaryKey(IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.size() == 1) {
			return primaryKeyColumns.get(0).getActualColumnName();
		} else if (primaryKeyColumns.size() > 1) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			for (int i = 0; i < primaryKeyColumns.size(); i++) {
				sb.append(primaryKeyColumns.get(i).getActualColumnName());
				if (i != primaryKeyColumns.size() - 1) {
					sb.append(",");
				}
			}
			sb.append(")");
			return sb.toString();
		}
		return null;
	}

	/**
	 * 多字段主键时的主键生成
	 * @param introspectedTable
	 * @return
	 */
	public static  String primaryKeyValue(IntrospectedTable introspectedTable) {
		// "#{item." + primaryKey(introspectedTable) + "}"
		// ((#{item.null},#{item.null}))
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.size() == 1) {
			return "#{item." + primaryKeyColumns.get(0).getJavaProperty() + "}";
		} else if (primaryKeyColumns.size() > 1) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			for (int i = 0; i < primaryKeyColumns.size(); i++) {
				sb.append("#{item." + primaryKeyColumns.get(i).getJavaProperty() + "}");
				if (i != primaryKeyColumns.size() - 1) {
					sb.append(",");
				}
			}
			sb.append(")");
			return sb.toString();
		}
		return null;
	}

	public static String primaryKeyForCaseWhen(IntrospectedTable introspectedTable) {
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.size() == 1) {
			return primaryKeyColumns.get(0).getActualColumnName();
		} else if (primaryKeyColumns.size() > 1) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < primaryKeyColumns.size(); i++) {
				sb.append(primaryKeyColumns.get(i).getActualColumnName());
				if (i != primaryKeyColumns.size() - 1) {
					sb.append(" and ");
				}
			}
			return sb.toString();
		}
		return null;
	}

	public static String primaryKeyParameterClause(IntrospectedTable introspectedTable) {

		StringBuilder sb = new StringBuilder();
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();

		for (int i = 0; i < primaryKeyColumns.size(); i++) {
			IntrospectedColumn introspectedColumn = primaryKeyColumns.get(i);
			sb.append(introspectedColumn.getActualColumnName()).append(" = ");
			sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn, "item."));
			if (i != primaryKeyColumns.size() - 1) {
				sb.append(" and ");
			}
		}
		return sb.toString();
	}
	public static String primaryKeyParameterWhere(IntrospectedTable introspectedTable) {

		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.size() == 1) {
			return MyBatis3FormattingUtilities.getParameterClause(primaryKeyColumns.get(0), "item.");
		} else if (primaryKeyColumns.size() > 1) {
			StringBuilder sb = new StringBuilder();
			sb.append("(") ;
			for (int i = 0; i < primaryKeyColumns.size(); i++) {
				IntrospectedColumn introspectedColumn = primaryKeyColumns.get(i);
				sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn, "item."));
				if (i != primaryKeyColumns.size() - 1) {
					sb.append(", ");
				}
			}
			sb.append(")") ;
			return sb.toString();
		}
		return null;
	}
}
