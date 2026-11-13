package com.bastiensabos.betterhusband.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bastiensabos.betterhusband.data.local.entity.DuoMemberEntity
import com.bastiensabos.betterhusband.data.local.entity.DuoStateEntity
import com.bastiensabos.betterhusband.data.local.entity.HouseholdTaskEntity
import com.bastiensabos.betterhusband.data.local.entity.IdeaEntity
import com.bastiensabos.betterhusband.data.local.entity.ImportantDateEntity
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import com.bastiensabos.betterhusband.data.local.entity.WishEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DuoExitDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = database.close()

    private val day = LocalDate.of(2027, 1, 4)

    private fun shared(deleted: Boolean = false) = SyncColumns(shared = true, revision = 10, author = "me", deleted = deleted)

    @Test
    fun leaveKeepsTasksAndDatesInSoloAndForgetsTheDuo() =
        runBlocking {
            database.householdTaskDao().upsert(
                HouseholdTaskEntity(
                    title = "Courses",
                    repeat = "WEEKLY",
                    createdOn = day,
                    lastDoneOn = day,
                    assignee = "me",
                    lastDoneBy = "other",
                    sync = shared(),
                ),
            )
            database.householdTaskDao().upsert(
                HouseholdTaskEntity(
                    title = "Supprimée",
                    repeat = "WEEKLY",
                    createdOn = day,
                    lastDoneOn = null,
                    sync = shared(deleted = true),
                ),
            )
            database.importantDateDao().upsert(
                ImportantDateEntity(title = "Rencontre", kind = "ANNIVERSARY", date = day, recurring = true, sync = shared()),
            )
            database.importantDateDao().upsert(
                ImportantDateEntity(title = "Supprimée", kind = "OTHER", date = day, recurring = false, sync = shared(deleted = true)),
            )
            database.ideaDao().insert(IdeaEntity(text = "Album", category = "GIFT", createdAt = 0, done = false, wishUid = "w-1"))
            database.wishDao().upsert(WishEntity(owner = "me", text = "Un vélo", link = null, createdAt = 0))
            database.wishDao().upsert(WishEntity(owner = "other", text = "Un album", link = null, createdAt = 0))
            database.duoDao().upsertMember(DuoMemberEntity(memberId = "me", isMe = true))
            database.duoDao().upsertMember(DuoMemberEntity(memberId = "other", isMe = false))
            database.duoDao().upsertState(DuoStateEntity(duoId = "duo-1", lastSeen = 0, pushedUntil = 0, ended = false))

            database.duoExitDao().leave()

            val task =
                database
                    .householdTaskDao()
                    .observeAll()
                    .first()
                    .single()
            assertThat(task.title).isEqualTo("Courses")
            assertThat(task.lastDoneOn).isEqualTo(day)
            assertThat(listOf(task.assignee, task.lastDoneBy, task.sync.author)).containsExactly(null, null, null)
            assertThat(task.sync.shared).isFalse()
            val date =
                database
                    .importantDateDao()
                    .observeAll()
                    .first()
                    .single()
            assertThat(date.title).isEqualTo("Rencontre")
            assertThat(date.sync.shared).isFalse()
            assertThat(date.sync.author).isNull()
            assertThat(
                database
                    .ideaDao()
                    .observeAll()
                    .first()
                    .single()
                    .wishUid,
            ).isNull()
            assertThat(database.wishDao().observeAll().first()).isEmpty()
            assertThat(database.duoDao().observeMembers().first()).isEmpty()
            assertThat(database.duoDao().observeState().first()).isNull()
        }
}
