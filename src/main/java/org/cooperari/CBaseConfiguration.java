package org.cooperari;

/**
 * Default configuration for test execution.
 * 
 * @since 0.2
 */
@CInstrument()
@CCoverage(CCoverageType.HDC)
@CMaxTrials(20)
@CRaceDetection(false)
@CDetectResourceDeadlocks()
@CAlways({}) 
@CNever({}) 
@CSometimes({})
@CTraceOptions()
public final class CBaseConfiguration {
  
  /**
   * Private constructor to prevent instantiation.
   */
  private CBaseConfiguration() { }
}
