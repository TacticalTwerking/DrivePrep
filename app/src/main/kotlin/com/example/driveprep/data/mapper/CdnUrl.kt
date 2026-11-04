package com.example.driveprep.data.mapper

/**
 * 题库图片 CDN(chetailian.com)已支持 https;
 * 统一升级链接,避免明文流量被系统拒绝(manifest 已禁 cleartext)。
 * 展示层与离线下载器必须使用同一转换,保证 URL 哈希一致。
 */
fun cdnUrlToHttps(url: String): String {
    return if (url.startsWith("http://")) "https://" + url.substring(7) else url
}
