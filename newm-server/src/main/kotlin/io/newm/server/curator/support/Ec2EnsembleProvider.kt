package io.newm.server.curator.support

import org.apache.curator.ensemble.EnsembleProvider
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest
import software.amazon.awssdk.services.ec2.model.Filter

class Ec2EnsembleProvider(
    private val ec2Client: Ec2Client,
    private val role: String,
    private val port: Int
) : EnsembleProvider {
    override fun getConnectionString(): String {
        val request = DescribeInstancesRequest
            .builder()
            .filters(
                Filter
                    .builder()
                    .name("tag:Role")
                    .values(role)
                    .build()
            ).build()

        return ec2Client
            .describeInstances(request)
            .reservations()
            .flatMap { it.instances() }
            .joinToString(",") { "${it.privateIpAddress()}:$port" }
    }

    override fun start() {}

    override fun close() {}

    override fun setConnectionString(value: String) {}

    override fun updateServerListEnabled(): Boolean = false
}
