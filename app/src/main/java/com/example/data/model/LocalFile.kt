package com.example.data.model

import java.io.File

data class LocalFile(
    val id: String,
    val name: String,
    val size: Long,
    val type: FileType,
    val uriString: String? = null,
    val extension: String
) {
    val sizeFormatted: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f جيجابايت", gb)
                mb >= 1.0 -> String.format("%.2f ميجابايت", mb)
                else -> String.format("%.2f كيلوبايت", kb)
            }
        }
}

enum class FileType {
    IMAGE,
    VIDEO,
    APP,
    DOCUMENT,
    AUDIO
}

object PredefinedFiles {
    val sampleFiles = listOf(
        LocalFile("img_1", "رحلة_العيد_٢٠٢٦.jpg", 4234000, FileType.IMAGE, extension = "jpg"),
        LocalFile("img_2", "صورة_المشروع_النهائي.png", 1850000, FileType.IMAGE, extension = "png"),
        LocalFile("img_3", "خلفية_الهاتف_الطبيعية.jpg", 3120000, FileType.IMAGE, extension = "jpg"),
        
        LocalFile("vid_1", "شرح_تطبيقات_الذكاء_الاصطناعي.mp4", 128400000, FileType.VIDEO, extension = "mp4"),
        LocalFile("vid_2", "رحلة_البر_العائلية.mov", 412500000, FileType.VIDEO, extension = "mov"),
        
        LocalFile("app_1", "واتساب_بلس_برو.apk", 62100000, FileType.APP, extension = "apk"),
        LocalFile("app_2", "لعبة_التحدي_السريرية.apk", 145000000, FileType.APP, extension = "apk"),
        LocalFile("app_3", "مصحف_القرآن_الكريم.apk", 35400000, FileType.APP, extension = "apk"),
        
        LocalFile("doc_1", "كتاب_البرمجة_بلغة_كوتلن.pdf", 12500000, FileType.DOCUMENT, extension = "pdf"),
        LocalFile("doc_2", "التقرير_المالي_الربع_السنوي.xlsx", 890000, FileType.DOCUMENT, extension = "xlsx"),
        LocalFile("doc_3", "سيرة_ذاتية_أحمد_البنا.pdf", 2100000, FileType.DOCUMENT, extension = "pdf"),
        
        LocalFile("aud_1", "شريط_تلاوة_العفاسي.mp3", 45000000, FileType.AUDIO, extension = "mp3"),
        LocalFile("aud_2", "بودكاست_التقنية_والفلسفة_ح١٢.mp3", 68000000, FileType.AUDIO, extension = "mp3")
    )
}
