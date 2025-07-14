package org.mybatis.generator.plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;

/**
 * Mapper接口方法参数类型自动同步为基本类型（如 long, int），和PO一致
 */
public class PrimitiveTypeClientPlugin extends PluginAdapter {

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

	// 对所有Client方法参数类型做替换
	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		for (Method method : interfaze.getMethods()) {
			List<Parameter> params = method.getParameters();
			for (int i = 0; i < params.size(); i++) {
				Parameter param = params.get(i);
				String fqType = param.getType().getFullyQualifiedName();
				if (WRAPPER_TO_PRIMITIVE.containsKey(fqType)) {
					// 新建新的parameter（保留注解、名称）
					Parameter newParam = new Parameter(new FullyQualifiedJavaType(WRAPPER_TO_PRIMITIVE.get(fqType)), param.getName(),
							param.isVarargs());
					// 复制注解
					for (String annotation : param.getAnnotations()) {
						newParam.addAnnotation(annotation);
					}
					params.set(i, newParam);
				}
			}
		}
		return true;
	}

	// 部分MyBatis Generator版本还需要重写clientInsertMethodGenerated等方法，以下可选
	@Override
	public boolean clientInsertMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
		fixMethodParameters(method);
		return true;
	}

	@Override
	public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
		fixMethodParameters(method);
		return true;
	}

	@Override
	public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
		fixMethodParameters(method);
		return true;
	}

	private void fixMethodParameters(Method method) {
		List<Parameter> params = method.getParameters();
		for (int i = 0; i < params.size(); i++) {
			Parameter param = params.get(i);
			String fqType = param.getType().getFullyQualifiedName();
			if (WRAPPER_TO_PRIMITIVE.containsKey(fqType)) {
				Parameter newParam = new Parameter(new FullyQualifiedJavaType(WRAPPER_TO_PRIMITIVE.get(fqType)), param.getName(),
						param.isVarargs());
				for (String annotation : param.getAnnotations()) {
					newParam.addAnnotation(annotation);
				}
				params.set(i, newParam);
			}
		}
	}
}