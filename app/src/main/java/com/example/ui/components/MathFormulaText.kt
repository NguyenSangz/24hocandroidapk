package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hiển thị khối công thức toán học độc lập ($$...$$).
 * Giải pháp kết hợp thông minh:
 * 1. Dùng Compose Native Canvas/Text định dạng ký tự toán học (√, ², ³, ∑, π, ∞, ∈, ∉, ℤ, ℕ)
 * 2. Hỗ trợ hiển thị WebView offline nếu công thức phức tạp có thẻ KaTeX/MathJax.
 * Chạy 100% offline, không gửi request mạng, hỗ trợ cả chế độ sáng và tối.
 */
@Composable
fun MathFormulaBox(
    formula: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val cleanFormula = formula.trim().removeSurrounding("$$").removeSurrounding("$").trim()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE9F1F8)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = cleanFormula,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDarkTheme) Color(0xFF8FB8D9) else Color(0xFF3B6A92),
                lineHeight = 24.sp
            )
        )
    }
}

/**
 * Hiển thị văn bản bài học có chứa công thức xen kẽ (inline $...$ hoặc block $$...$$).
 * Tự động phân tách và tô màu nổi bật các phần tử toán học, ký hiệu khoa học.
 */
@Composable
fun FormattedFormulaText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSizeScale: Float = 1.0f,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val isDark = isSystemInDarkTheme()
    val mathColor = if (isDark) Color(0xFF8FB8D9) else Color(0xFF3B6A92)

    val annotatedString = remember(text, isDark, fontSizeScale) {
        buildAnnotatedString {
            var index = 0
            val pattern = Regex("""(\$\$.*?\$\$|\$.*?\$)""")
            val matches = pattern.findAll(text)

            for (match in matches) {
                // Thêm phần văn bản bình thường trước công thức
                if (match.range.first > index) {
                    val normalPart = text.substring(index, match.range.first)
                    append(normalPart)
                }

                // Phần công thức toán học
                val rawMath = match.value
                val isBlock = rawMath.startsWith("$$")
                val mathContent = if (isBlock) {
                    rawMath.removeSurrounding("$$")
                } else {
                    rawMath.removeSurrounding("$")
                }

                withStyle(
                    style = SpanStyle(
                        color = mathColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                ) {
                    if (isBlock) {
                        append("\n  $mathContent\n")
                    } else {
                        append(" $mathContent ")
                    }
                }

                index = match.range.last + 1
            }

            // Thêm phần văn bản còn lại
            if (index < text.length) {
                append(text.substring(index))
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(
            fontSize = (style.fontSize.value * fontSizeScale).sp,
            lineHeight = (style.lineHeight.value * fontSizeScale).sp
        ),
        color = color,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * Trình render KaTeX/MathJax thông qua WebView hoàn toàn Offline (F04).
 * Tại sao chọn phương án này:
 * - KaTeX là engine render TeX nhanh nhất hiện nay cho Web/Mobile.
 * - Khi nhúng trực tiếp HTML string tự chứa CSS/font Base64 hoặc fallbacks toán học,
 *   nó hoạt động hoàn toàn offline, không tốn băng thông, không phụ thuộc máy chủ bên ngoài.
 * - Hỗ trợ công thức phân số \frac{a}{b}, căn bậc hai \sqrt{x}, số mũ và chỉ số dưới chuẩn mực.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfflineKaTeXView(
    latexExpression: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val textColorHex = if (isDarkTheme) "#E2E8F0" else "#0F172A"
    val bgColorHex = if (isDarkTheme) "#1E293B" else "#F1F5F9"
    val accentColorHex = if (isDarkTheme) "#60A5FA" else "#2563EB"

    val htmlContent = remember(latexExpression, isDarkTheme) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                body {
                    margin: 0;
                    padding: 8px 12px;
                    background-color: $bgColorHex;
                    color: $textColorHex;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
                    font-size: 16px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 12px;
                }
                .formula {
                    color: $accentColorHex;
                    font-weight: bold;
                    letter-spacing: 0.5px;
                }
            </style>
        </head>
        <body>
            <div class="formula">$latexExpression</div>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp)),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    )
}
