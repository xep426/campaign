package io.github.xep426.campaign.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2: the (date, slot) index stops being unique.
 *
 * Cheap, and worth saying why: dropping an INDEX is one statement. It is
 * dropping a COLUMN that SQLite cannot do in place and that would mean
 * recreating the table and copying real user data. Those two got conflated
 * once in this project and made a five-minute change look like a risk.
 *
 * The uniqueness was never enforcing the three-a-day limit — see
 * [DailyTaskEntity] — and it was what forced reordering to park rows on
 * negative slots before writing their real ones.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_daily_tasks_date_slot")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_daily_tasks_date_slot " +
                "ON daily_tasks (date, slot)"
        )
    }
}

/**
 * 2 → 3: ARCHIVED campaigns become COMPLETED.
 *
 * The status column is a plain string, so nothing in SQLite objects to a
 * value the enum no longer has — which is exactly the danger. [toDomain]
 * is deliberately lenient and maps an unparseable status to ACTIVE, so
 * without this statement every archived campaign would have come back to
 * life in the active list on first launch after the update. Silently, and
 * with its closedAt still set.
 *
 * COMPLETED rather than deleted, because the row is a record of work the
 * user did and closing it was their decision either way. Anyone who wants
 * it gone can delete it, which is what deletion has always meant here.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE campaigns SET status = 'COMPLETED' WHERE status = 'ARCHIVED'")
    }
}

/**
 * 3 → 4: the questlog. Campaigns hold the slots; steps outlive the day.
 *
 * THE DAY IS GONE AS AN ORGANISING IDEA. Three tasks a day with a hard
 * boundary at 22:00 meant unfinished work was thrown away nightly, which
 * is right for a daily list and wrong for the thing the app turned out to
 * be for: hobbies and side projects that lose to everyday life not because
 * a day went badly but because they are never the most urgent thing.
 *
 * So the cap moves up a level. A campaign runs until it is done, holds an
 * ordered list of steps, and at most three campaigns can be PINNED at
 * once. That is still a forcing function — you may only push three things
 * — but it no longer resets every night.
 *
 * Three cheap ALTERs rather than a table rebuild, which matters because
 * this database holds real history:
 *  - campaigns.slot     — 0..2 when pinned, NULL otherwise.
 *  - daily_tasks.completedOn — when a step was finished. Needed for
 *    "nothing for 23 days", the pressure that replaces the daily reset.
 *  - daily_tasks.position    — order within a campaign.
 *
 * daily_tasks.date and .slot become dead weight rather than being dropped:
 * removing a column in SQLite means recreating the table. They join
 * campaigns.nextStep in the "remove at the next migration that already
 * needs a rebuild" pile.
 *
 * Backfills, in order of how much they could cost if wrong:
 *
 *  1. Completed steps get completedOn = date. Their date WAS the day they
 *     were finished on, so this is a rename rather than a guess.
 *  2. position stays 0 everywhere. Queries order by (position, id), so
 *     existing steps fall into creation order — right, and safer than a
 *     correlated UPDATE that has to reason about nulls.
 *  3. The three oldest active campaigns get pinned, so the app opens with
 *     content instead of an empty questlog asking what happened.
 *  4. Every OPEN task with no campaign becomes a one-step campaign of the
 *     same name. In the new model that is exactly what it already was, and
 *     the alternative — leaving it in a table nothing reads — is data loss
 *     that merely looks tidy. Completed ones stay as they are; history
 *     shows them without needing a parent.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE campaigns ADD COLUMN slot INTEGER")
        db.execSQL("ALTER TABLE daily_tasks ADD COLUMN completedOn TEXT")
        db.execSQL("ALTER TABLE daily_tasks ADD COLUMN position INTEGER NOT NULL DEFAULT 0")

        db.execSQL("UPDATE daily_tasks SET completedOn = date WHERE completed = 1")

        // Pin the three oldest. The subquery counts how many active
        // campaigns are older, which is that campaign's rank; anything
        // ranked 3 or beyond is left unpinned.
        db.execSQL(
            """
            UPDATE campaigns SET slot = (
                SELECT COUNT(*) FROM campaigns AS older
                WHERE older.status = 'ACTIVE'
                  AND (older.createdAt < campaigns.createdAt
                       OR (older.createdAt = campaigns.createdAt AND older.id < campaigns.id))
            ) WHERE status = 'ACTIVE'
            """.trimIndent()
        )
        db.execSQL("UPDATE campaigns SET slot = NULL WHERE slot IS NOT NULL AND slot > 2")

        // Loose open tasks become their own campaigns. Done row by row
        // because each needs the id of the campaign just inserted, and a
        // few dozen inserts at upgrade time is not worth a clever single
        // statement that would be harder to be sure of.
        val loose = db.query(
            "SELECT id, title, date FROM daily_tasks WHERE campaignId IS NULL AND completed = 0"
        )
        val adopted = mutableListOf<Triple<Long, String, String>>()
        loose.use {
            while (it.moveToNext()) {
                adopted += Triple(it.getLong(0), it.getString(1), it.getString(2))
            }
        }
        for ((taskId, title, date) in adopted) {
            db.execSQL(
                "INSERT INTO campaigns (title, status, createdAt, closedAt, nextStep, notes, slot) " +
                    "VALUES (?, 'ACTIVE', ?, NULL, '', '', NULL)",
                arrayOf(title, date),
            )
            val idCursor = db.query("SELECT last_insert_rowid()")
            val campaignId = idCursor.use { if (it.moveToFirst()) it.getLong(0) else 0L }
            if (campaignId != 0L) {
                db.execSQL(
                    "UPDATE daily_tasks SET campaignId = ? WHERE id = ?",
                    arrayOf(campaignId, taskId),
                )
            }
        }
    }
}
