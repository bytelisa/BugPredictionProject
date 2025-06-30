package org.example.controller;

import org.example.entity.JiraTicket;
import org.example.entity.Release;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProportionController {

    /** Class responsibility: apply Proportion method to estimate missing inject Version of a bug.
     *      Proportion method: IV = FV − (FV − OV) * p
     *      where p = (FV − IV) / (FV − OV), computed as an average on bugs with a known inject version.
     *
     * openingVersion (OV): la capiamo dalla data di crezione del ticket associandola alla release più vicina
     * injectedVersion (IV): JIRA non fornisce questo dato direttamente. Se un bug ha una sola affectedVersion, quella è la nostra IV "nota".
     * Se ne ha molte, la più vecchia è la nostra IV. Se non ne ha, l'IV è sconosciuta e va stimata
     *
     */


    private static final Logger LOGGER = Logger.getLogger(ProportionController.class.getName());


    public void applyProportion(List<JiraTicket> tickets, List<Release> allReleases) {

        LOGGER.log(Level.INFO, "Starting Proportion technique application...");

        // compute p from tickets that have a known IV
        double pValue = computeP(tickets);
        LOGGER.log(Level.INFO, "Calculated global proportion value (p): {0}", pValue);

        // estimate missing IV for the other tickets
        for (JiraTicket ticket : tickets) {

            if (ticket.getInjectVersion() == null) {

                //just to make sure they're actually sorted
                if (ticket.getFixVersions() != null) {
                    ticket.getFixVersions().sort(Comparator.comparing(Release::getDate));
                }

                Release estimatedIV = estimateInjectedVersion(ticket, pValue, allReleases);

                // estimate injecting version of the bug cannot come later than opening version of the ticket!
                if (estimatedIV != null && estimatedIV.getDate().isBefore(ticket.getOpeningVersion().getDate())) {
                    ticket.setInjectVersion(estimatedIV);

                } else {
                    LOGGER.log(Level.WARNING, "Could not estimate IV for ticket {0}. Defaulting IV to OV.", ticket.getName());
                    ticket.setInjectVersion(ticket.getOpeningVersion());
                }
            }
        }
        LOGGER.log(Level.INFO, "Finished estimating missing Injected Versions.");

        // populate the 'affectedVersions' list for every ticket based on the [IV, FV) range
        for (JiraTicket ticket : tickets) {
            determineAffectedVersions(ticket, allReleases);
        }
        LOGGER.log(Level.INFO, "Finished populating affected versions for all tickets.");
    }


    private double computeP(List<JiraTicket> tickets) {

        //calculates the proportion value 'p' by analyzing tickets where IV, OV, and FV are known

        List<Double> proportions = new ArrayList<>();

        for (JiraTicket ticket : tickets) {
            Release iv = ticket.getInjectVersion();
            Release ov = ticket.getOpeningVersion();

            // we need IV, OV, and at least one FV to calculate p.
            if (iv != null && ov != null && !ticket.getFixVersions().isEmpty()) {

                // for FV, we take the last fix version, assuming it's the final one (we sorted beforehand in applyProportion)
                Release fv = ticket.getFixVersions().getLast();

                Duration fvToOv = Duration.between(ov.getDate(), fv.getDate());
                Duration ivToFv = Duration.between(iv.getDate(), fv.getDate());

                if (!fvToOv.isZero() && !fvToOv.isNegative() && !ivToFv.isNegative()) {
                    double p = (double) ivToFv.getSeconds() / fvToOv.getSeconds();
                    proportions.add(p);
                }
            }
        }

        if (proportions.isEmpty()) {
            LOGGER.log(Level.WARNING, "Could not calculate any proportion value. Using default of 0.5.");
            return 0.5; // Default value if no data is available
        }

        // todo return the median value, which is more robust to outliers than the mean? or mean?
        Collections.sort(proportions);
        int middle = proportions.size() / 2;
        if (proportions.size() % 2 == 1) {
            return proportions.get(middle);
        } else {
            return (proportions.get(middle - 1) + proportions.get(middle)) / 2.0;
        }
    }


    private Release estimateInjectedVersion(JiraTicket ticket, double pValue, List<Release> allReleases) {

        /*
         * Estimates the injected version for a ticket using the Proportion method
         * uses the sorted list of all releases, needed to map the estimated date back to a Release.
         * returns the estimated injected Release, or null if it cannot be estimated.
         */

        Release ov = ticket.getOpeningVersion();

        if (ov == null || ticket.getFixVersions().isEmpty()) {
            return null; // Cannot estimate without OV and FV.
        }
        Release fv = ticket.getFixVersions().getLast();

        Duration fvToOv = Duration.between(ov.getDate(), fv.getDate());

        // Calculate the duration to subtract from FV's date.
        long secondsToSubtract = (long) (fvToOv.getSeconds() * pValue);

        // The estimated date of injection.
        Instant estimatedIVDate = fv.getDate().minusSeconds(secondsToSubtract);

        // Find the release that was active at the estimated injection date
        return ReleaseController.findReleaseByDate(estimatedIVDate, allReleases);
    }


    private void determineAffectedVersions(JiraTicket ticket, List<Release> releases) {

        // Populates the affected versions list for a ticket. A release is considered affected
        // if its date falls within the [IV, FV) interval.

        Release iv = ticket.getInjectVersion();

        if (iv == null || ticket.getFixVersions().isEmpty()) {
            return; // Cannot determine the range
        }

        Release fv = ticket.getFixVersions().getLast();

        Instant ivDate = iv.getDate();
        Instant fvDate = fv.getDate();

        List<Release> affected = new ArrayList<>();

        for (Release release : releases) {
            Instant releaseDate = release.getDate();

            // A release is affected if: releaseDate >= ivDate AND releaseDate < fvDate
            if (!releaseDate.isBefore(ivDate) && releaseDate.isBefore(fvDate)) {
                affected.add(release);
            }
        }

        // update internal list of affected versions with the one we just computed
        ticket.setAffectVersions(affected);
    }
}