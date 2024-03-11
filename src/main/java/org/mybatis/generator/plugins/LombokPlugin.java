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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerEnum;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

public class LombokPlugin extends PluginAdapter {
    private final Collection<Annotations> annotations;
    public LombokPlugin() {
        annotations = new HashSet<Annotations>(Annotations.values().length);
    }
    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }
    /**
     * Intercepts base record class generation
     *
     * @param topLevelClass
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
                                                 IntrospectedTable introspectedTable) {
        addDataAnnotation(topLevelClass, true);
        addTableColumn(topLevelClass, introspectedTable);
        return true;
    }
    /**
     * Intercepts primary key class generation
     *
     * @param topLevelClass
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass,
                                                 IntrospectedTable introspectedTable) {
        addDataAnnotation(topLevelClass, false);
        addTableColumn(topLevelClass, introspectedTable);
        return true;
    }
    /**
     * Intercepts "record with blob" class generation
     *
     * @param topLevelClass
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean modelRecordWithBLOBsClassGenerated(
            TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        addDataAnnotation(topLevelClass, true);
        addTableColumn(topLevelClass, introspectedTable);
        return true;
    }
    /**
     * Prevents all getters from being generated.
     * See SimpleModelGenerator
     *
     * @param method
     * @param topLevelClass
     * @param introspectedColumn
     * @param introspectedTable
     * @param modelClassType
     * @return
     */
    @Override
    public boolean modelGetterMethodGenerated(Method method,
                                              TopLevelClass topLevelClass,
                                              IntrospectedColumn introspectedColumn,
                                              IntrospectedTable introspectedTable,
                                              ModelClassType modelClassType) {
        return false;
    }
    /**
     * Prevents all setters from being generated
     * See SimpleModelGenerator
     *
     * @param method
     * @param topLevelClass
     * @param introspectedColumn
     * @param introspectedTable
     * @param modelClassType
     * @return
     */
    @Override
    public boolean modelSetterMethodGenerated(Method method,
                                              TopLevelClass topLevelClass,
                                              IntrospectedColumn introspectedColumn,
                                              IntrospectedTable introspectedTable,
                                              ModelClassType modelClassType) {
        return false;
    }
    /**
     * Adds the lombok annotations' imports and annotations to the class
     *
     * @param topLevelClass
     */
    private void addDataAnnotation(TopLevelClass topLevelClass, boolean builder) {
        for (Annotations annotation : annotations) {
            if (!builder && annotation.paramName.equals("builder")) {
                continue;
            }
            topLevelClass.addImportedType(annotation.javaType);
            topLevelClass.addAnnotation(annotation.name);
        }
    }
    private void addTableColumn(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        InnerEnum innerEnum = new InnerEnum(new FullyQualifiedJavaType("COLUMNS"));
        innerEnum.setVisibility(JavaVisibility.PUBLIC);
        innerEnum.setStatic(true);
        Field columnField = new Field("column", FullyQualifiedJavaType.getStringInstance());
        columnField.setVisibility(JavaVisibility.PRIVATE);
        columnField.addAnnotation(Annotations.GETTER.name);
        innerEnum.addField(columnField);
        introspectedTable.getAllColumns().forEach(introspectedColumn -> {
            StringBuilder sb = new StringBuilder();
            sb.append(introspectedColumn.getActualColumnName().toUpperCase());
            sb.append("(\"");
            sb.append(introspectedColumn.getActualColumnName());
            sb.append("\")");
            innerEnum.addEnumConstant(sb.toString());
        });
        innerEnum.addAnnotation(Annotations.ALL_ARGS_CONSTRUCTOR.name);
        topLevelClass.addInnerEnum(innerEnum);
    }
    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
		// 要添加到类上的注解
        annotations.add(Annotations.DATA);
        annotations.add(Annotations.GETTER);
        annotations.add(Annotations.BUILDER);
        annotations.add(Annotations.ALL_ARGS_CONSTRUCTOR);
        for (Entry<Object, Object> entry : properties.entrySet()) {
            boolean isEnable = Boolean.parseBoolean(entry.getValue().toString());
            if (isEnable) {
                String paramName = entry.getKey().toString().trim();
                Annotations annotation = Annotations.getValueOf(paramName);
                if (annotation != null) {
                    annotations.add(annotation);
                    annotations.addAll(Annotations.getDependencies(annotation));
                }
            }
        }
    }
    private enum Annotations {
        GETTER("getter", "@Getter", "lombok.Getter"),
        SETTER("setter", "@Setter", "lombok.Setter"),
        DATA("data", "@Data", "lombok.Data"),
        BUILDER("builder", "@Builder", "lombok.Builder"),
        ALL_ARGS_CONSTRUCTOR("allArgsConstructor", "@AllArgsConstructor", "lombok.AllArgsConstructor"),
        NO_ARGS_CONSTRUCTOR("noArgsConstructor", "@NoArgsConstructor", "lombok.NoArgsConstructor"),
        TO_STRING("toString", "@ToString", "lombok.ToString");
        private final String paramName;
        private final String name;
        private final FullyQualifiedJavaType javaType;
        Annotations(String paramName, String name, String className) {
            this.paramName = paramName;
            this.name = name;
            this.javaType = new FullyQualifiedJavaType(className);
        }
        private static Annotations getValueOf(String paramName) {
            for (Annotations annotation : Annotations.values()){
                if (String.CASE_INSENSITIVE_ORDER.compare(paramName, annotation.paramName) == 0) {
                    return annotation;
                }
            }
            return null;
        }
        private static Collection<Annotations> getDependencies(Annotations annotation) {
            if (annotation == ALL_ARGS_CONSTRUCTOR) {
                return Collections.singleton(NO_ARGS_CONSTRUCTOR);
            } else {
                return Collections.emptyList();
            }
        }
    }
}