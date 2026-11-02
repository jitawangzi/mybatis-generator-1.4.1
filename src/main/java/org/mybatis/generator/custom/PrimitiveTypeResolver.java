/*
 *    Copyright 2006-2025 the original author or authors.
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
package org.mybatis.generator.custom;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl;

/**
 * 让MyBatis Generator字段类型全部采用基本类型（long/int等）
 *
 * 其实不配置这个，直接用插件也可以。
 */
public class PrimitiveTypeResolver extends JavaTypeResolverDefaultImpl {

	@Override
	public FullyQualifiedJavaType calculateJavaType(IntrospectedColumn introspectedColumn) {
		FullyQualifiedJavaType javaType = super.calculateJavaType(introspectedColumn);
		String fqType = javaType.getFullyQualifiedName();

		// 只处理常见包装类型
		switch (fqType) {
		case "java.lang.Long":
			return new FullyQualifiedJavaType("long");
		case "java.lang.Integer":
			return new FullyQualifiedJavaType("int");
		case "java.lang.Double":
			return new FullyQualifiedJavaType("double");
		case "java.lang.Float":
			return new FullyQualifiedJavaType("float");
		case "java.lang.Boolean":
			return new FullyQualifiedJavaType("boolean");
		case "java.lang.Short":
			return new FullyQualifiedJavaType("short");
		case "java.lang.Byte":
			return new FullyQualifiedJavaType("byte");
		// 其他类型（如String、Date等）不变
		default:
			return javaType;
		}
	}
}
