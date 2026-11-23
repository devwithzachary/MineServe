package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.engine.AptInstallProgressTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootfsSetupProgressTest {

    @Test
    fun aptInstallProgressTrackerAdvancesMonotonicallyAcrossAllPhases() {
        val tracker = AptInstallProgressTracker(startPercent = 25, maxPercent = 99)

        val simulatedLines = listOf(
            "Hit:1 http://ports.ubuntu.com/ubuntu-ports noble InRelease",
            "Get:2 http://ports.ubuntu.com/ubuntu-ports noble-updates InRelease [126 kB]",
            "Get:3 http://ports.ubuntu.com/ubuntu-ports noble-security InRelease [126 kB]",
            "Fetched 252 kB in 1s (210 kB/s)",
            "Reading package lists...",
            "Building dependency tree...",
            "Reading state information...",
            "0 upgraded, 4 newly installed, 0 to remove and 0 not upgraded.",
            "Need to get 48.2 MB of archives.",
            "Get:1 http://ports.ubuntu.com/ubuntu-ports noble/main arm64 ca-certificates-java all 20240118 [11.6 kB]",
            "Get:2 http://ports.ubuntu.com/ubuntu-ports noble/main arm64 curl arm64 8.5.0 [220 kB]",
            "Get:3 http://ports.ubuntu.com/ubuntu-ports noble/main arm64 wget arm64 1.21.4 [310 kB]",
            "Get:4 http://ports.ubuntu.com/ubuntu-ports noble-updates/main arm64 openjdk-21-jre-headless arm64 21.0.5 [45.0 MB]",
            "Fetched 48.2 MB in 6s (8,033 kB/s)",
            "Selecting previously unselected package ca-certificates-java.",
            "Preparing to unpack .../ca-certificates-java_20240118_all.deb ...",
            "Unpacking ca-certificates-java (20240118) ...",
            "Unpacking curl (8.5.0) ...",
            "Unpacking wget (1.21.4) ...",
            "Unpacking openjdk-21-jre-headless:arm64 (21.0.5) ...",
            "Setting up ca-certificates-java (20240118) ...",
            "Setting up curl (8.5.0) ...",
            "Setting up wget (1.21.4) ...",
            "Setting up openjdk-21-jre-headless:arm64 (21.0.5) ...",
            "Processing triggers for libc-bin (2.39-0ubuntu8) ...",
            "Processing triggers for ca-certificates-java (20240118) ...",
            "Updating certificates in /etc/ssl/certs...",
            "done."
        )

        var lastPercent = 25
        val milestones = mutableListOf<Int>()

        for (line in simulatedLines) {
            val snapshot = tracker.onLine(line)
            assertTrue(
                "Progress moved backward on '$line': ${snapshot.percent} < $lastPercent",
                snapshot.percent >= lastPercent
            )
            assertTrue("Progress exceeded max on '$line'", snapshot.percent <= 99)
            lastPercent = snapshot.percent
            milestones.add(snapshot.percent)
        }

        // Verify distinct weighted ranges were traversed smoothly
        assertTrue("Expected final progress to reach 99%, was $lastPercent", lastPercent == 99)
        assertTrue("Expected progress milestones in 35..52 range", milestones.any { it in 38..52 })
        assertTrue("Expected progress milestones in 53..72 range", milestones.any { it in 53..72 })
        assertTrue("Expected progress milestones in 73..95 range", milestones.any { it in 73..95 })
        assertEquals(99, milestones.last())
    }
}
