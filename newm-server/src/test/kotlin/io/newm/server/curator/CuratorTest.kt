package io.newm.server.curator

import java.net.Socket
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.retry.ExponentialBackoffRetry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Simple connectivity test to a locally running ZooKeeper instance on localhost:2181 using Curator.
 *
 * The test will:
 * 1. Attempt a TCP connection to localhost:2181. If not available, it is skipped (so normal unit test runs
 *    without ZooKeeper present won't fail the build).
 * 2. Start a CuratorFramework client.
 * 3. Wait (up to 10 seconds) for Curator to report a connected state.
 * 4. Create a unique znode, write data, read it back, verify round-trip equality, and delete the node.
 */
@Disabled
class CuratorTest {
    private fun isZookeeperAvailable(
        host: String = "localhost",
        port: Int = 2181
    ): Boolean =
        try {
            Socket(host, port).use { true }
        } catch (_: Exception) {
            false
        }

    @Test
    fun `connect to local zookeeper and perform CRUD`() {
        Assumptions.assumeTrue(isZookeeperAvailable(), "ZooKeeper not available on localhost:2181 - skipping")

        val retryPolicy = ExponentialBackoffRetry(1_000, 3)
        val client =
            CuratorFrameworkFactory
                .builder()
                .connectString("localhost:2181")
                .retryPolicy(retryPolicy)
                .sessionTimeoutMs(15_000)
                .connectionTimeoutMs(5_000)
                .build()

        client.start()
        try {
            val start = System.currentTimeMillis()
            while (!client.zookeeperClient.isConnected && System.currentTimeMillis() - start < 10_000) {
                Thread.sleep(200)
            }
            Assertions.assertTrue(
                client.zookeeperClient.isConnected,
                "Client failed to connect to ZooKeeper at localhost:2181"
            )
            Assertions.assertEquals(CuratorFrameworkState.STARTED, client.state, "Curator client not in STARTED state")

            val path = "/newm-test-${System.nanoTime()}"
            val data = "hello-zk".toByteArray()

            client.create().creatingParentsIfNeeded().forPath(path, data)
            val read = client.data.forPath(path)
            Assertions.assertArrayEquals(data, read, "Data read from ZooKeeper does not match what was written")

            client.delete().forPath(path)
        } finally {
            client.close()
        }
    }
}
