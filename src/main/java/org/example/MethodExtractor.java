package org.example;

public class MethodExtractor {

    //class responsibility: extract data and write it onto csv files for it to be later analyzed by classifier

    //csv format: project name, method name, release ID, <features> (about 10), bugginess [yes/no]
    //bugs to be considered for the analysis: Jira tickets like [issuetype = Bug AND status in (Resolved, Closed) AND resolution = Fixed]
    //bugs to be excluded: bugs without an affect version (pre-release), bugs devoid of related fix commit on git

    //uses JavaParser Library to parse all methods in the desired project,
    // it then multiplies them by the releases in favor of a more in depth analysis

    //todo oppure meglio ck di javaparser? Mi dovrebbe semplificare il lavoro

    public static void extractMethods() {

    }

}
