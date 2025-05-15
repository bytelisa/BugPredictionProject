package org.example.entity;

import java.util.List;

public class JiraTicket {


    /* bugs to be considered for the analysis:
            *       Jira tickets like [issuetype = Bug AND status in (Resolved, Closed) AND resolution = Fixed]
            * bugs to be excluded: bugs without an affect version (pre-release), bugs devoid of related fix commit on git
    */

    private String issueId;
    private String description;
    private String name;
    private boolean archived;
    private boolean released;
    private String releaseDate;

    private String status;  //resolved or closed
    private String resolution;  //fixed
    private String creationDate;
    private String resolutionDate;
    private String fixVersions;
    private List<String> affectVersions;

    public JiraTicket(){}

    public JiraTicket(String id, String name,
                      String resolution, String creationDate, String resolutionDate, List<String> affectV, String comment){
        this.issueId = id;
        this.name = name;
        this.resolution = resolution;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        this.description = comment;
        this.affectVersions = affectV;


    }
    public JiraTicket(String id, String description, String status, String resolution,
                      boolean released, String releaseDate){
        this.issueId = id;
        this.status = status;
        this.resolution = resolution;
        this.description = description;
        this.released = released;
        this.releaseDate = releaseDate;

    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(String resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public List<String> getAffectVersions() {
        return affectVersions;
    }

    public void setAffectVersions(List<String> affectVersions) {
        this.affectVersions = affectVersions;
    }

    public String getFixVersions() {
        return fixVersions;
    }

    public void setFixVersions(String fixVersions) {
        this.fixVersions = fixVersions;
    }

}
