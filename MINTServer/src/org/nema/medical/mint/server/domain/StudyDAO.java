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
package org.nema.medical.mint.server.domain;

import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nema.medical.mint.server.controller.Utils;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class StudyDAO extends HibernateDaoSupport {

    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	private static int MAXSECONDS = 60 * 60 * 24;

	private int between(final int first, final int second, final int value) {
		final int min = Math.min(first, second);
		final int max = Math.max(first, second);
		return Math.min(Math.max(value, min), max);
	}

    @SuppressWarnings("unchecked")
	public List<Study> findStudies(String studyInstanceUID, String accessionNumber,
                                String accessionNumberIssuer, String patientID, String patientIDIssuer,
                                String minStudyDateTime, String minStudyDate, String maxStudyDateTime,
                                String maxStudyDate, int limit, int offset) throws ParseException {

        final DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Study.class);
        detachedCriteria.addOrder(Order.desc("lastModified"));
        
        if (studyInstanceUID != null && StringUtils.isNotBlank(studyInstanceUID)){
            detachedCriteria.add(Restrictions.eq("studyInstanceUID", studyInstanceUID));
        }
        if (accessionNumber != null && StringUtils.isNotBlank(accessionNumber)){
            detachedCriteria.add(Restrictions.eq("accessionNumber", accessionNumber));
        }
        if (accessionNumberIssuer != null && StringUtils.isNotBlank(accessionNumberIssuer)){
            //TODO rename issuerOfAccessionNumber to accessionNumberIssuer
            detachedCriteria.add(Restrictions.eq("issuerOfAccessionNumber", accessionNumberIssuer));
        }
        if (patientID != null && StringUtils.isNotBlank(patientID)){
            detachedCriteria.add(Restrictions.eq("patientID", patientID));
        }
        if (patientIDIssuer != null && StringUtils.isNotBlank(patientIDIssuer)){
            //TODO rename issuerOfPatientID to patientIDIssuer
            detachedCriteria.add(Restrictions.eq("issuerOfPatientID", patientIDIssuer));
        }
        if (minStudyDateTime != null && StringUtils.isNotBlank(minStudyDateTime)){
            detachedCriteria.add(Restrictions.ge("dateTime", Utils.parseISO8601(minStudyDateTime)));
        }
        if (minStudyDate != null && StringUtils.isNotBlank(minStudyDate)){
            detachedCriteria.add(Restrictions.ge("dateTime", Utils.parseISO8601Date(minStudyDate)));
        }
        if (maxStudyDateTime != null && StringUtils.isNotBlank(maxStudyDateTime)){
            detachedCriteria.add(Restrictions.le("dateTime", Utils.parseISO8601(maxStudyDateTime)));
        }
        if (maxStudyDate != null && StringUtils.isNotBlank(maxStudyDate)){
            detachedCriteria.add(Restrictions.lt("dateTime", Utils.parseISO8601Date(maxStudyDate)));
        }

        int firstResult = (offset-1) * limit;
        final List<Study> list = (List<Study>)getHibernateTemplate().findByCriteria(detachedCriteria,firstResult,limit);
        return list;
    }

	@SuppressWarnings("unchecked")
	public Study findStudy(final String uuid) {
		if (StringUtils.isNotBlank(uuid)) {
			final DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Study.class).add(
					Restrictions.eq("id", uuid));
			final List<Study> list = getHibernateTemplate().findByCriteria(detachedCriteria);
			if (!list.isEmpty()) {
				return list.get(0);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<Study> getMostRecentStudies(final int max, final int seconds) {
		final Calendar calendar = Calendar.getInstance(GMT);
		calendar.add(Calendar.SECOND, -between(1, MAXSECONDS, seconds));

		final DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Study.class)
				.add(Restrictions.ge("lastModified", calendar.getTime())).addOrder(Order.desc("lastModified"));
		final List<Study> list = getHibernateTemplate().findByCriteria(detachedCriteria, 0, between(1, 50, max));
		return list;
	}

    public Study insertStudy(final Study study) {
        if (study != null) {
            study.setLastModified(Study.now());
            getHibernateTemplate().save(study);
            getHibernateTemplate().flush();
            getHibernateTemplate().refresh(study);
        }
        return study;
    }

    public Study updateStudy(final Study study) {
        if (study != null) {
            study.setLastModified(Study.now());
            getHibernateTemplate().saveOrUpdate(study);
            getHibernateTemplate().flush();
            getHibernateTemplate().refresh(study);
        }
        return study;
    }
}
