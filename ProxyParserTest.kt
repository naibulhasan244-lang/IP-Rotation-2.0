package com.yourname.iprotation2

import com.yourname.iprotation2.data.model.ProxyType
import com.yourname.iprotation2.data.parser.ProxyParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProxyParserTest {

    private fun parse(s: String, def: ProxyType = ProxyType.HTTP) = ProxyParser.parseLine(s, def)

    @Test fun ipPort_usesDefaultType() {
        val p = parse("1.2.3.4:8080", ProxyType.SOCKS5)!!
        assertEquals("1.2.3.4", p.host)
        assertEquals(8080, p.port)
        assertEquals(ProxyType.SOCKS5, p.type)
        assertNull(p.username)
    }

    @Test fun ipPortUserPass() {
        val p = parse("1.2.3.4:8080:bob:secret")!!
        assertEquals("bob", p.username)
        assertEquals("secret", p.password)
    }

    @Test fun userPassAtIpPort() {
        val p = parse("bob:secret@1.2.3.4:3128")!!
        assertEquals("1.2.3.4", p.host)
        assertEquals(3128, p.port)
        assertEquals("bob", p.username)
        assertEquals("secret", p.password)
    }

    @Test fun schemeOverridesDefault() {
        val p = parse("socks5://5.6.7.8:1080", ProxyType.HTTP)!!
        assertEquals(ProxyType.SOCKS5, p.type)
        assertEquals(1080, p.port)
    }

    @Test fun schemeWithAuth() {
        val p = parse("socks4://u:p@host.example.com:1080")!!
        assertEquals(ProxyType.SOCKS4, p.type)
        assertEquals("host.example.com", p.host)
        assertEquals("u", p.username)
    }

    @Test fun userPassHostPort() {
        val p = parse("bob:secret:1.2.3.4:8080")!!
        assertEquals("1.2.3.4", p.host)
        assertEquals(8080, p.port)
        assertEquals("bob", p.username)
    }

    @Test fun invalidLines() {
        assertNull(parse("hello"))
        assertNull(parse("1.2.3.4"))
        assertNull(parse("1.2.3.4:99999"))
        assertNull(parse("1.2.3.4:abc"))
        assertNull(parse("ftp://1.2.3.4:21"))
    }

    @Test fun bulk_skipsBlankAndComments_andReportsInvalid() {
        val r = ProxyParser.parseBulk(
            """
            # my list
            1.2.3.4:80

            socks5://5.6.7.8:1080
            garbage
            """.trimIndent(),
            ProxyType.HTTP
        )
        assertEquals(2, r.valid.size)
        assertEquals(listOf("garbage"), r.invalid)
        assertNotNull(r.valid.firstOrNull { it.type == ProxyType.SOCKS5 })
    }
}
