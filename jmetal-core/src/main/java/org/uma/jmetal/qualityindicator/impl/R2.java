//  R2.java
//
//  Author:
//       Juan J. Durillo <juanjo.durillo@gmail.com>
//
//  Copyright (c) 2013 Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.uma.jmetal.qualityindicator.impl;

import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.front.Front;
import org.uma.jmetal.util.front.imp.ArrayFront;
import org.uma.jmetal.util.front.util.FrontNormalizer;
import org.uma.jmetal.util.front.util.FrontUtils;
import org.uma.jmetal.util.naming.impl.SimpleDescribedEntity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * TODO: Add comments here
 */
@SuppressWarnings("serial")
public class R2<Evaluate extends List<? extends Solution<?>>>
    extends SimpleDescribedEntity
    implements QualityIndicator<Evaluate,Double> {
    private double[][] lambda = null;
  

  private Front referenceParetoFront = null;



  /**
   * Creates a new instance of the R2 indicator for a problem with
   * two objectives and 100 lambda vectors
   */
  public R2(Front referenceParetoFront) {
    // by default it creates an R2 indicator for a two dimensions problem and
    // uses only 100 weight vectors for the R2 computation
    this(100, referenceParetoFront);
  }


  /**
   * Creates a new instance of the R2 indicator for a problem with
   * two objectives and 100 lambda vectors
   */
  public R2() {
    // by default it creates an R2 indicator for a two dimensions problem and
    // uses only 100 weight vectors for the R2 computation
    this(100);
  }


  /**
   * Creates a new instance of the R2 indicator for a problem with
   * two objectives and N lambda vectors
   */
  public R2(int nVectors, Front referenceParetoFront)  {
    this(nVectors);
    this.referenceParetoFront = referenceParetoFront;
  }

  /**
   * Creates a new instance of the R2 indicator for a problem with
   * two objectives and N lambda vectors
   */
  public R2(int nVectors)  {
    // by default it creates an R2 indicator for a two dimensions problem and
    // uses only <code>nVectors</code> weight vectors for the R2 computation
    super("R2", "R2 quality indicator") ;
    // generating the weights
    lambda = new double[nVectors][2];
    for (int n = 0; n < nVectors; n++) {
      double a = 1.0 * n / (nVectors - 1);
      lambda[n][0] = a;
      lambda[n][1] = 1 - a;
    }
  }

    /**
     * Constructor
     * Creates a new instance of the R2 indicator for nDimensiosn
     * It loads the weight vectors from the file fileName
     */
    public R2(String file, Front referenceParetoFront) throws java.io.IOException {
        this(file);
        this.referenceParetoFront = referenceParetoFront;
    } // R2


    /**
   * Constructor
   * Creates a new instance of the R2 indicator for nDimensiosn
   * It loads the weight vectors from the file fileName
   */
  public R2(String file) throws java.io.IOException {
        // reading weights from file
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);

        int numberOfObjectives = 0;
        int i = 0;
        int j = 0;
        String aux = br.readLine();
        LinkedList<double[]> list = new LinkedList<double[]>();
        while (aux != null) {
            StringTokenizer st = new StringTokenizer(aux);
            j = 0;
            numberOfObjectives = st.countTokens();
            double[] vector = new double[numberOfObjectives];
            while (st.hasMoreTokens()) {
                double value = new Double(st.nextToken());
                vector[j++] = value;
            }
            list.add(vector);
            aux = br.readLine();
        }
        br.close();

        // convert the LinkedList into a vector
        lambda = new double[list.size()][];
        int index = 0;
        for (double[] aList : list) {
            lambda[index++] = aList;
        }

        br.close();

    } // R2


  @Override public Double evaluate(Evaluate solutionList) {
    return r2(new ArrayFront(solutionList));
  }

  @Override public String getName() {
    return super.getName();
  }

  public double r2(Front front) {
    if (this.referenceParetoFront != null) {
       // STEP 1. Obtain the maximum and minimum values of the Pareto front
       double[] maximumValues = FrontUtils.getMaximumValues(this.referenceParetoFront);
       double[] minimumValues = FrontUtils.getMinimumValues(this.referenceParetoFront);

       // STEP 2. Get the normalized front
       FrontNormalizer frontNormalizer = new FrontNormalizer(minimumValues, maximumValues);
       front = frontNormalizer.normalize(front);
    }

    int numberOfObjectives = front.getPoint(0).getNumberOfDimensions();

    // STEP 3. compute all the matrix of Tschebyscheff values if it is null
    double[][] matrix = new double[front.getNumberOfPoints()][lambda.length];
    for (int i = 0; i < front.getNumberOfPoints(); i++) {
      for (int j = 0; j < lambda.length; j++) {
        matrix[i][j] = lambda[j][0] * Math.abs(front.getPoint(i).getDimensionValue(0));
        for (int n = 1; n < numberOfObjectives; n++) {
          matrix[i][j] = Math.max(matrix[i][j],
          lambda[j][n] * Math.abs(front.getPoint(i).getDimensionValue(n)));
        }
      }
    }

    double sum = 0.0;
    for (int i = 0; i < lambda.length; i++) {
      double tmp = matrix[0][i];
      for (int j = 1; j < front.getNumberOfPoints(); j++) {
        tmp = Math.min(tmp, matrix[j][i]);
      }
      sum += tmp;
    }
    return sum / (double) lambda.length;
  }
}
