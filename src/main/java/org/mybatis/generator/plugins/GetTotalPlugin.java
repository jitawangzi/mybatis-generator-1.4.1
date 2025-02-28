package org.mybatis.generator.plugins;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * 查询总数插件
 * 2025年2月28日 16:48:24
 * @author SYQ
 */
public class GetTotalPlugin extends PluginAdapter {
	private static final String METHOD_NAME = "getTotal";

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
		// 添加Mapper接口方法
		Method method = new Method(METHOD_NAME);
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setReturnType(new FullyQualifiedJavaType("long"));
		method.setAbstract(true);

		// 添加JavaDoc
		context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);
		interfaze.addMethod(method);
		return true;
	}

	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		// 构建XML元素
		XmlElement select = new XmlElement("select");
		context.getCommentGenerator().addComment(select);
		select.addAttribute(new Attribute("id", METHOD_NAME));
		select.addAttribute(new Attribute("resultType", "java.lang.Long"));

		// 构建SQL语句
		String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
		select.addElement(new TextElement("SELECT COUNT(*) FROM " + tableName));

		// 添加到XML文档
		document.getRootElement().addElement(select);
		return true;
	}

	@Override
	public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		// 禁用默认的count查询（如果需要）
		return false;
	}
}