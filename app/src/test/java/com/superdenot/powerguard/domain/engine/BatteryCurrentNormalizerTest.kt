package com.superdenot.powerguard.domain.engine
import org.junit.Assert.assertEquals
import org.junit.Test
class BatteryCurrentNormalizerTest {
    @Test fun preservesDefaultScaleAndDirection() {
        assertEquals(1800,BatteryCurrentNormalizer.normalize(1800000,true));assertEquals(-1800,BatteryCurrentNormalizer.normalize(-1800000,true))
        assertEquals(-1800,BatteryCurrentNormalizer.normalize(1800000,false));assertEquals(-1800,BatteryCurrentNormalizer.normalize(-1800000,false))
    }
    @Test fun zeroAndUnavailableSentinelRemainSafe() {
        assertEquals(0,BatteryCurrentNormalizer.normalize(0,true));assertEquals(0,BatteryCurrentNormalizer.normalize(Int.MIN_VALUE,true));assertEquals(0,BatteryCurrentNormalizer.normalize(Int.MIN_VALUE,false))
    }
    @Test fun thousandScaleCorrectsRawMilliampReportedAsMicroamps() {
        assertEquals(1800,BatteryCurrentNormalizer.normalize(1800,true,1000.0));assertEquals(-1800,BatteryCurrentNormalizer.normalize(-1800,true,1000.0))
    }
    @Test fun oneThousandthScaleCorrectsRawValueOverstatedByOneThousand() {
        assertEquals(1800,BatteryCurrentNormalizer.normalize(1800000000,true,0.001))
    }
    @Test fun invalidScaleFallsBackAndExtremeValuesSaturate() {
        assertEquals(1800,BatteryCurrentNormalizer.normalize(1800000,true,Double.NaN));assertEquals(1800,BatteryCurrentNormalizer.normalize(1800000,true,-1.0))
        assertEquals(Int.MAX_VALUE,BatteryCurrentNormalizer.normalize(Int.MAX_VALUE,true,1000.0))
    }
    @Test fun derivedPowerUsesCalibratedCurrentAndDoubleIntermediate() {
        val ma=BatteryCurrentNormalizer.normalize(1800,true,1000.0);assertEquals(7.2f,BatteryCurrentNormalizer.batterySidePowerWatt(4000,ma),0.0001f)
    }
}
