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

package org.cooperari.junit;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import org.cooperari.CSystem;
import org.cooperari.CTest;
import org.cooperari.CTestResult;
import org.cooperari.core.CSession;
import org.cooperari.core.CWorkspace;
import org.cooperari.errors.CCheckedExceptionError;
import org.cooperari.errors.CConfigurationError;
import org.cooperari.errors.CInternalError;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;

/**
 * JUnit cooperative test runner.
 * 
 * <p>
 * JUnit test classes must be parameterized with this test runner for cooperative test execution.
 * The usual {@link org.junit.runner.RunWith} annotation should be used to set this runner for test classes,
 * as usual. Here is a simple example:</p>
 * <pre>
 * import org.junit.runner.RunWith;
 * import org.cooperari.CJUnitRunner;
 * ...
 * &#064;RunWith(CJUnitRunner.class)
 * public class MyTestClass {
 *   ...
 * }
 * </pre>
 * 
 * @see CPreemptiveOnly
 * @since 0.2
 */
public final class CJUnitRunner extends BlockJUnit4ClassRunner {

  /**
   * Auxiliary object for interface with JUnit.
   */
  private static final RunnerScheduler JRUNNER_SCHEDULER =
      new RunnerScheduler() {

    @Override
    public void schedule(Runnable childStatement) {
      childStatement.run();
    }

    @Override
    public void finished() {

    }
  };

  /**
   * Fall-back workspace dir.
   */
  private static final String FALLBACK_WORSPACE_DIR = "cooperari-test-data";

  /**
   * Constructs a new runner.
   * @param testClass Test class.
   * @throws InitializationError if an error occurs during initialization
   */
  public CJUnitRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
    super.setScheduler(JRUNNER_SCHEDULER);
    try {
      CWorkspace.INSTANCE.initializeIfNecessary(new File(FALLBACK_WORSPACE_DIR));
    } catch (IOException e) {
      throw new CInternalError(e);
    }
  }

  /**
   * Callback method to execute a test method.
   * @param fm Method to run.
   * @param notifier Notifier instance.
   */
  @Override
  protected void runChild(FrameworkMethod fm, RunNotifier notifier) {
    Description desc = describeChild(fm);
    if (fm.getAnnotation(Ignore.class) != null) {
      // @Ignore annotation for method
      notifier.fireTestIgnored(desc); 
      return;
    }

    if (fm.getAnnotation(CPreemptiveOnly.class) != null) {
      // Run test only once using the standard runner and preemptive semantics
      super.runChild(fm, notifier);
      return;
    }
    
    if (!CSystem.inCooperativeMode() && fm.getAnnotation(CCooperativeOnly.class) != null) {
      // @Ignore annotation for method
      notifier.fireTestIgnored(desc); 
      return;
    }

    notifier.fireTestStarted(desc);

    if (fm.getAnnotation(Test.class).timeout() > 0) {
      // timeout attribute not honored for now
      notifier.fireTestFailure(new Failure(desc, new CConfigurationError("'timeout' attribute is not honored for @Test. Sorry :(")));
      return;
    }

    CTest ctest = new MethodRunner(fm, createJUnitStatement(fm, false)); 
    CTestResult result = CSession.executeTest(ctest);
    CTestResultPool.INSTANCE.setTestResult(desc, result);
    if (result.failed()) {
      notifier.fireTestFailure(new Failure(desc, result.getFailure()));
    } else {
      notifier.fireTestFinished(desc);
    }

  }

  /**
   * Create JUnit statement to run a test.
   * @param fm Method.
   * @param handleExpected If <code>true</code>, let JUnit handle {@link Test#expected()}.
   * @return JUnit statement object
   */
  private Statement createJUnitStatement(FrameworkMethod fm, boolean handleExpected) {
    try {
      Object test = super.createTest();
      Statement statement = super.methodInvoker(fm, test);
      if (handleExpected) {
        statement = super.possiblyExpectingExceptions(fm, test, statement);
      }
      statement = super.withBefores(fm, test, statement);
      statement = super.withAfters(fm, test, statement);
      return statement;
    } catch(Throwable e) {
      throw new CInternalError("Error defining JUnit statement", e);
    }
  }

  /**
   * Method runner thread.
   * @since 0.2 
   */
  private final class MethodRunner extends Thread implements CTest {
    /** 
     * JUnit statement 
     */
    private final Statement _statement;

    /**
     * Method.
     */
    private final Method _method;

    /**
     * Expected exception.
     */
    private Class<?> _expectedException;

    /**
     * Constructs a new method runner.
     * @param fm Method handle.
     * @param statement JUnit statement to run the method.
     */
    public MethodRunner(FrameworkMethod fm, Statement statement) {
      setName(fm.getName());
      _statement = statement;
      _method = fm.getMethod();
      _expectedException = fm.getAnnotation(Test.class).expected();
    }

    @Override
    public String getSuiteName() {
      return CJUnitRunner.this.getTestClass().getJavaClass().getCanonicalName();
    }

    @Override
    public AnnotatedElement getConfiguration() {
      return _method;
    }

    @Override
    public void onNormalCompletion() throws Throwable {
      if (_expectedException != Test.None.class) {
        throw new AssertionError("Expected exception " + _expectedException.getCanonicalName());
      }
    }
    
    @Override
    public boolean ignoreException(Throwable e)  {
      return e.getClass() == _expectedException;
    }

    @Override
    public void run () {
      try {
        _statement.evaluate();
      } catch(RuntimeException|Error e) {
        throw e;
      } catch(Throwable e) {
        throw new CCheckedExceptionError(e);
      }
    }

  }

}