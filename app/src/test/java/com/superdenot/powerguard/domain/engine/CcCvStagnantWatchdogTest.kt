package com.superdenot.powerguard.domain.engine
import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class CcCvStagnantWatchdogTest {
 private fun state(level:Int,current:Int=100,charging:Boolean=true)=LiveBatteryState(level=level,currentMa=current,isCharging=charging,isPowerConnected=true,isCurrentAvailable=true,isLevelAvailable=true)
 @Test fun ccThresholdIsTenMinutesBelow150mA() {
  val w=CcCvStagnantWatchdog(); assertNull(w.evaluate(state(55),0L)); assertNull(w.evaluate(state(55),599999L))
  assertEquals(10,w.evaluate(state(55),600000L)); val m=CcCvStagnantWatchdog()
  assertNull(m.evaluate(state(55,200),0L)); assertNull(m.evaluate(state(55,200),900000L))
 }
 @Test fun cvThresholdIsTwentyMinutesBelow120mA() {
  val w=CcCvStagnantWatchdog(); assertNull(w.evaluate(state(85),0L)); assertNull(w.evaluate(state(85),1199999L))
  assertEquals(20,w.evaluate(state(85),1200000L)); val m=CcCvStagnantWatchdog()
  assertNull(m.evaluate(state(85,125),0L)); assertNull(m.evaluate(state(85,125),1500000L))
 }
 @Test fun socGainRestartsWindow() {
  val w=CcCvStagnantWatchdog(); w.evaluate(state(70),0L); assertNull(w.evaluate(state(71),590000L))
  assertNull(w.evaluate(state(71),1189999L)); assertEquals(10,w.evaluate(state(71),1190000L))
 }
 @Test fun unplugInvalidSensorsAndNonChargingReset() {
  val w=CcCvStagnantWatchdog(); w.evaluate(state(50),0L)
  assertNull(w.evaluate(state(50).copy(isPowerConnected=false),600000L)); assertNull(w.evaluate(state(50),600001L))
  assertNull(w.evaluate(state(50).copy(isCurrentAvailable=false),700000L)); assertNull(w.evaluate(state(50),700001L))
  assertNull(w.evaluate(state(50,charging=false),800000L)); assertNull(w.evaluate(state(50),800001L))
 }
 @Test fun sustained300mARecoveryResetsWindow() {
  val w=CcCvStagnantWatchdog(); w.evaluate(state(50),0L); assertNull(w.evaluate(state(50,350),590000L))
  assertNull(w.evaluate(state(50,350),620000L)); assertNull(w.evaluate(state(50),1219999L))
  assertEquals(10,w.evaluate(state(50),1819999L))
 }
 @Test fun fullBatteryIsNotStagnant() {
  val w=CcCvStagnantWatchdog(); assertNull(w.evaluate(state(100),0L)); assertNull(w.evaluate(state(100),2000000L))
 }
}
