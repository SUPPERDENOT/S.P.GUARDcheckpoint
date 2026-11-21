package com.superdenot.powerguard.domain.engine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class ChargingEtaEstimatorTest {
 @Test fun requiresThreeSamples() {
  val e=ChargingEtaEstimator(); e.addSample(1000,1000,true,true,true); e.addSample(1000,2000,true,true,true)
  assertNull(e.estimateMinutes(50,5000,2000)); e.addSample(1000,3000,true,true,true)
  assertEquals(165L,e.estimateMinutes(50,5000,3000))
 }
 @Test fun taperAppliesOnlyToCvSegment() {
  val e=ChargingEtaEstimator(); repeat(3){i->e.addSample(1000,(i+1)*1000L,true,true,true)}
  assertEquals(75L,e.estimateMinutes(80,5000,3000)); assertEquals(165L,e.estimateMinutes(50,5000,3000))
  assertEquals(4L,e.estimateMinutes(99,5000,3000))
 }
 @Test fun timestampWeightedEmaFavorsRecentSamples() {
  val e=ChargingEtaEstimator(); e.addSample(1000,1000,true,true,true); e.addSample(1000,2000,true,true,true)
  e.addSample(3000,32000,true,true,true); assertEquals(35L,e.estimateMinutes(80,5000,32000))
 }
 @Test fun invalidOrNonPositiveSampleResetsWindow() {
  val e=ChargingEtaEstimator(); repeat(3){i->e.addSample(1000,(i+1)*1000L,true,true,true)}
  e.addSample(0,4000,true,true,true); assertNull(e.estimateMinutes(50,5000,4000)); assertEquals(0,e.sampleCount())
 }
 @Test fun unplugAndUnavailableCurrentResetWindow() {
  val e=ChargingEtaEstimator(); repeat(3){i->e.addSample(1000,(i+1)*1000L,true,true,true)}
  e.addSample(1000,4000,true,false,true); assertNull(e.estimateMinutes(50,5000,4000)); assertEquals(0,e.sampleCount())
 }
 @Test fun invalidCapacityStaleAndLowCurrentReturnNull() {
  val invalid=ChargingEtaEstimator(); repeat(3){i->invalid.addSample(1000,(i+1)*1000L,true,true,true)}
  assertNull(invalid.estimateMinutes(50,0,3000))
  val stale=ChargingEtaEstimator(); repeat(3){i->stale.addSample(1000,(i+1)*1000L,true,true,true)}
  assertNull(stale.estimateMinutes(50,5000,10000))
  val low=ChargingEtaEstimator(); repeat(3){i->low.addSample(50,(i+1)*1000L,true,true,true)}
  assertNull(low.estimateMinutes(50,5000,3000))
 }
}
