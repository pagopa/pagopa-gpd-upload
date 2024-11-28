package it.gov.pagopa.gpd.upload.entity;


import java.util.List;

public record MatchResult(List<String> matchingIUPD, List<String> nonMatchingIUPD) {
    // used by RecoveryService to store 2 list, match and non-match cases
}