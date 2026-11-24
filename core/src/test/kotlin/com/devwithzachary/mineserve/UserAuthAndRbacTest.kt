package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.auth.PasswordHasher
import com.devwithzachary.mineserve.auth.Permission
import com.devwithzachary.mineserve.auth.UserRepository
import com.devwithzachary.mineserve.auth.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createTempDirectory

class UserAuthAndRbacTest {

    @Test
    fun passwordHasherProducesUniqueSaltsAndVerifiesCorrectly() {
        val h1 = PasswordHasher.hash("SuperSecret123", iterations = 10_000)
        val h2 = PasswordHasher.hash("SuperSecret123", iterations = 10_000)
        assertTrue(h1 != h2)
        assertTrue(PasswordHasher.verify("SuperSecret123", h1))
        assertTrue(PasswordHasher.verify("SuperSecret123", h2))
        assertFalse(PasswordHasher.verify("WrongPassword", h1))
        assertFalse(PasswordHasher.verify("SuperSecret123", "malformed"))
    }

    @Test
    fun rbacPermissionsAndServerScopingWorkCorrectly() {
        val dir = createTempDirectory("mineserve-rbac-test").toFile()
        try {
            val repo = UserRepository(dir)
            val owner = repo.ensureDefaultOwner("admin", "ownerpass1")
            assertEquals(UserRole.OWNER, owner.role)
            assertNotNull(repo.authenticate("admin", "ownerpass1"))
            assertNull(repo.authenticate("admin", "badpass"))

            // Cannot delete or demote last owner
            assertTrue(repo.deleteUser(owner.id).isFailure)
            assertTrue(repo.updateUser(owner.id, role = UserRole.ADMIN).isFailure)
            assertTrue(repo.updateUser(owner.id, enabled = false).isFailure)

            // Create scoped Moderator
            val mod = repo.createUser(
                username = "alex_mod",
                password = "modpassword",
                role = UserRole.MODERATOR,
                restrictServers = true,
                allowedServerIds = listOf("srv-smp")
            ).getOrThrow()

            assertTrue(repo.hasPermission(mod, Permission.SERVER_CONTROL, "srv-smp"))
            assertTrue(repo.hasPermission(mod, Permission.SERVER_CONSOLE_WRITE, "srv-smp"))
            assertFalse(repo.hasPermission(mod, Permission.SERVER_CONTROL, "srv-proxy"))
            assertFalse(repo.hasPermission(mod, Permission.SERVER_FILES_WRITE, "srv-smp"))
            assertFalse(repo.hasPermission(mod, Permission.MANAGE_USERS))

            // Create Viewer
            val viewer = repo.createUser(
                username = "sam_view",
                password = "viewpassword",
                role = UserRole.VIEWER,
                restrictServers = false
            ).getOrThrow()

            assertTrue(repo.hasPermission(viewer, Permission.SERVER_VIEW, "srv-smp"))
            assertFalse(repo.hasPermission(viewer, Permission.SERVER_CONTROL, "srv-smp"))
            assertFalse(repo.hasPermission(viewer, Permission.SERVER_CONSOLE_WRITE, "srv-smp"))

            // Password change & persistence across reload
            assertTrue(repo.changeOwnPassword(viewer.id, "wrong", "newpass123").isFailure)
            assertTrue(repo.changeOwnPassword(viewer.id, "viewpassword", "newpass123").isSuccess)
            val reloaded = UserRepository(dir)
            assertNotNull(reloaded.authenticate("sam_view", "newpass123"))

            // Audit log records and persists
            repo.recordAudit(mod, "SERVER_START", "srv-smp", "Started server", "192.168.1.50")
            val auditEntries = UserRepository(dir).getAuditLog(10)
            assertEquals(1, auditEntries.size)
            assertEquals("SERVER_START", auditEntries.first().action)
            assertEquals("alex_mod", auditEntries.first().username)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun userRepositoryReloadsExternalPanelUsersChangesFromDisk() {
        val dir = createTempDirectory("mineserve-reload-test").toFile()
        try {
            val runningDaemonRepo = UserRepository(dir)
            runningDaemonRepo.ensureDefaultOwner("admin", "initialPass1")
            assertNotNull(runningDaemonRepo.authenticate("admin", "initialPass1"))

            // Simulate another process (e.g. `mineserve --reset-admin`) updating panel_users.json
            Thread.sleep(20)
            val cliRepo = UserRepository(dir)
            cliRepo.syncPrimaryOwnerCredentials("admin", "resetPass999")

            // Running daemon instance should detect disk change and accept the new password
            assertNull(runningDaemonRepo.authenticate("admin", "initialPass1"))
            assertNotNull(runningDaemonRepo.authenticate("admin", "resetPass999"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
