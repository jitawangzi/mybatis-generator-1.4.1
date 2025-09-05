package org.mybatis.generator.plugins;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.mybatis.generator.api.FullyQualifiedTable;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.VisitableElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

/**
 * 根据表的索引，生成 delete 方法
 * 生成方法名：deleteBy{IndexNameCamel}
 * 参数：与索引列一致（多列索引多参数，搭配 @Param）
 * 返回：int（受影响行数）
 */
public class DeleteByIndexPlugin extends PluginAdapter {

	public DeleteByIndexPlugin() {
		super();
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	// 初始化，查询并设置表的索引数据到 IntrospectedTable
	@Override
	public void initialized(IntrospectedTable introspectedTable) {
		try {
			Connection connection = context.getConnection();
			DatabaseMetaData metaData = connection.getMetaData();
			Map<String, List<IntrospectedColumn>> indexMap = new HashMap<>(5);
			Map<String, Boolean> uniqueIndexMap = new HashMap<>(5);
			FullyQualifiedTable fullyQualifiedTable = introspectedTable.getFullyQualifiedTable();

			ResultSet rs = metaData.getIndexInfo(fullyQualifiedTable.getIntrospectedCatalog(), fullyQualifiedTable.getIntrospectedSchema(),
					fullyQualifiedTable.getIntrospectedTableName(), false, false);

			while (rs.next()) {
				String indexName = rs.getString("INDEX_NAME");
				if ("PRIMARY".equals(indexName)) {
					continue;
				}
				Optional<IntrospectedColumn> columnOptional = introspectedTable.getColumn(rs.getString("COLUMN_NAME"));
				if (!columnOptional.isPresent()) {
					continue;
				}

				// 收集列
				if (indexMap.containsKey(indexName)) {
					indexMap.get(indexName).add(columnOptional.get());
				} else {
					List<IntrospectedColumn> cols = new ArrayList<>();
					cols.add(columnOptional.get());
					indexMap.put(indexName, cols);
				}

				// 记录唯一性（NON_UNIQUE=false 表示唯一索引）
				boolean nonUnique = rs.getBoolean("NON_UNIQUE");
				boolean isUnique = !nonUnique;
				uniqueIndexMap.merge(indexName, isUnique, (oldVal, newVal) -> oldVal || newVal);
			}
			rs.close();

			introspectedTable.setAttribute("indexs_for_delete", indexMap);
			introspectedTable.setAttribute("uniqueIndexFlags_for_delete", uniqueIndexMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 在 Mapper 接口中生成 deleteBy 索引名 方法
	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		@SuppressWarnings("unchecked")
		Map<String, List<IntrospectedColumn>> indexMap = (Map<String, List<IntrospectedColumn>>) introspectedTable
				.getAttribute("indexs_for_delete");

		if (indexMap == null || indexMap.isEmpty()) {
			return super.clientGenerated(interfaze, introspectedTable);
		}

		for (Entry<String, List<IntrospectedColumn>> entry : indexMap.entrySet()) {
			String indexName = entry.getKey();
			String methodName = getDeleteMethodName(indexName);

			Method method = new Method(methodName);
			method.setVisibility(JavaVisibility.PUBLIC);
			method.setAbstract(true);
			method.setReturnType(FullyQualifiedJavaType.getIntInstance()); // 返回影响行数

			// 参数
			List<IntrospectedColumn> columns = entry.getValue();
			List<Parameter> parameters = columns.stream()
					.map(it -> new Parameter(it.getFullyQualifiedJavaType(), it.getJavaProperty(),
							"@Param(\"" + it.getJavaProperty() + "\")"))
					.collect(Collectors.toList());
			for (Parameter para : parameters) {
				method.addParameter(para);
			}

			// 注释
			context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);

			// 导入
			Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
			importedTypes.add(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Param"));
			interfaze.addImportedTypes(importedTypes);

			interfaze.addMethod(method);
		}

		return super.clientGenerated(interfaze, introspectedTable);
	}

	// 在 mapper.xml 中生成 <delete> 节点
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		@SuppressWarnings("unchecked")
		Map<String, List<IntrospectedColumn>> indexMap = (Map<String, List<IntrospectedColumn>>) introspectedTable
				.getAttribute("indexs_for_delete");

		if (indexMap == null || indexMap.isEmpty()) {
			return true;
		}

		for (Entry<String, List<IntrospectedColumn>> entry : indexMap.entrySet()) {
			String indexName = entry.getKey();
			String methodName = getDeleteMethodName(indexName);
			List<IntrospectedColumn> columns = entry.getValue();

			XmlElement delete = new XmlElement("delete");
			context.getCommentGenerator().addComment(delete);
			delete.addAttribute(new Attribute("id", methodName));

			// 参数类型（与 select 的规则一致）
			String parameterType;
			if (columns.size() == 1) {
				parameterType = columns.get(0).getFullyQualifiedJavaType().getFullyQualifiedName();
			} else {
				parameterType = "map";
			}
			delete.addAttribute(new Attribute("parameterType", parameterType));

			// delete from table
			StringBuilder sb = new StringBuilder();
			sb.append("delete from ");
			sb.append(introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime());
			delete.addElement(new TextElement(sb.toString()));

			// where
			for (VisitableElement where : generateWheres(columns)) {
				delete.addElement(where);
			}

			addElementWithBestPosition(document.getRootElement(), delete);
		}

		return true;
	}

	private String getDeleteMethodName(String indexName) {
		return "deleteBy" + makeIndex(indexName);
	}

	private String makeIndex(String name) {
		String indexName = name.replace("idx_", "");
		return upperIndex(indexName);
	}

	private String upperIndex(String name) {
		String[] split = name.split("_");
		String ret = "";
		for (int i = 0; i < split.length; i++) {
			char[] arr = split[i].toCharArray();
			if (arr.length > 0) {
				arr[0] = Character.toUpperCase(arr[0]);
			}
			ret += new String(arr);
		}
		return ret.length() == 0 ? name : ret;
	}

	private List<VisitableElement> generateWheres(List<IntrospectedColumn> uniqueKey) {
		List<VisitableElement> answer = new ArrayList<>();
		boolean and = false;
		StringBuilder sb = new StringBuilder();
		for (IntrospectedColumn introspectedColumn : uniqueKey) {
			sb.setLength(0);
			if (and) {
				sb.append("  and ");
			} else {
				sb.append("where ");
				and = true;
			}
			sb.append(MyBatis3FormattingUtilities.getAliasedEscapedColumnName(introspectedColumn));
			sb.append(" = ");
			sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
			answer.add(new TextElement(sb.toString()));
		}
		return answer;
	}

	/**
	 * 在最佳位置添加节点（与 SelectByIndexPlugin 相同策略）
	 */
	public void addElementWithBestPosition(XmlElement rootElement, XmlElement element) {
		if (element.getName().equals("sql")) {
			int index = 0;
			for (VisitableElement ele : rootElement.getElements()) {
				if (ele instanceof XmlElement && ((XmlElement) ele).getName().equals("sql")) {
					index++;
				}
			}
			rootElement.addElement(index, element);
		} else {
			String id = getIdFromElement(element);
			if (id == null) {
				rootElement.addElement(element);
			} else {
				List<VisitableElement> elements = rootElement.getElements();
				int index = -1;
				for (int i = 0; i < elements.size(); i++) {
					VisitableElement ele = elements.get(i);
					if (ele instanceof XmlElement) {
						String eleId = getIdFromElement((XmlElement) ele);
						if (eleId != null) {
							if (eleId.startsWith(id)) {
								if (index == -1) {
									index = i;
								}
							} else if (id.startsWith(eleId)) {
								index = i + 1;
							}
						}
					}
				}
				if (index == -1 || index >= elements.size()) {
					rootElement.addElement(element);
				} else {
					elements.add(index, element);
				}
			}
		}
	}

	private String getIdFromElement(XmlElement element) {
		for (Attribute attribute : element.getAttributes()) {
			if (attribute.getName().equals("id")) {
				return attribute.getValue();
			}
		}
		return null;
	}
}