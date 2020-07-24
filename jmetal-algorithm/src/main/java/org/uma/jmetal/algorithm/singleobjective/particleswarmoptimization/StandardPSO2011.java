package org.uma.jmetal.algorithm.singleobjective.particleswarmoptimization;

import org.uma.jmetal.algorithm.impl.AbstractParticleSwarmOptimization;
import org.uma.jmetal.operator.Operator;
import org.uma.jmetal.operator.selection.impl.BestSolutionSelection;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionUtils;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.neighborhood.impl.AdaptiveRandomNeighborhood;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.impl.ExtendedPseudoRandomGenerator;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;
import org.uma.jmetal.util.solutionattribute.impl.GenericSolutionAttribute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Class implementing a Standard PSO 2011 algorithm.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class StandardPSO2011 extends AbstractParticleSwarmOptimization<DoubleSolution, DoubleSolution> {
  private DoubleProblem problem;
  private SolutionListEvaluator<DoubleSolution> evaluator;

  private Operator<List<DoubleSolution>, DoubleSolution> findBestSolution;
  private Comparator<DoubleSolution> fitnessComparator;
  private int swarmSize;
  private int maxIterations;
  private int iterations;
  private int numberOfParticlesToInform;
  private DoubleSolution[] localBest;
  private DoubleSolution[] neighborhoodBest;
  private double[][] speed;
  private AdaptiveRandomNeighborhood<DoubleSolution> neighborhood;
  private GenericSolutionAttribute<DoubleSolution, Integer> positionInSwarm;
  private double weight;
  private double c;
  private JMetalRandom randomGenerator ;
  private DoubleSolution bestFoundParticle;
  private double changeVelocity;

  private int objectiveId;

  /**
   * Constructor
   *
   * @param problem
   * @param objectiveId This field indicates which objective, in the case of a multi-objective problem,
   *                    is selected to be optimized.
   * @param swarmSize
   * @param maxIterations
   * @param numberOfParticlesToInform
   * @param evaluator
   */
  public StandardPSO2011(DoubleProblem problem, int objectiveId, int swarmSize, int maxIterations,
                         int numberOfParticlesToInform, SolutionListEvaluator<DoubleSolution> evaluator) {
    this.problem = problem;
    this.swarmSize = swarmSize;
    this.maxIterations = maxIterations;
    this.numberOfParticlesToInform = numberOfParticlesToInform;
    this.evaluator = evaluator;
    this.objectiveId = objectiveId;

    weight = 1.0 / (2.0 * Math.log(2)); //0.721;
    c = 1.0 / 2.0 + Math.log(2); //1.193;
    changeVelocity = -0.5 ;

    fitnessComparator = new ObjectiveComparator<DoubleSolution>(objectiveId);
    findBestSolution = new BestSolutionSelection<DoubleSolution>(fitnessComparator);

    localBest = new DoubleSolution[swarmSize];
    neighborhoodBest = new DoubleSolution[swarmSize];
    speed = new double[swarmSize][problem.getNumberOfVariables()];

    positionInSwarm = new GenericSolutionAttribute<DoubleSolution, Integer>();

    randomGenerator = JMetalRandom.getInstance() ;
    randomGenerator.setRandomGenerator(new ExtendedPseudoRandomGenerator(new JavaRandomGenerator()));

    bestFoundParticle = null;
    neighborhood = new AdaptiveRandomNeighborhood<DoubleSolution>(swarmSize, this.numberOfParticlesToInform);
  }

  /**
   * Constructor
   *
   * @param problem
   * @param swarmSize
   * @param maxIterations
   * @param numberOfParticlesToInform
   * @param evaluator
   */
  public StandardPSO2011(DoubleProblem problem, int swarmSize, int maxIterations,
                         int numberOfParticlesToInform, SolutionListEvaluator<DoubleSolution> evaluator) {
    this(problem, 0, swarmSize, maxIterations, numberOfParticlesToInform, evaluator);
  }

  @Override
  public void initProgress() {
    iterations = 1;
  }

  @Override
  public void updateProgress() {
    iterations += 1;
  }

  @Override
  public boolean isStoppingConditionReached() {
    return iterations >= maxIterations;
  }

  @Override
  public List<DoubleSolution> createInitialSwarm() {
    List<DoubleSolution> swarm = new ArrayList<>(swarmSize);

    DoubleSolution newSolution;
    for (int i = 0; i < swarmSize; i++) {
      newSolution = problem.createSolution();
      positionInSwarm.setAttribute(newSolution, i);
      swarm.add(newSolution);
    }

    return swarm;
  }

  @Override
  public List<DoubleSolution> evaluateSwarm(List<DoubleSolution> swarm) {
    swarm = evaluator.evaluate(swarm, problem);

    return swarm;
  }

  @Override
  public void initializeLeader(List<DoubleSolution> swarm) {
    for (int i = 0; i < swarm.size(); i++) {
      neighborhoodBest[i] = getNeighborBest(i);
    }
  }

  @Override
  public void initializeParticlesMemory(List<DoubleSolution> swarm) {
    for (int i = 0; i < swarm.size(); i++) {
      localBest[i] = (DoubleSolution) swarm.get(i).copy();
    }
  }

  @Override
  public void initializeVelocity(List<DoubleSolution> swarm) {
    for (int i = 0; i < swarmSize; i++) {
      DoubleSolution particle = swarm.get(i);
      for (int j = 0; j < problem.getNumberOfVariables(); j++) {
        speed[i][j] = (randomGenerator.nextDouble(
                particle.getLowerBound(j) - particle.getVariable(0),
                particle.getUpperBound(j) - particle.getVariable(0)));
      }
    }
  }

  @Override
  public void updateVelocity(List<DoubleSolution> swarm) {
    double r1, r2;

    for (int i = 0; i < swarmSize; i++) {
      DoubleSolution particle = swarm.get(i);

      r1 = randomGenerator.nextDouble(0, c);
      r2 = randomGenerator.nextDouble(0, c);

      DoubleSolution gravityCenter = problem.createSolution();

      if (this.localBest[i] != this.neighborhoodBest[i]) {
        for (int var = 0; var < particle.getNumberOfVariables(); var++) {
          double G;
          G = particle.getVariable(var) +
                  c * (localBest[i].getVariable(var) +
                          neighborhoodBest[i].getVariable(var) - 2 *
                          particle.getVariable(var)) / 3.0;

          gravityCenter.setVariable(var, G);
        }
      } else {
        for (int var = 0; var < particle.getNumberOfVariables(); var++) {
          double g  = particle.getVariable(var) +
                  c * (localBest[i].getVariable(var) - particle.getVariable(var)) / 2.0;

          gravityCenter.setVariable(var, g);
        }
      }

      DoubleSolution randomParticle = problem.createSolution() ;

      double radius = 0;
      radius = SolutionUtils.distanceBetweenSolutionsInObjectiveSpace(gravityCenter, particle);

      double[] random = ((ExtendedPseudoRandomGenerator)randomGenerator.getRandomGenerator()).randSphere(problem.getNumberOfVariables());

      for (int var = 0; var < particle.getNumberOfVariables(); var++) {
        randomParticle.setVariable(var, gravityCenter.getVariable(var) + radius * random[var]);
      }

      for (int var = 0; var < particle.getNumberOfVariables(); var++) {
        speed[i][var] =
                weight * speed[i][var] + randomParticle.getVariable(var) - particle.getVariable(var);
      }


      if (localBest[i] != neighborhoodBest[i]) {
        for (int var = 0; var < particle.getNumberOfVariables(); var++) {
          speed[i][var] = weight * speed[i][var] +
                  r1 * (localBest[i].getVariable(var) - particle.getVariable(var)) +
                  r2 * (neighborhoodBest[i].getVariable(var) - particle.getVariable
                          (var));
        }
      } else {
        for (int var = 0; var < particle.getNumberOfVariables(); var++) {
          speed[i][var] = weight * speed[i][var] +
                  r1 * (localBest[i].getVariable(var) -
                          particle.getVariable(var));
        }
      }
    }
  }

  @Override
  public void updatePosition(List<DoubleSolution> swarm) {
    for (int i = 0; i < swarmSize; i++) {
      DoubleSolution particle = swarm.get(i);
      for (int var = 0; var < particle.getNumberOfVariables(); var++) {
        particle.setVariable(var, particle.getVariable(var) + speed[i][var]);

        if (particle.getVariable(var) < problem.getLowerBound(var)) {
          particle.setVariable(var, problem.getLowerBound(var));
          speed[i][var] = changeVelocity * speed[i][var];
        }
        if (particle.getVariable(var) > problem.getUpperBound(var)) {
          particle.setVariable(var, problem.getUpperBound(var));
          speed[i][var] = changeVelocity * speed[i][var];
        }
      }
    }
  }

  @Override
  public void perturbation(List<DoubleSolution> swarm) {
    /*
    MutationOperator<DoubleSolution> mutation =
            new PolynomialMutation(1.0/problem.getNumberOfVariables(), 20.0) ;
    for (DoubleSolution particle : swarm) {
      mutation.execute(particle) ;
    }
    */
  }

  @Override
  public void updateLeaders(List<DoubleSolution> swarm) {
    for (int i = 0; i < swarm.size(); i++) {
      neighborhoodBest[i] = getNeighborBest(i);
    }

    DoubleSolution bestSolution = findBestSolution.execute(swarm);

    if (bestFoundParticle == null) {
      bestFoundParticle = (DoubleSolution) bestSolution.copy();
    } else {
      if (bestSolution.objectives().get(objectiveId) == bestFoundParticle.objectives().get(0)) {
        neighborhood.recompute();
      }
      if (bestSolution.objectives().get(objectiveId) < bestFoundParticle.objectives().get(0)) {
        bestFoundParticle = (DoubleSolution) bestSolution.copy();
      }
    }
  }

  @Override
  public void updateParticlesMemory(List<DoubleSolution> swarm) {
    for (int i = 0; i < swarm.size(); i++) {
      if ((swarm.get(i).objectives().get(objectiveId) < localBest[i].objectives().get(0))) {
        localBest[i] = (DoubleSolution) swarm.get(i).copy();
      }
    }
  }

  @Override
  public DoubleSolution getResult() {
    return bestFoundParticle;
  }

  private DoubleSolution getNeighborBest(int i) {
    DoubleSolution bestLocalBestSolution = null;

    for (DoubleSolution solution : neighborhood.getNeighbors(getSwarm(), i)) {
      int solutionPositionInSwarm = positionInSwarm.getAttribute(solution);
      if ((bestLocalBestSolution == null) || (bestLocalBestSolution.objectives().get(0)
              > localBest[solutionPositionInSwarm].objectives().get(0))) {
        bestLocalBestSolution = localBest[solutionPositionInSwarm];
      }
    }

    return bestLocalBestSolution ;
  }

  /* Getters */
  public double[][]getSwarmSpeedMatrix() {
    return speed ;
  }

  public DoubleSolution[] getLocalBest() {
    return localBest ;
  }

  @Override public String getName() {
    return "SPSO11" ;
  }

  @Override public String getDescription() {
    return "Standard PSO 2011" ;
  }
}