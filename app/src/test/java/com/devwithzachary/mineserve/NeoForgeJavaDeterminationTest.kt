package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.determineJavaVersion
import org.junit.Assert.assertEquals
import org.junit.Test

class NeoForgeJavaDeterminationTest {
    @Test
    fun testNeoForgeVersions() {
        assertEquals(21, determineJavaVersion("21.1.265", ServerType.NEOFORGE))
        assertEquals(21, determineJavaVersion("21.2.1-beta", ServerType.NEOFORGE))
        assertEquals(21, determineJavaVersion("21.6.20-beta", ServerType.NEOFORGE))
        assertEquals(17, determineJavaVersion("20.4.80", ServerType.NEOFORGE))
        assertEquals(17, determineJavaVersion("47.1.0", ServerType.NEOFORGE))
        assertEquals(25, determineJavaVersion("26.1.0", ServerType.NEOFORGE))
    }
}
