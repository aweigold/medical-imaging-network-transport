package org.nema.medical.mint.studies;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchResults {

    private String studyInstanceUID;
    private String accessionNumber;
    private String issuerOfAccessionNumber;
    private String patientID;
    private String issuerOfPatientID;
    private Timestamp minStudyDate;
    private Timestamp minStudyDateTime;
    private Timestamp maxStudyDate;
    private Timestamp maxStudyDateTime;
    private String timeZone;
    private int offset;
    private int limit;
	private final List<Study> studies = new ArrayList<Study>();

	public SearchResults() {
		//Default constructor for JiBX
	}

    public SearchResults(String studyInstanceUID, String accessionNumber, String issuerOfAccessionNumber, String patientID, String issuerOfPatientID, Timestamp minStudyDate, Timestamp minStudyDateTime, Timestamp maxStudyDate, Timestamp maxStudyDateTime, String timeZone, int offset, int limit) {
        this.studyInstanceUID = studyInstanceUID;
        this.accessionNumber = accessionNumber;
        this.issuerOfAccessionNumber = issuerOfAccessionNumber;
        this.patientID = patientID;
        this.issuerOfPatientID = issuerOfPatientID;
        this.minStudyDate = minStudyDate;
        this.minStudyDateTime = minStudyDateTime;
        this.maxStudyDate = maxStudyDate;
        this.maxStudyDateTime = maxStudyDateTime;
        this.timeZone = timeZone;
        this.offset = offset;
        this.limit = limit;
    }
    
	public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public String getIssuerOfAccessionNumber() {
        return issuerOfAccessionNumber;
    }

    public String getPatientID() {
        return patientID;
    }

    public String getIssuerOfPatientID() {
        return issuerOfPatientID;
    }

    public Timestamp getMinStudyDate() {
        return minStudyDate;
    }

    public Timestamp getMinStudyDateTime() {
        return minStudyDateTime;
    }

    public Timestamp getMaxStudyDate() {
        return maxStudyDate;
    }

    public Timestamp getMaxStudyDateTime() {
        return maxStudyDateTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }
    
    public Iterator<Study> resultsIterator() {
    	return studies.iterator();
    }
    
    public void addStudy(Study study) {
    	studies.add(study);
    }

    public void setStudyInstanceUID(String studyInstanceUID) {
		this.studyInstanceUID = studyInstanceUID;
	}

	public void setAccessionNumber(String accessionNumber) {
		this.accessionNumber = accessionNumber;
	}

	public void setIssuerOfAccessionNumber(String issuerOfAccessionNumber) {
		this.issuerOfAccessionNumber = issuerOfAccessionNumber;
	}

	public void setPatientID(String patientID) {
		this.patientID = patientID;
	}

	public void setIssuerOfPatientID(String issuerOfPatientID) {
		this.issuerOfPatientID = issuerOfPatientID;
	}

	public void setMinStudyDate(Timestamp minStudyDate) {
		this.minStudyDate = minStudyDate;
	}

	public void setMinStudyDateTime(Timestamp minStudyDateTime) {
		this.minStudyDateTime = minStudyDateTime;
	}

	public void setMaxStudyDate(Timestamp maxStudyDate) {
		this.maxStudyDate = maxStudyDate;
	}

	public void setMaxStudyDateTime(Timestamp maxStudyDateTime) {
		this.maxStudyDateTime = maxStudyDateTime;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}
}