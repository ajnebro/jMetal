package org.uma.jmetal.util.solutionattribute.impl;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.comparator.impl.OverallConstraintViolationComparator;
import org.uma.jmetal.util.solutionattribute.Ranking;
import ru.ifmo.nds.JensenFortinBuzdalov;
import ru.ifmo.nds.NonDominatedSorting;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an implementation of the {@link Ranking} interface using non-dominated sorting algorithms
 * from the <a href="https://github.com/mbuzdalov/non-dominated-sorting">non-dominated sorting repository</a>.
 *
 * It uses the solution's objectives directly, and not the dominance comparators,
 * as the structure of the efficient algorithms requires it to be this way.
 *
 * Additionally, if the {@link OverallConstraintViolation} property is set for at least one solution, and is less than
 * zero, it is used as a preliminary comparison key: all solutions are first sorted in the decreasing order of this
 * property, then non-dominated sorting is executed for each block of solutions with equal value of this property.
 *
 * @param <S> the exact type of a solution.
 * @author Maxim Buzdalov
 */
public class ExperimentalFastDominanceRanking<S extends Solution<?>>
        extends GenericSolutionAttribute<S, Integer> implements Ranking<S> {
    // Interface support: the place to store the fronts.
    private final List<List<S>> subFronts = new ArrayList<>();

    // Constraint violation checking support.
    private final OverallConstraintViolation<S> overallConstraintViolation = new OverallConstraintViolation<>();
    private final OverallConstraintViolationComparator<S> overallConstraintViolationComparator
            = new OverallConstraintViolationComparator<>();

    // Delegation.
    private NonDominatedSorting sortingInstance = null;

    @Override
    public Ranking<S> computeRanking(List<S> solutionList) {
        subFronts.clear();
        int nSolutions = solutionList.size();
        if (nSolutions == 0) {
            return this;
        }

        // We have at least one individual
        S first = solutionList.get(0);
        int nObjectives = first.getNumberOfObjectives();
        boolean hasConstraintViolation = getConstraint(first) < 0;

        // Iterate over all individuals to check if all have the same number of objectives,
        // and to get whether we have meaningful constraints
        for (int i = 1; i < nSolutions; ++i) {
            S current = solutionList.get(i);
            if (nObjectives != current.getNumberOfObjectives()) {
                throw new IllegalArgumentException("Solutions have different numbers of objectives");
            }
            hasConstraintViolation |= getConstraint(current) < 0;
        }

        if (!hasConstraintViolation) {
            // Running directly on the input, no further work is necessary
            runSorting(solutionList, 0, nSolutions, nObjectives, 0);
        } else {
            // Need to apply the constraint comparator first
            List<S> defensiveCopy = new ArrayList<>(solutionList);
            defensiveCopy.sort(overallConstraintViolationComparator);
            int rankOffset = 0;
            int lastSpanStart = 0;
            double lastConstraint = getConstraint(defensiveCopy.get(0));
            for (int i = 1; i < nSolutions; ++i) {
                double currConstraint = getConstraint(defensiveCopy.get(i));
                if (lastConstraint != currConstraint) {
                    lastConstraint = currConstraint;
                    rankOffset = 1 + runSorting(defensiveCopy, lastSpanStart, i, nObjectives, rankOffset);
                    lastSpanStart = i;
                }
            }
            runSorting(defensiveCopy, lastSpanStart, nSolutions, nObjectives, rankOffset);
        }

        return this;
    }

    private int runSorting(List<S> solutions, int from, int until, int dimension, int rankOffset) {
        ensureEnoughSpace(until - from, dimension);
        double[][] points = new double[until - from][];
        int[] ranks = new int[until - from];
        for (int i = from; i < until; ++i) {
            points[i - from] = solutions.get(i).getObjectives();
        }
        sortingInstance.sort(points, ranks, until - from);
        int maxRank = 0;
        for (int i = from; i < until; ++i) {
            S current = solutions.get(i);
            int rank = ranks[i - from] + rankOffset;
            maxRank = Math.max(maxRank, rank);
            setAttribute(current, rank);
            while (subFronts.size() <= rank) {
                subFronts.add(new ArrayList<>());
            }
            subFronts.get(rank).add(current);
        }
        return maxRank;
    }

    private void ensureEnoughSpace(int nPoints, int dimension) {
        if (sortingInstance == null
                || sortingInstance.getMaximumPoints() < nPoints
                || sortingInstance.getMaximumDimension() < dimension) {

            // This might be more intellectual.
            // For instance, for nPoints <= 10000 and dimension >= 7 one can instead use SetIntersectionSort aka MNDS.
            sortingInstance = JensenFortinBuzdalov
                    .getRedBlackTreeSweepHybridENSImplementation(1)
                    .getInstance(nPoints, dimension);
        }
    }

    private double getConstraint(S solution) {
        Double result = overallConstraintViolation.getAttribute(solution);
        return result == null ? 0 : result;
    }

    @Override
    public List<S> getSubfront(int rank) {
        return subFronts.get(rank);
    }

    @Override
    public int getNumberOfSubfronts() {
        return subFronts.size();
    }
}
