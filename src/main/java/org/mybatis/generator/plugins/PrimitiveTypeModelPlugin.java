package org.mybatis.generator.plugins;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 将PO类中的包装类型(Long/Integer/Double/Float/Boolean/Short/Byte)自动替换为基本类型
 */
public class PrimitiveTypeModelPlugin extends PluginAdapter {

	private static final Map<String, String> WRAPPER_TO_PRIMITIVE = new HashMap<>();
	static {
		WRAPPER_TO_PRIMITIVE.put("java.lang.Long", "long");
		WRAPPER_TO_PRIMITIVE.put("java.lang.Integer", "int");
		WRAPPER_TO_PRIMITIVE.put("java.lang.Double", "double");
		WRAPPER_TO_PRIMITIVE.put("java.lang.Float", "float");
		WRAPPER_TO_PRIMITIVE.put("java.lang.Boolean", "boolean");
		WRAPPER_TO_PRIMITIVE.put("java.lang.Short", "short");
		WRAPPER_TO_PRIMITIVE.put("java.lang.Byte", "byte");
		WRAPPER_TO_PRIMITIVE.put("java.lang.Character", "char");
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, org.mybatis.generator.api.Plugin.ModelClassType modelClassType) {
		String fqType = field.getType().getFullyQualifiedName();
		if (WRAPPER_TO_PRIMITIVE.containsKey(fqType)) {
			field.setType(new FullyQualifiedJavaType(WRAPPER_TO_PRIMITIVE.get(fqType)));
		}
		return true;
	}

	// 还需要同步修改 getter/setter 参数和返回值类型
	@Override
	public boolean modelGetterMethodGenerated(org.mybatis.generator.api.dom.java.Method method, TopLevelClass topLevelClass,
			IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
			org.mybatis.generator.api.Plugin.ModelClassType modelClassType) {
		String fqType = method.getReturnType().get().getFullyQualifiedName();
		if (WRAPPER_TO_PRIMITIVE.containsKey(fqType)) {
			method.setReturnType(new FullyQualifiedJavaType(WRAPPER_TO_PRIMITIVE.get(fqType)));
		}
		return true;
	}

	@Override
	public boolean modelSetterMethodGenerated(org.mybatis.generator.api.dom.java.Method method, TopLevelClass topLevelClass,
			IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
			org.mybatis.generator.api.Plugin.ModelClassType modelClassType) {

		if (method.getParameters().size() == 1) {
			org.mybatis.generator.api.dom.java.Parameter oldParam = method.getParameters().get(0);
			String fqType = oldParam.getType().getFullyQualifiedName();
			if (WRAPPER_TO_PRIMITIVE.containsKey(fqType)) {
				// 新建一个参数（参数名和注解都保留）
				org.mybatis.generator.api.dom.java.Parameter newParam = new org.mybatis.generator.api.dom.java.Parameter(
						new FullyQualifiedJavaType(WRAPPER_TO_PRIMITIVE.get(fqType)), oldParam.getName());
				// 拷贝注解
				for (String annotation : oldParam.getAnnotations()) {
					newParam.addAnnotation(annotation);
				}
				// 替换参数
				method.getParameters().set(0, newParam);
			}
		}
		return true;
	}
}