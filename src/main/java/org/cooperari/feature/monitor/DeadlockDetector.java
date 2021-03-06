//
//   Copyright 2014-2019 Eduardo R. B. Marques
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package org.cooperari.feature.monitor;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;

import org.cooperari.config.CDetectResourceDeadlocks;
import org.cooperari.core.CRuntime;
import org.cooperari.core.CThread;
import org.cooperari.core.CTrace;
import org.cooperari.core.util.ResourceGraph;


/**
 * Monitor acquisition deadlock detector.
 * 
 * @since 0.2
 */
public class DeadlockDetector {

  /**
   * Resource graph.
   */
  private final ResourceGraph<Monitor> _graph = new ResourceGraph<>();

  /**
   * Lock chain per thread.
   */
  private final IdentityHashMap<CThread, LinkedList<Monitor>> _lockChain = new IdentityHashMap<>();

  /**
   * Constructs a new deadlock detector.
   * @param config Configuration options.
   */
  public DeadlockDetector(CDetectResourceDeadlocks config) {
    
  }

  /**
   * Signal monitor acquisition intent by given thread.
   * 
   * @param t The acquiring thread.
   * @param m The monitor.
   */
  public void onMonitorEnter(CThread t, Monitor m) {
    LinkedList<Monitor> chain = _lockChain.get(t);
    if (chain == null) {
      chain = new LinkedList<>();
      chain.add(m);
      _lockChain.put(t, chain);
    } else {
      Monitor from = chain.getLast();
      _graph.addEdge(from, m);
      List<Monitor> deadlock = _graph.findCycle(m);
      if (!deadlock.isEmpty()) {
        _graph.removeEdge(from, m);
        CResourceDeadlockError error = new CResourceDeadlockError(t, deadlock);
        for (Monitor m2 : deadlock) {
          CThread t2 = m2.getOwner();
          CRuntime.getRuntime().get(CTrace.class).record(t2, CTrace.EventType.DEADLOCK);
          if (t2 != t) {
            t2.cStop(error);
          }
        }
        throw error;
      } else {
        chain.addLast(m);
      }
    }
  }

  /**
   * Signal monitor release by a thread. The implicit monitor is the last one
   * acquired by the thread.
   * 
   * @param t The releasing thread.
   */
  public void onMonitorExit(CThread t) {
    LinkedList<Monitor> chain = _lockChain.get(t);

    Monitor m = chain.removeLast();
    if (!chain.isEmpty()) {
      _graph.removeEdge(chain.getLast(), m);
    } else {
      _lockChain.remove(t);
    }
  }
}
