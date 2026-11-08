package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Une envie notée par [owner] (l'un des deux membres du duo). Les envies n'existent qu'en duo (`docs/duo.md`). */
@Entity(tableName = "wish", indices = [Index(value = ["uid"], unique = true)])
data class WishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "owner") val owner: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "link") val link: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @Embedded val sync: SyncColumns = SyncColumns(shared = true),
)

/** Les deux membres du duo : moi ([isMe]) et l'autre. L'identifiant est un UUID aléatoire, sans lien avec un compte. */
@Entity(tableName = "duo_member")
data class DuoMemberEntity(
    @PrimaryKey @ColumnInfo(name = "member_id") val memberId: String,
    @ColumnInfo(name = "is_me") val isMe: Boolean,
)

/**
 * État de la synchro, une seule ligne. La clé de couple n'est pas ici : elle est chiffrée par le Keystore, comme la
 * clé de la base (ADR 0004).
 */
@Entity(tableName = "duo_state")
data class DuoStateEntity(
    @PrimaryKey val id: Int = SINGLE_STATE_ID,
    @ColumnInfo(name = "duo_id") val duoId: String,
    /** Horodatage serveur du dernier bloc reçu. */
    @ColumnInfo(name = "last_seen") val lastSeen: Long,
    /** Dernier `local_changed_at` envoyé avec succès. */
    @ColumnInfo(name = "pushed_until") val pushedUntil: Long,
    /** Le duo n'existe plus côté serveur : l'autre l'a quitté. */
    @ColumnInfo(name = "ended") val ended: Boolean,
) {
    companion object {
        const val SINGLE_STATE_ID = 1
    }
}
