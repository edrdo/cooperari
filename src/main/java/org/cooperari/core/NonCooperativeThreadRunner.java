package org.cooperari.core;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.cooperari.CInternalError;
import org.cooperari.feature.monitor.ResourceDeadlockError;

/**
 * Helper class to run a set of threads in non-cooperative manner.
 * 
 * Creating an object of this type will launch a set of threads 
 * and make the calling thread wait for their completion.
 * 
 * The launched threads run in daemon mode and all go
 * through an initialization barrier before executing their
 * code (no thread exits before all of them started).
 * 
 * @since 0.1
 */
public class NonCooperativeThreadRunner implements UncaughtExceptionHandler {

  /**
   * Array of threads.
   */
  private final Thread[] _threads;

  /**
   * Initialization barrier.
   */
  private final CyclicBarrier _barrier;

  /**
   * Uncaught exception. 
   * TODO: should be a list
   */
  private Throwable _uncaughtException;

  /**
   * Constructor. 
   * @param rv Runnable objects.
   */
  public NonCooperativeThreadRunner(Runnable[] rv) {
    _threads = new Thread[rv.length];
    _barrier = new CyclicBarrier(rv.length + 1);
    _uncaughtException = null;
    for (int i = 0; i < rv.length; i++) {
      Thread t = new NCThread(i, rv[i]);
      t.setUncaughtExceptionHandler(this);
      t.start();
      _threads[i] = t;
    }
    assert CWorkspace.debug("waiting for all threads to join");
    try {
      _barrier.await();
    } catch (InterruptedException | BrokenBarrierException e) {
      throw new CInternalError(e);
    }
    HashSet<Thread> set = new HashSet<>();
    for (Thread t : _threads) set.add(t);
    int ldCtr = 0;
    while(!set.isEmpty()) { 
      int aliveThreads = set.size();
      int waitingThreads = 0, blockedThreads = 0;
      Iterator<Thread> itr = set.iterator();
      while (itr.hasNext()) {
        Thread t = itr.next();
        if (!t.isAlive()) {
          --aliveThreads;
          itr.remove();
        } else if (t.getState() == Thread.State.WAITING) {
          waitingThreads ++;
        } else if (t.getState() == Thread.State.BLOCKED) {
          blockedThreads++;
        }
      } 
      // assert debug("A %d B %d W %d CTR %d", aliveThreads, blockedThreads, waitingThreads, ldCtr);
      if (aliveThreads > 0) { 
        if (blockedThreads + waitingThreads == aliveThreads) {
          ThreadMXBean bean = ManagementFactory.getThreadMXBean();
          long[] threadIds = bean.findDeadlockedThreads(); 
          if (threadIds != null) {
            throw new ResourceDeadlockError();
          }
          ldCtr++;
          if (ldCtr == 100) {
            assert CWorkspace.debug("dealock");
            throw new WaitDeadlockError();
          }
          try { Thread.sleep(1); } catch(Throwable e) { }
        } else {
          ldCtr = 0;
        }
      } 
    }
    if (_uncaughtException != null) {
      if (_uncaughtException instanceof Error)
        throw (Error) _uncaughtException;
      if (_uncaughtException instanceof RuntimeException)
        throw (RuntimeException) _uncaughtException;
      throw new CInternalError(_uncaughtException); // should not happen
    }
  }

  /**
   * Listener method for uncaught exceptions.
   * @param t Thread.
   * @param e Exception.
   */
  @Override
  public void uncaughtException(Thread t, Throwable e) {
    _uncaughtException = e;
    assert CWorkspace.debug(t, e);
  }

  /**
   * Inner class for threads that execute non-cooperatively.
   */
  private class NCThread extends Thread {
    /**
     * Runnable object.
     */
    private final Runnable _runnable;

    /**
     * Constructor. 
     * @param id Numerical thread identifier.
     * @param r Runnable to use in association to the thread.
     */
    public NCThread(int id, Runnable r) {
      super("Thread-"+ id);
      setDaemon(true);
      _runnable = r;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @SuppressWarnings("javadoc")
    @Override
    public void run() {
      try {
        // Initialization barrier.
        _barrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new CInternalError(e);
      }
      _runnable.run();
    }
  };
}
