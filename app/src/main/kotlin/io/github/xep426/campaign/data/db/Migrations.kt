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
