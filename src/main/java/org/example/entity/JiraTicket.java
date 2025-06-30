package org.example.entity;

import java.util.List;

public class JiraTicket {


    private final String issueId;
    private final String name;
    private final String resolution;
    private final String description; // Rinominato da 'comment' per chiarezza

    private final Release openingVersion;
    private Release injectVersion; // 'injectedVersion' è un nome più comune
    private final List<Release> fixVersions;
    private List<Release> affectedVersions;


    public JiraTicket(String issueId, String name, String resolution, String description,
                      Release openingVersion, Release injectVersion, List<Release> fixVersions, List<Release> affectedVersions) {
        this.issueId = issueId;
        this.name = name;
        this.resolution = resolution;
        this.description = description;
        this.openingVersion = openingVersion;
        this.injectVersion = injectVersion;
        this.fixVersions = fixVersions;
        this.affectedVersions = affectedVersions;
    }


    public String getIssueId() { return issueId; }
    public String getName() { return name; }
    public String getResolution() { return resolution; }
    public String getDescription() { return description; }
    public Release getOpeningVersion() { return openingVersion; }
    public Release getInjectVersion() { return injectVersion; }
    public List<Release> getFixVersions() { return fixVersions; }
    public List<Release> getAffectedVersions() { return affectedVersions; }

    public void setInjectVersion(Release injectVersion) {
        this.injectVersion = injectVersion;
    }

    public void setAffectVersions(List<Release> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }
}