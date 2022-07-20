/*
 *    Copyright ${license.git.copyrightYears} the original author or authors.
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
package org.mybatis.generator.internal;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.JavaElement;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.util.StringUtility;

/**
 * 自定义注释生成，修改的部分复写父类的方法
 */
public class CustomCommentGenerator extends DefaultCommentGenerator {

	// 表注释加到类注释里
	@Override
	public void addModelClassComment(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		if (suppressAllComments || !addRemarkComments) {
			return;
		}
		String tableDesc = introspectedTable.getRemarks();
		if (tableDesc != null && tableDesc.length() > 0) {
			addJavadoc(topLevelClass, tableDesc);
		}
	}

	/**
	 * Database Column Remarks:
	 *   数据库唯一id
	 *
	 * This field was generated by MyBatis Generator.
	 * This field corresponds to the database column t_role.id
	 *
	 * @mbg.generated
	 */
	//把默认的上面这种注释，换成下面这种在一行里的
//	/** @mbg.generated 数据库唯一id*/\
	// 不能放一行，合并生成时格式有问题。
	@Override
	public void addFieldComment(Field field, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
		if (suppressAllComments) {
			return;
		}
		StringBuffer comment = new StringBuffer() ;
//
//		comment.append("/** ").append("\r\n").append(" * ").
//		append(MergeConstants.NEW_ELEMENT_TAG).append("\r\n").append(" */");
//
//
//		comment.append("/** ").append("\r\n").append(" * ").append(MergeConstants.NEW_ELEMENT_TAG).append(" ");
//
		String remarks = introspectedColumn.getRemarks();
		if (addRemarkComments && StringUtility.stringHasValue(remarks)) {
			String[] remarkLines = remarks.split(System.getProperty("line.separator")); //$NON-NLS-1$
			for (int i = 0; i < remarkLines.length; i++) {
				comment.append(remarkLines[i]);
				if (i != remarkLines.length - 1) {
					comment.append(",");
				}
			}
		}
//		comment.append("\r\n");
//		comment.append(" */") ;
//
//		field.addJavaDocLine(comment.toString()); //$NON-NLS-1$

		addJavadoc(field, comment.toString());
	}

	@Override
	public void addFieldComment(Field field, IntrospectedTable introspectedTable) {
//		field.addJavaDocLine("/**");
//		addJavadocTag(field, false);
//		field.addJavaDocLine(" */");
		addDefaultJavadoc(field, false);
	}

	// get方法的注释，
	@Override
	public void addGetterComment(Method method, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
		if (suppressAllComments) {
			return;
		}
		addDefaultJavadoc(method, false);
	}

	@Override
	public void addSetterComment(Method method, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
		if (suppressAllComments) {
			return;
		}
		addDefaultJavadoc(method, false);
	}

	// mapper里面方法的注释
	@Override
	public void addGeneralMethodComment(Method method, IntrospectedTable introspectedTable) {
		if (suppressAllComments) {
			return;
		}
		addDefaultJavadoc(method, false);
	}

	@Override
	public void addJavadocTag(JavaElement javaElement, boolean markAsDoNotDelete) {
		super.addJavadocTag(javaElement, markAsDoNotDelete);
	}
	/**
	 * 增加默认的注释
	 * @param javaElement
	 * @param markAsDoNotDelete
	 */
	public void addDefaultJavadoc(JavaElement javaElement, boolean markAsDoNotDelete) {
		addJavadoc(javaElement, null);
	}
	/**
	 * 增加注释
	 * @param javaElement
	 */
	public void addJavadoc(JavaElement javaElement, String remark) {

		javaElement.addJavaDocLine("/**");
		if (remark != null && remark.length() > 0) {
			javaElement.addJavaDocLine(" * " + remark);
		}
		addJavadocTag(javaElement, false);
		javaElement.addJavaDocLine(" */");
	}

}
