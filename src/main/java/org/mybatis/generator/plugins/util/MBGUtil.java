package org.mybatis.generator.plugins.util;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

/**
 *
 * @date 2024年3月28日 下午6:12:07
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
