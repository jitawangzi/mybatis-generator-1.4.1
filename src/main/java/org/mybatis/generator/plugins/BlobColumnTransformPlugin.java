package org.mybatis.generator.plugins;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.codegen.AbstractJavaGenerator;
import org.mybatis.generator.config.BlobTransformColumn;
import org.mybatis.generator.internal.CustomCommentGenerator;
import org.mybatis.generator.internal.util.JavaBeansUtil;

/**
 * blob类型的列，转成业务对象
 */
public class BlobColumnTransformPlugin extends PluginAdapter {

	public BlobColumnTransformPlugin() {
		super();
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass,
			IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		BlobTransformColumn blobTransformColumn = introspectedTable.getTableConfiguration().getBlobTransformColumn(introspectedColumn
				.getActualColumnName());
		if (blobTransformColumn == null) {
			return true;
		}
		String domainObjectFieldName = blobTransformColumn.getDomainObjectFieldName();
		String domainObjectFieldType = blobTransformColumn.getDomainObjectFieldType();
		FullyQualifiedJavaType fullyQualifiedJavaType = new FullyQualifiedJavaType(domainObjectFieldType);
		// 生成 blob 列对应的java业务对象属性
		Field objectField = new Field(domainObjectFieldName, fullyQualifiedJavaType);
		objectField.setInitializationString("new " + fullyQualifiedJavaType + "()");
		objectField.setVisibility(JavaVisibility.PRIVATE);
		CustomCommentGenerator customCommentGenerator = new CustomCommentGenerator();
		customCommentGenerator.addJavadoc(objectField, introspectedColumn.getRemarks());

		topLevelClass.addField(objectField);

//			生成业务对象属性的set和get方法
		Method getter = AbstractJavaGenerator.getGetter(objectField);
		customCommentGenerator.addDefaultJavadoc(getter, false);
		IntrospectedColumn objColumn = new IntrospectedColumn();
		objColumn.setFullyQualifiedJavaType(fullyQualifiedJavaType);
		objColumn.setJavaProperty(domainObjectFieldName);
		Method javaBeansSetter = JavaBeansUtil.getJavaBeansSetter(objColumn, context, introspectedTable);
		topLevelClass.addMethod(getter);
		topLevelClass.addMethod(javaBeansSetter);
		return true;
	}

	@Override
	public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		BlobTransformColumn blobTransformColumn = introspectedTable.getTableConfiguration().getBlobTransformColumn(introspectedColumn
				.getActualColumnName());
		if (blobTransformColumn == null) {
			return true;
		}
		String domainObjectFieldName = blobTransformColumn.getDomainObjectFieldName();
		String domainObjectFieldType = blobTransformColumn.getDomainObjectFieldType();
		String blobColumn = blobTransformColumn.getBlobColumn();
		// 修改blob列set方法内容，做业务对象之间的转换
		if (introspectedColumn.getActualColumnName().equals(blobColumn)) {
			// 修改set方法
			String body;
			boolean isText = introspectedColumn.getFullyQualifiedJavaType().getFullyQualifiedNameWithoutTypeParameters()
					.equalsIgnoreCase("java.lang.String");
			if (isText) {
				// 默认json string
				body = String.format(
						"this.%s = com.alibaba.fastjson.JSON.parseObject(%s,\r\n"
								+ "				new com.alibaba.fastjson.TypeReference<%s>() {\r\n" + "				});",
						domainObjectFieldName, introspectedColumn.getJavaProperty(), domainObjectFieldType);
				body += "\r\n";
				body += String.format("        if (this.%s == null) {\r\n" + "        	this.%s = new %s() ;\r\n" + "		}",
						domainObjectFieldName, domainObjectFieldName, domainObjectFieldType);
			} else {
				// byte[]
				String fieldName = introspectedColumn.getJavaProperty();
				body = String.format("if (%s != null && %s.length > 0) {\r\n" + "			this.%s = (%s) util.KryoUtils\r\n"
						+ "					.deserializeClassAndObjectWithVersion(%s);\r\n" + "		}\r\n"
						+ "		if (this.%s == null) {\r\n"
						+ "			this.%s = new %s();\r\n"
						+ "		}", fieldName, fieldName, domainObjectFieldName, domainObjectFieldType, fieldName,
						domainObjectFieldName, domainObjectFieldName,domainObjectFieldType);
			}
			method.getBodyLines().clear();
			method.addBodyLine(body);
		}

		return true;
	}

	@Override
	public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, ModelClassType modelClassType) {

		BlobTransformColumn blobTransformColumn = introspectedTable.getTableConfiguration().getBlobTransformColumn(introspectedColumn
				.getActualColumnName());
		if (blobTransformColumn == null) {
			return true;
		}
		String domainObjectFieldName = blobTransformColumn.getDomainObjectFieldName();
		String domainObjectFieldType = blobTransformColumn.getDomainObjectFieldType();
		String blobColumn = blobTransformColumn.getBlobColumn();
		// 修改blob列get方法内容，做业务对象之间的转换
		if (introspectedColumn.getActualColumnName().equals(blobColumn)) {
			// 修改get方法
			String body;
			boolean isText = introspectedColumn.getFullyQualifiedJavaType().getFullyQualifiedNameWithoutTypeParameters()
					.equalsIgnoreCase("java.lang.String");
			if (isText) {
				// 默认json string
				body = String.format(
						"return com.alibaba.fastjson.JSON.toJSONString(this.%s);", domainObjectFieldName);
			} else {
				// byte[]
				body = String.format("return util.KryoUtils.serializeClassAndObjectWithVersion(this.%s);\r\n",
						domainObjectFieldName);
			}
			method.getBodyLines().clear();
			method.addBodyLine(body);
		}
		return true;
	}

}
