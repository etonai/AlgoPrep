package com.algoprep.studylist;

import com.algoprep.problem.Problem;

import java.util.Optional;

/** One line of the list, with the problem it matched in the PROBLEMS directory, if any. */
public record StudyListRow(StudyListEntry entry, Optional<Problem> problem) {

    public boolean isFound() {
        return problem.isPresent();
    }
}
