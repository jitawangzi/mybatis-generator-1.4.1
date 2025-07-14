package org.mybatis.generator.plugins;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.xml.XmlElement;

public class DisableSelectiveMethodPlugin extends PluginAdapter {

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	// 禁止生成 insertSelective 方法
	@Override
	public boolean clientInsertSelectiveMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
		return false;
	}


	// 禁止生成 updateByPrimaryKeySelective 方法
	@Override
	public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
//		if (method.getName().equals("updateByPrimaryKeySelective")) {
//			return false;
//		}
		return false;
	}

	// 禁止生成 insertSelective XML
	@Override
	public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		return false;
	}

	// 禁止生成 updateByPrimaryKeySelective XML
	@Override
	public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		return false;
	}
}