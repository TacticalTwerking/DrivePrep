package com.example.driveprep.data.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ImagePackRepositoryTest {

    @Test
    fun `sha1 hex matches known vector and is stable`() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", ImagePackRepository.sha1Hex("abc"))
        assertEquals(
            ImagePackRepository.sha1Hex("http://example.com/a.jpg"),
            ImagePackRepository.sha1Hex("http://example.com/a.jpg")
        )
    }

    @Test
    fun `sha1 hex is 40 lowercase hex chars`() {
        val hex = ImagePackRepository.sha1Hex("任意中文输入")
        assertEquals(40, hex.length)
        hex.forEach { c ->
            assert(c in '0'..'9' || c in 'a'..'f') { "非十六进制字符: $c" }
        }
    }

    @Test
    fun `different urls yield different file names`() {
        assertNotEquals(
            ImagePackRepository.sha1Hex("http://a/1.jpg"),
            ImagePackRepository.sha1Hex("http://a/2.jpg")
        )
    }

    @Test
    fun `similar urls with encoding difference yield different names`() {
        // 同一张图的 %2F 与 / 编码差异在题库中并存,按原样哈希天然区分
        assertNotEquals(
            ImagePackRepository.sha1Hex("https://cdn/a%2Fb.jpg"),
            ImagePackRepository.sha1Hex("https://cdn/a/b.jpg")
        )
    }
}
