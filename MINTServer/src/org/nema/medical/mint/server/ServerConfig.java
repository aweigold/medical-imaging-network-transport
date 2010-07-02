/*
 *   Copyright 2010 MINT Working Group
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.nema.medical.mint.server;

import java.io.File;

import org.nema.medical.mint.common.MINTCommonConfiguration;
import org.nema.medical.mint.common.domain.JobInfoDAO;
import org.nema.medical.mint.common.domain.StudyDAO;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@Configuration
public class ServerConfig extends MINTCommonConfiguration {

	protected StudyDAO studyDAO = null;
	protected JobInfoDAO jobInfoDAO = null;
	protected File jobTemp = null;
	protected File studiesRoot = null;

	@Bean(name = "jobTemp", autowire = Autowire.BY_NAME)
	public File jobTemp() throws Exception {
		jobTemp = new File(mintHome(), "jobtemp");
		return jobTemp;
	}

	@Bean(name = "studiesRoot", autowire = Autowire.BY_NAME)
	public File studiesRoot() throws Exception {
		studiesRoot = new File(mintHome(), "studies");
		return studiesRoot;
	}

	@Bean(name = "studyDAO", autowire = Autowire.BY_NAME)
	public StudyDAO studyDAO() throws Exception {
		if (studyDAO == null) {
			studyDAO = new StudyDAO();
			studyDAO.setSessionFactory(sessionFactory());
			studyDAO.afterPropertiesSet();
		}
		return studyDAO;
	}

	@Bean(name = "jobInfoDAO", autowire = Autowire.BY_NAME)
	public JobInfoDAO jobInfoDAO() throws Exception {
		if (jobInfoDAO == null) {
			jobInfoDAO = new JobInfoDAO();
			jobInfoDAO.setSessionFactory(sessionFactory());
			jobInfoDAO.afterPropertiesSet();
		}
		return jobInfoDAO;
	}

	@Bean
	public ViewResolver viewResolver() {
		final InternalResourceViewResolver internalResourceViewResolver = new InternalResourceViewResolver();
		internalResourceViewResolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
		internalResourceViewResolver.setPrefix("/WEB-INF/jsp/");
		internalResourceViewResolver.setSuffix(".jsp");
		internalResourceViewResolver.setViewClass(JstlView.class);
		return internalResourceViewResolver;
	}
}