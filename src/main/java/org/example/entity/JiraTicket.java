package org.example.entity;

import java.util.List;

public class JiraTicket {

    // MODIFICA: Rimossi tutti i campi e costruttori duplicati o non utilizzati.
    // SPIEGAZIONE: Manteniamo un solo set di campi per le versioni, usando oggetti Release per coerenza.
    // I campi stringa ridondanti sono stati eliminati per evitare confusione.
    private String issueId;
    private String name;
    private String resolution;
    private String description; // Rinominato da 'comment' per chiarezza

    // Campi chiave per l'analisi
    private Release openingVersion;
    private Release injectVersion; // 'injectedVersion' è un nome più comune
    private List<Release> fixVersions;
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
        //for estimation via proportion
        this.injectVersion = injectVersion;
    }
}