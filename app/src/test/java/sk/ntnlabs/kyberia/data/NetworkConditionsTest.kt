package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkConditionsTest {

    @Test
    fun offlinePhone() {
        assertEquals("bez siete, standby active", NetworkConditions(NetState.NONE, standbyBucket = 10).describe())
    }

    @Test
    fun androidBlocksTheApp() {
        assertEquals(
            "sieť zablokovaná pre appku, Doze, šetrič batérie, šetrič dát, obmedzená na pozadí, standby restricted",
            NetworkConditions(
                NetState.BLOCKED,
                deviceIdle = true,
                powerSave = true,
                dataSaver = true,
                backgroundRestricted = true,
                standbyBucket = 45,
            ).describe(),
        )
    }

    @Test
    fun unknownBucketShowsNumber() {
        assertEquals("sieť OK, standby 99", NetworkConditions(NetState.OK, standbyBucket = 99).describe())
        assertEquals("sieť bez internetu", NetworkConditions(NetState.NO_INTERNET).describe())
    }
}
