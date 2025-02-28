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
package org.mybatis.generator;

import java.util.ArrayList;
import java.util.List;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;

/**
 * 测试入口
 * 2024年11月8日 14:01:24
 * @author SYQ
 */
public class MBGTest {
	public static void main(String[] args) {
		try {
			List<String> warnings = new ArrayList<>();
			ConfigurationParser cp = new ConfigurationParser(warnings);
			Configuration config = cp
					.parseConfiguration(MBGTest.class.getClassLoader().getResourceAsStream("dataServer-mybatorConfig.xml"));

			DefaultShellCallback shellCallback = new DefaultShellCallback(true);
			MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, shellCallback, warnings);
			myBatisGenerator.generate(null, null, null, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("生成完成");

	}
}
