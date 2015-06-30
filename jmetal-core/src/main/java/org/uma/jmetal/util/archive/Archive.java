package org.uma.jmetal.util.archive;

import org.uma.jmetal.solution.Solution;

import java.util.List;

/**
 * Created by Antonio J. Nebro on 24/09/14.
 */
public interface Archive<S extends Solution<?>> {
  public boolean add(S solution) ;
  public S get(int index) ;
  public List<S> getSolutionList() ;
  public int size() ;
}
