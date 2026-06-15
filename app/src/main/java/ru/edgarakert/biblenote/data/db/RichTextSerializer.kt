package ru.edgarakert.biblenote.data.db

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import org.json.JSONArray
import org.json.JSONObject

object RichTextSerializer {

    fun toJson(spannable: SpannableStringBuilder): String {
        val text = spannable.toString()
        val spans = JSONArray()
        val len = spannable.length

        for (span in spannable.getSpans(0, len, StyleSpan::class.java)) {
            val type = when (span.style) {
                Typeface.BOLD -> "bold"
                Typeface.ITALIC -> "italic"
                Typeface.BOLD_ITALIC -> "bold_italic"
                else -> continue
            }
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            if (start < 0 || end < 0 || start >= end) continue
            spans.put(JSONObject().apply {
                put("type", type)
                put("start", start)
                put("end", end)
            })
        }

        for (span in spannable.getSpans(0, len, RelativeSizeSpan::class.java)) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            if (start < 0 || end < 0 || start >= end) continue
            spans.put(JSONObject().apply {
                put("type", "large")
                put("start", start)
                put("end", end)
            })
        }

        if (spans.length() == 0) return ""

        return JSONObject().apply {
            put("text", text)
            put("spans", spans)
        }.toString()
    }

    fun fromJson(json: String): SpannableStringBuilder {
        if (json.isBlank()) return SpannableStringBuilder()
        return try {
            val obj = JSONObject(json)
            val text = obj.getString("text")
            val result = SpannableStringBuilder(text)
            val len = text.length
            val spans = obj.optJSONArray("spans") ?: return result

            for (i in 0 until spans.length()) {
                val s = spans.getJSONObject(i)
                val type = s.getString("type")
                val start = s.getInt("start").coerceIn(0, len)
                val end = s.getInt("end").coerceIn(0, len)
                if (start >= end) continue

                when (type) {
                    "bold" -> result.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "italic" -> result.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "bold_italic" -> result.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "large" -> result.setSpan(RelativeSizeSpan(1.5f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            result
        } catch (_: Exception) {
            SpannableStringBuilder(runCatching { JSONObject(json).getString("text") }.getOrDefault(""))
        }
    }

    fun hasFormatting(spannable: Spannable): Boolean =
        spannable.getSpans(0, spannable.length, StyleSpan::class.java).isNotEmpty() ||
            spannable.getSpans(0, spannable.length, RelativeSizeSpan::class.java).isNotEmpty()
}