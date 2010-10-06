<%@ page language="java" contentType="text/xml; charset=utf-8"
	pageEncoding="ISO-8859-1"%><%@ taglib prefix="c"
	uri="http://java.sun.com/jsp/jstl/core"%><?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mint="http://medical.nema.org/mint">

<xsl:template match="/">
	<html>
		<body>
			<xsl:apply-templates/>
		</body>
	</html>
</xsl:template>

<xsl:template match="mint:studySearchResults">
	<h1>Studies</h1>
	<ol>
	<xsl:for-each select="mint:study">
		<li>
			<dl>
				<dt>MINT Study UUID</dt>
				<dd class='StudyUUID'><a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="./@studyUUID"/></xsl:attribute><xsl:value-of select="./@studyUUID"/></a></dd>
				<dt>Last Modified</dt>
				<dd class='LastModified'><xsl:value-of select="./@lastModified"/></dd>
				<dt>Study Version</dt>
				<dd class='StudyVersion'><xsl:value-of select="./@version"/></dd>
				<dt>Links</dt>
				<dd class='StudySummary'><a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="./@studyUUID"/>/DICOM/summary</xsl:attribute>Summary</a></dd>
				<dd class='StudyMetadata'><a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="./@studyUUID"/>/DICOM/metadata</xsl:attribute>Metadata</a></dd>
				<dd class='StudyChangeLog'><a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="./@studyUUID"/>/changelog</xsl:attribute>ChangeLog</a></dd>
			</dl>
		</li>
	</xsl:for-each>
	</ol>
</xsl:template>

<xsl:template match="mint:jobStatus">
	<h1>Job Information</h1>
	<dl>
		<dt>JobID</dt>
		<dd class='JobID'><xsl:value-of select="./@jobID"/></dd>
		<dt>Study ID</dt>
		<dd class='StudyUUID'><a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="./@studyUUID"/></xsl:attribute><xsl:value-of select="./@studyUUID"/></a></dd>
		<dt>Job Status</dt>
		<dd class='JobStatus'><xsl:value-of select="./@jobStatus"/></dd>
		<dt>Job Created</dt>
		<dd class='JobCreated'><xsl:value-of select="./@jobCreated"/></dd>
		<dt>Job Updated</dt>
		<dd class='JobUpdated'><xsl:value-of select="./@jobUpdated"/></dd>
	</dl>
</xsl:template>

<xsl:template match="mint:studyRoot">
	<h1>MINT Study</h1>
	<dl>
		<dt>MINT Study UUID</dt>
		<dd class='StudyUUID'><a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="./@studyUUID"/></xsl:attribute><xsl:value-of select="./@studyUUID"/></a></dd>
		<dt>Last Modified</dt>
		<dd class='LastModified'><xsl:value-of select="./@lastUpdate"/></dd>
		<dt>Study Version</dt>
		<dd class='StudyVersion'><xsl:value-of select="./@version"/></dd>
	</dl>
	<h2>Types</h2>
	<ul>
	<xsl:for-each select="mint:type">
		<li class='type'>
    		<a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="../@studyUUID"/>/<xsl:value-of select="./@name"/>/summary</xsl:attribute><xsl:value-of select="./@name"/> Summary</a>
		</li>
	</xsl:for-each>
	</ul>
</xsl:template>

<xsl:template match="mint:studyMeta">
	<h1>Study Metadata</h1>
	<dl>
		<dt>Study Instance UID</dt>
		<dd class='InstanceUID'><xsl:value-of select="./@studyInstanceUID"/></dd>
		<dt>Instance Count</dt>
		<dd class='InstanceCount'><xsl:value-of select="./@instanceCount"/></dd>
		<xsl:if test="./@type">
			<dt>Type</dt>
			<dd class='Type'><xsl:value-of select="./@type"/></dd>
		</xsl:if>
		<xsl:if test="./@version">
			<dt>Version</dt>
			<dd class='Version'><xsl:value-of select="./@version"/></dd>
		</xsl:if>
		<xsl:for-each select="mint:attributes">
		<dt>Attributes</dt>
		<dd>
			<table border="1" cellpadding="2" cellspacing="0">
				<tr>
					<th>Attribute Tag</th>
					<th>VR</th>
					<th>Value</th>
					<th>Bid</th>
					<th>Frame count</th>
					<th>Bytes</th>
				</tr>
				<xsl:apply-templates/>
			</table>
		</dd>
		</xsl:for-each>
	</dl>
	<xsl:for-each select="mint:seriesList">
	<h2>Series List</h2>
	<ol>
	<xsl:for-each select="mint:series">
		<li>
			<dl>
				<dt>Series Instance UID</dt>
				<dd class='SeriesInstanceUID'><xsl:value-of select="./@seriesInstanceUID"/></dd>
				<dt>Instance Count</dt>
				<dd class='InstanceCount'><xsl:value-of select="./@instanceCount"/></dd>
				<xsl:for-each select="mint:attributes">
					<dt>Attributes</dt>
					<dd>
						<table border="1" cellpadding="2" cellspacing="0">
							<tr>
								<th>Attribute Tag</th>
								<th>VR</th>
								<th>Value</th>
								<th>Bid</th>
								<th>Frame count</th>
								<th>Bytes</th>
							</tr>
							<xsl:apply-templates/>
						</table>
					</dd>
				</xsl:for-each>
			</dl>
		</li>
	</xsl:for-each>
	</ol>
	</xsl:for-each>
</xsl:template>

<xsl:template match="mint:attr">
	<tr>
		<td class='AttributeTag'><xsl:value-of select="./@tag"/></td>
		<td class='vr'><xsl:value-of select="./@vr"/></td>
		<td class='Value'>
			<xsl:if test="./@val">
				<xsl:value-of select="./@val"/>
			</xsl:if>
		</td>
		<td class='Bid'>
			<xsl:if test="./@bid">
				<xsl:value-of select="./@bid"/>
			</xsl:if>
		</td>
		<td class='FrameCount'>
			<xsl:if test="./@framecount">
				<xsl:value-of select="./@framecount"/>
			</xsl:if>
		</td>
		<td class='Bytes'>
			<xsl:if test="./@bytes">
				<xsl:value-of select="./@bytes"/>
			</xsl:if>
		</td>
	</tr>
</xsl:template>

<xsl:template match="mint:changelog">
	<h1>Changelog</h1>
	<dl>
		<xsl:if test="./@studyUUID">
			<dt>MINT Study UUID</dt>
			<dd class='StudyUUID'><a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="./@studyUUID"/></xsl:attribute><xsl:value-of select="./@studyUUID"/></a></dd>
		</xsl:if>
		<dt>Changes</dt>
		<dd class='Changes'>
			<table border="1" cellpadding="2" cellspacing="0">
				<tr>
					<xsl:if test="not(./@studyUUID)">
						<th>Study UUID</th>
					</xsl:if>
					<th>Change Number</th>
					<th>Type</th>
					<th>Date Time</th>
					<th>Remote host</th>
					<th>Remote user</th>
					<th>Principal</th>
				</tr>
				<xsl:apply-templates/>
			</table>
		</dd>
	</dl>
</xsl:template>

<xsl:template match="mint:change">
	<tr>
		<xsl:if test="./@studyUUID">
			<td class='StudyUUID'><a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:value-of select="./@studyUUID"/></xsl:attribute><xsl:value-of select="./@studyUUID"/></a></td>
		</xsl:if>
		<td class='changeNumber'>
		    <a><xsl:attribute name ="href"><%=request.getContextPath()%>/studies/<xsl:if test="./@studyUUID"><xsl:value-of select="./@studyUUID"/>/</xsl:if><xsl:if test="not(./@studyUUID)"><xsl:value-of select="../@studyUUID"/>/</xsl:if>changelog/<xsl:value-of select="./@changeNumber"/></xsl:attribute><xsl:value-of select="./@changeNumber"/></a>
		</td>
		<td class='type'><xsl:value-of select="./@type"/></td>
		<td class='dateTime'><xsl:value-of select="./@dateTime"/></td>
		<td class='remoteHost'><xsl:value-of select="./@remoteHost"/></td>
		<td class='remoteUser'>
			<xsl:if test="./@remoteUser">
				<xsl:value-of select="./@remoteUser"/>
			</xsl:if>
		</td>
		<td class='principal'>
			<xsl:if test="./@principal">
				<xsl:value-of select="./@principal"/>
			</xsl:if>
		</td>
	</tr>
</xsl:template>

</xsl:stylesheet>
