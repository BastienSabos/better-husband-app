package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedType
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DuoInviteFlowTest {
    private val server = InMemoryDuoInviteServer()
    private val now = 1_800_000_000_000L

    @Test
    fun `host and guest end up with the same keys and the same duo`() {
        val host = DuoInviteHost.start(server, now)
        val guest = DuoInviteGuest.join(server, DuoInviteLink.parse(host.link.toUrl("duo.example"))!!, now)

        assertThat(guest.poll()).isEqualTo(GuestStep.Waiting)
        val atHost = (host.poll() as HostStep.Joined).joined
        val atGuest = (guest.poll() as GuestStep.Joined).joined

        assertThat(atGuest.duoId).isEqualTo(atHost.duoId)
        assertThat(atGuest.keys.verificationCode()).isEqualTo(atHost.keys.verificationCode())
        assertThat(atHost.myMemberId).isEqualTo(atGuest.partnerMemberId)
        assertThat(atGuest.myMemberId).isEqualTo(atHost.partnerMemberId)
        assertThat(atHost.myMemberId).isNotEqualTo(atGuest.myMemberId)
        val blob = atHost.keys.blobId(SharedType.WISH, "w")
        assertThat(atGuest.keys.open(atHost.keys.seal("ok".toByteArray(), "d", blob), "d", blob)).isEqualTo("ok".toByteArray())
    }

    @Test
    fun `the host keeps waiting until someone answers`() {
        assertThat(DuoInviteHost.start(server, now).poll()).isEqualTo(HostStep.Waiting)
    }

    @Test
    fun `someone who only knows the invitation id is refused and the invitation stays open`() {
        val host = DuoInviteHost.start(server, now)
        val forged = DuoInviteLink(host.link.inviteId, DuoInvitation.newSecret())
        DuoInviteGuest.join(server, forged, now)

        assertThat(host.poll()).isEqualTo(HostStep.Refused)
        assertThat(host.poll()).isEqualTo(HostStep.Waiting)
        DuoInviteGuest.join(server, host.link, now)
        assertThat(host.poll()).isInstanceOf(HostStep.Joined::class.java)
    }

    @Test
    fun `an expired invitation cannot be joined`() {
        val host = DuoInviteHost.start(server, now)

        assertThrows(InviteUnavailableException::class.java) {
            DuoInviteGuest.join(server, host.link, now + DuoInviteHost.VALIDITY_MILLIS + 1)
        }
    }

    @Test
    fun `an unknown invitation cannot be joined`() {
        assertThrows(InviteUnavailableException::class.java) {
            DuoInviteGuest.join(server, DuoInviteLink("0".repeat(32), DuoInvitation.newSecret()), now)
        }
    }

    @Test
    fun `an invitation serves only once`() {
        val host = DuoInviteHost.start(server, now)
        DuoInviteGuest.join(server, host.link, now)
        host.poll()

        assertThrows(InviteUnavailableException::class.java) { DuoInviteGuest.join(server, host.link, now) }
    }

    @Test
    fun `the link carries the secret after the hash and nothing else readable`() {
        val host = DuoInviteHost.start(server, now)
        val url = host.link.toUrl("duo.example")

        assertThat(url).startsWith("https://duo.example/i/${host.link.inviteId}#")
        val parsed = DuoInviteLink.parse(url)!!
        assertThat(parsed.inviteId).isEqualTo(host.link.inviteId)
        assertThat(parsed.secret).isEqualTo(host.link.secret)
    }

    @Test
    fun `malformed links are not understood`() {
        val id = "a".repeat(32)
        val secret = "A".repeat(22)

        listOf(
            "",
            "http://duo.example/i/$id#$secret",
            "https://duo.example/i/$id",
            "https://duo.example/i/short#$secret",
            "https://duo.example/i/$id#tooShort",
            "https://duo.example/x/$id#$secret",
            "https://duo.example/i/$id?x=1#$secret",
        ).forEach { assertThat(DuoInviteLink.parse(it)).isNull() }
    }
}
