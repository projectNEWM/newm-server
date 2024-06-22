package io.newm.server.features.minting

import com.amazonaws.services.sqs.model.Message
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import io.mockk.*
import io.mockk.junit5.MockKExtension
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.features.arweave.repo.ArweaveRepository
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.daemon.QuartzSchedulerDaemon
import io.newm.server.features.minting.repo.MintingRepository
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.logging.json
import io.newm.server.typealiases.SongId
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.test.KoinTest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.extension.ExtendWith
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.impl.JobDetailImpl
import java.util.*
import kotlin.test.assertFailsWith

@ExtendWith(MockKExtension::class)
class MintingMessageReceiverTest : KoinTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            // initialize Koin dependency injection for tests
            // Mock deps
            val songRepo = mockk<SongRepository>(relaxed = true)
            val cardanoRepo = mockk<CardanoRepository>(relaxed = true)
            val arweaveRepo = mockk<ArweaveRepository>(relaxed = true)
            val mintingRepo = mockk<MintingRepository>(relaxed = true)
            val quartzSchedulerDaemon = mockk<QuartzSchedulerDaemon>(relaxed = true)

            //
            val song =
                Song(
                    mintingStatus = MintingStatus.SubmittedForDistribution,
                    title = "test [ForceError]"
                )
            coEvery { songRepo.get(any()) } coAnswers { song }

            // Mock ConfigRepository
            val configRepository = mockk<ConfigRepository>(relaxed = true)

            startKoin {
                modules(
                    module {
                        // inject mocks
                        single {
                            Json {
                                ignoreUnknownKeys = true
                                explicitNulls = false
                                isLenient = true
                                serializersModule =
                                    SerializersModule {
                                        contextual(UUID::class, UUIDSerializer)
                                    }
                            }
                        }
                        single { mockk<CardanoRepository>(relaxed = true) }
                        single { songRepo }
                        single { configRepository }
                        single { cardanoRepo }
                        single { arweaveRepo }
                        single { mintingRepo }
                        single { quartzSchedulerDaemon }
                    }
                )
            }
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            // close Koin
            stopKoin()
        }
    }

    var mintingMessageReceiver: MintingMessageReceiver = MintingMessageReceiver()

    @Test()
    fun `test when forcing a track into error`() =
        runTest {
            // var evearaReleaseStatusJob: EvearaReleaseStatusJob = EvearaReleaseStatusJob()
            val dataMp: JobDataMap = JobDataMap()
            dataMp.put("userId", "20073f71-6c95-4565-89f4-c1640657086c")
            dataMp.put("songId", "82a37c5f-525a-416a-ba34-6eaa8a5a486c")
            val mockJobExecutionContext = mockk<JobExecutionContext>(relaxed = true)
            every { mockJobExecutionContext.mergedJobDataMap } returns dataMp
            // create JobDetail
            val jobDetail = JobDetailImpl()
            jobDetail.group = "refire"
            jobDetail.name = "refire-test"
            every { mockJobExecutionContext.jobDetail } returns jobDetail

            // Mock Scheduler
            val mockScheduler = mockk<Scheduler>(relaxed = true)
            every { mockJobExecutionContext.scheduler } returns mockScheduler

            val mintingMsg = MintingStatusSqsMessage(SongId.randomUUID(), MintingStatus.SubmittedForDistribution)
            val msg = Message()
            msg.messageId = "test-minting-message-receiver"
            msg.body = json.encodeToString(mintingMsg)

            mintingMessageReceiver = MintingMessageReceiver()
            // When we call display() with the wrong argument

            // Then it should throw an IllegalArgumentException
            assertFailsWith<DistributeAndMintException>(block = {
                runBlocking {
                    launch {
                        mintingMessageReceiver.onMessageReceived(msg)
                    }
                }
            })
        }
}
