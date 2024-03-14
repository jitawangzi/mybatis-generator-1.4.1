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
import org.mybatis.generator.api.dom.OutputUtilities;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.ListUtilities;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

/**
 * 不存在就插入，存在就更新,官方文档不建议同时插多条数据，有潜在风险，暂时只实现单条。
 * 2024年3月11日 下午2:56:41
 * @author SYQ
 */
public class InsertOrUpdatePlugin extends PluginAdapter {
	@Override
	public boolean validate(List<String> list) {
		return true;
	}

	@Override
	public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {

		if (element.getName().equals("insert")) {
			for (Attribute attribute : element.getAttributes()) {
				if (attribute.getName().equals("id") && attribute.getValue().equals("insertOrUpdate")) {

					StringBuilder updateClause = new StringBuilder();
					updateClause.append("ON DUPLICATE KEY UPDATE ");

					List<IntrospectedColumn> columns = ListUtilities
							.removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns());
					columns.removeAll(introspectedTable.getPrimaryKeyColumns());
					for (int i = 0; i < columns.size(); i++) {
						IntrospectedColumn introspectedColumn = columns.get(i);
						// 过滤不生成update的列
						if (context.isNotUpdateColumn(introspectedColumn)) {
							continue;
						}

						updateClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
						updateClause.append("=");

						updateClause.append("#{"); //$NON-NLS-1$
						updateClause.append(introspectedColumn.getJavaProperty());
						updateClause.append('}');
						if (i + 1 < columns.size()) {
							updateClause.append(", "); //$NON-NLS-1$
						}
						if (updateClause.length() > 80) {
							element.addElement(new TextElement(updateClause.toString()));
							updateClause.setLength(0);
							OutputUtilities.xmlIndent(updateClause, 1);
						}
					}
					element.addElement(new TextElement(updateClause.toString()));

					break;
				}
			}
		}
		return true;
	}
}
