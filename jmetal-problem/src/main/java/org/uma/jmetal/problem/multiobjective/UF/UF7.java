package org.uma.jmetal.problem.multiobjective.UF;

import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing problem CEC2009_UF7
 */
@SuppressWarnings("serial")
public class UF7 extends AbstractDoubleProblem {
    
 /** 
  * Constructor.
  * Creates a default instance of problem CEC2009_UF7 (30 decision variables)
  */
  public UF7() {
    this(30);
  }
  
 /**
  * Creates a new instance of problem CEC2009_UF7.
  * @param numberOfVariables Number of variables.
  */
  public UF7(int numberOfVariables) {
    setNumberOfVariables(numberOfVariables) ;
    setNumberOfObjectives(2) ;
    setNumberOfConstraints(0) ;
    setName("UF7") ;

    List<Double> lowerLimit = new ArrayList<>(getNumberOfVariables()) ;
    List<Double> upperLimit = new ArrayList<>(getNumberOfVariables()) ;

    lowerLimit.add(0.0);
    upperLimit.add(1.0);
    for (int i = 1; i < getNumberOfVariables(); i++) {
      lowerLimit.add(-1.0);
      upperLimit.add(1.0);
    }

    setVariableBounds(lowerLimit, upperLimit);
  }

  /** Evaluate() method */
  @Override
  public void evaluate(DoubleSolution solution) {
    double[] x = new double[getNumberOfVariables()];
    for (int i = 0; i < solution.getNumberOfVariables(); i++) {
      x[i] = solution.getVariable(i) ;
    }


  	int count1, count2;
		double sum1, sum2, yj;
		sum1   = sum2   = 0.0;
		count1 = count2 = 0;
    
    for (int j = 2 ; j <= getNumberOfVariables(); j++) {
			yj = x[j-1] - Math.sin(6.0*Math.PI*x[0]+j*Math.PI/getNumberOfVariables());
			if (j % 2 == 0) {
				sum2  += yj*yj;
				count2++;
			} else {
				sum1  += yj*yj;
				count1++;
			}
    }
    yj = Math.pow(x[0],0.2);
    
    solution.objectives().set(0, yj + 2.0*sum1 / (double)count1);
    solution.objectives().set(1, 1.0 - yj + 2.0*sum2 / (double)count2);
  }
}
