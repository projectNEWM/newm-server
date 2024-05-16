package io.newm.server.features.scheduler

import io.mockk.*
import io.mockk.junit5.MockKExtension
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.user.model.User
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.typealiases.ReleaseId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.impl.JobDetailImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ExtendWith(MockKExtension::class)
class EvearaReleaseStatusJobTest : KoinTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            // initialize Koin dependency injection for tests
            val userRepo = mockk<UserRepository>(relaxed = true)
            val user =
                User(
                    firstName = "Danketsu",
                    lastName = "",
                    genre = "House",
                    role = "Artist",
                    email = "danketsu@me.com",
                    nickname = "Danketsu",
                    websiteUrl = "https://danketsu.io",
                    instagramUrl = "https://instagram.com/danketsu",
                    twitterUrl = "https://twitter.com/danketsu",
                    // FIXME: This fails at Eveara because their spotify name is "Ada Ninjaz" and not "Danketsu"
                    spotifyProfile = "https://open.spotify.com/artist/4EiSbT0iP4YARJ9MGClRgB",
                )
            coEvery { userRepo.get(any()) } coAnswers { user }

            // Mock SongRepository
            val songRepo = mockk<SongRepository>(relaxed = true)
            val release = Release(forceDistributed = true, distributionReleaseId = 12)
            val song = Song(releaseId = ReleaseId.randomUUID(), title = "[DistributionFailure]")
            coEvery { songRepo.get(any()) } coAnswers { song }
            coEvery { songRepo.getRelease(any()) } coAnswers { release }

            // Mock ConfigRepository
            val configRepository = mockk<ConfigRepository>(relaxed = true)
            coEvery { configRepository.getInt(any()) } coAnswers { 30 }

            startKoin {
                modules(
                    module {
                        // inject mocks
                        single<Logger> { LoggerFactory.getLogger("UtilTest") }
                        single {
                            Json {
                                ignoreUnknownKeys = true
                                explicitNulls = false
                                isLenient = true
                            }
                        }
                        single {
                            mockk<DistributionRepository>(relaxed = true) {
                            }
                        }
                        single { mockk<CardanoRepository>(relaxed = true) }
                        single { userRepo }
                        single { songRepo }
                        single { configRepository }
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

    var evearaReleaseStatusJob: EvearaReleaseStatusJob = EvearaReleaseStatusJob()

    @Test
    fun `test when refire count is less than CONFIG_KEY_EVEARA_STATUS_CHECK_REFIRE`() =
        runTest {
            // var evearaReleaseStatusJob: EvearaReleaseStatusJob = EvearaReleaseStatusJob()
            var dataMp: JobDataMap = JobDataMap()
            dataMp.put("userId", "20073f71-6c95-4565-89f4-c1640657086c")
            dataMp.put("songId", "82a37c5f-525a-416a-ba34-6eaa8a5a486c")
            val mockJobExecutionContext = mockk<JobExecutionContext>(relaxed = true)
            every { mockJobExecutionContext.mergedJobDataMap } returns dataMp
            // create JobDetail
            var jobDetail = JobDetailImpl()
            jobDetail.group = "refire"
            jobDetail.name = "refire-test"
            every { mockJobExecutionContext.jobDetail } returns jobDetail

            // Mock Scheduler
            val mockScheduler = mockk<Scheduler>(relaxed = true)
            every { mockJobExecutionContext.scheduler } returns mockScheduler

            evearaReleaseStatusJob.execute(mockJobExecutionContext)
            var jobKey = JobKey("refire-test", "refire")
            verify { mockScheduler wasNot Called }

            // verify { mockScheduler.deleteJob(jobKey) }
        }

    @Test
    fun `test when refire count is greater than CONFIG_KEY_EVEARA_STATUS_CHECK_REFIRE`() =
        runTest {
            // var evearaReleaseStatusJob: EvearaReleaseStatusJob = EvearaReleaseStatusJob()
            var dataMp: JobDataMap = JobDataMap()
            dataMp.put("userId", "20073f71-6c95-4565-89f4-c1640657086c")
            dataMp.put("songId", "82a37c5f-525a-416a-ba34-6eaa8a5a486c")
            val mockJobExecutionContext = mockk<JobExecutionContext>(relaxed = true)
            every { mockJobExecutionContext.mergedJobDataMap } returns dataMp
            every { mockJobExecutionContext.refireCount } returns 61
            // create JobDetail
            var jobDetail = JobDetailImpl()
            jobDetail.group = "refire"
            jobDetail.name = "refire-test"
            every { mockJobExecutionContext.jobDetail } returns jobDetail

            // Mock Scheduler
            val mockScheduler = mockk<Scheduler>(relaxed = true)
            every { mockJobExecutionContext.scheduler } returns mockScheduler

            evearaReleaseStatusJob.execute(mockJobExecutionContext)
            var jobKey = JobKey("refire-test", "refire")

            verify { mockScheduler.deleteJob(jobKey) }
        }
}
