package com.example.domain.tools.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Chế độ góc cho hàm lượng giác.
 */
enum class AngleMode {
    DEGREE,
    RADIAN
}

/**
 * Kết quả phân tích và tính toán biểu thức.
 */
sealed class CalcResult {
    data class Success(
        val value: Double,
        val formatted: String,
        val exactFraction: String? = null
    ) : CalcResult()

    data class Error(
        val message: String
    ) : CalcResult()
}

/**
 * Bộ phân tích cú pháp biểu thức toán học (Tokenizer & Recursive Descent Parser).
 * Tuân thủ yêu cầu F14a:
 * - Không dùng `eval` hay thực thi mã script.
 * - Ưu tiên toán tử chuẩn: Ngoặc, Hàm số, Lũy thừa, Nhân/Chia, Cộng/Trừ.
 * - Hỗ trợ nhân ngầm định (2π, 2(3+4), 3sin(30)).
 * - Chấp nhận dấu phẩy thập phân kiểu Việt Nam (ví dụ 3,14) lẫn dấu chấm (3.14).
 * - Xử lý đầy đủ lỗi: Chia cho 0, Căn số âm, Biểu thức thiếu ngoặc, Sai cú pháp.
 * - Khử sai số dấu phẩy động (0.1 + 0.2 = 0.3).
 */
class CalculatorParser(
    private val angleMode: AngleMode = AngleMode.DEGREE,
    private val ansValue: Double = 0.0
) {

    // Danh sách các loại Token
    private sealed class Token {
        data class Number(val value: Double, val rawText: String) : Token()
        data class Operator(val op: Char) : Token() // '+', '-', '*', '/', '^'
        data class Func(val name: String) : Token() // "sin", "cos", "tan", "log", "ln", "sqrt", "abs"
        data object OpenParen : Token()
        data object CloseParen : Token()
        data object Factorial : Token() // '!'
        data object Percent : Token() // '%'
        data object PipeOpen : Token() // '|' mở cho abs
        data object PipeClose : Token() // '|' đóng cho abs
    }

    /**
     * Tính toán biểu thức toán học dạng chuỗi.
     */
    fun evaluate(expression: String): CalcResult {
        val cleanExpr = expression.trim()
        if (cleanExpr.isBlank()) {
            return CalcResult.Error("Biểu thức trống")
        }

        return try {
            // 1. Phân tách từ vựng (Tokenize)
            val tokens = tokenize(cleanExpr)
            if (tokens.isEmpty()) {
                return CalcResult.Error("Biểu thức trống")
            }

            // 2. Phân tích cú pháp (Parse)
            val parser = TokenParser(tokens, angleMode)
            val result = parser.parseExpression()

            if (parser.hasMoreTokens()) {
                return CalcResult.Error("Lỗi cú pháp: Ký tự thừa hoặc thiếu toán tử")
            }

            if (result.isNaN()) {
                return CalcResult.Error("Phép tính không xác định")
            }
            if (result.isInfinite()) {
                return CalcResult.Error("Giá trị vượt quá giới hạn (Vô cực)")
            }

            val formatted = formatResult(result)
            val fraction = tryFindFraction(result)
            CalcResult.Success(result, formatted, fraction)
        } catch (e: CalculatorException) {
            CalcResult.Error(e.message ?: "Lỗi tính toán")
        } catch (e: Exception) {
            CalcResult.Error("Lỗi cú pháp biểu thức: ${e.message ?: "Không hợp lệ"}")
        }
    }

    /**
     * Bộ tách từ vựng (Tokenizer).
     */
    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = input.length

        while (i < len) {
            val c = input[i]

            when {
                c.isWhitespace() -> {
                    i++
                }
                // Số tự nhiên hoặc thập phân (chấp nhận cả '.' và ',')
                c.isDigit() || ( (c == '.' || c == ',') && i + 1 < len && input[i + 1].isDigit()) -> {
                    val sb = StringBuilder()
                    var hasDot = false
                    while (i < len) {
                        val ch = input[i]
                        if (ch.isDigit()) {
                            sb.append(ch)
                            i++
                        } else if ((ch == '.' || ch == ',') && !hasDot) {
                            hasDot = true
                            sb.append('.') // Chuẩn hóa về dấu chấm cho Double parser
                            i++
                        } else {
                            break
                        }
                    }
                    val numVal = sb.toString().toDoubleOrNull()
                        ?: throw CalculatorException("Số không hợp lệ: $sb")
                    tokens.add(Token.Number(numVal, sb.toString()))
                }
                c == '+' -> {
                    tokens.add(Token.Operator('+'))
                    i++
                }
                c == '-' || c == '−' -> {
                    tokens.add(Token.Operator('-'))
                    i++
                }
                c == '*' || c == '×' || c == 'x' || c == 'X' || c == '·' -> {
                    tokens.add(Token.Operator('*'))
                    i++
                }
                c == '/' || c == '÷' || c == ':' -> {
                    tokens.add(Token.Operator('/'))
                    i++
                }
                c == '^' -> {
                    tokens.add(Token.Operator('^'))
                    i++
                }
                c == '!' -> {
                    tokens.add(Token.Factorial)
                    i++
                }
                c == '%' -> {
                    tokens.add(Token.Percent)
                    i++
                }
                c == '(' -> {
                    tokens.add(Token.OpenParen)
                    i++
                }
                c == ')' -> {
                    tokens.add(Token.CloseParen)
                    i++
                }
                c == '|' -> {
                    val lastToken = tokens.lastOrNull()
                    val isOpening = lastToken == null ||
                        lastToken is Token.Operator ||
                        lastToken is Token.OpenParen ||
                        lastToken is Token.PipeOpen
                    if (isOpening) {
                        tokens.add(Token.PipeOpen)
                    } else {
                        tokens.add(Token.PipeClose)
                    }
                    i++
                }
                c == '√' -> {
                    tokens.add(Token.Func("sqrt"))
                    i++
                }
                c == 'π' || c == 'p' && i + 1 < len && input[i + 1] == 'i' -> {
                    tokens.add(Token.Number(Math.PI, "π"))
                    i += if (c == 'π') 1 else 2
                }
                c == 'e' || c == 'E' -> {
                    // Kiểm tra xem là số Euler hay tiền tố chữ khác
                    if (i + 1 < len && input[i + 1].isLetter()) {
                        // Từ khác
                        val word = readWord(input, i)
                        i += word.length
                        handleWordToken(word, tokens)
                    } else {
                        tokens.add(Token.Number(Math.E, "e"))
                        i++
                    }
                }
                c.isLetter() -> {
                    val word = readWord(input, i)
                    i += word.length
                    handleWordToken(word, tokens)
                }
                else -> {
                    throw CalculatorException("Ký tự không hỗ trợ: '$c'")
                }
            }
        }

        // Bổ sung các phép nhân ngầm định (Implicit Multiplication)
        return insertImplicitMultiplication(tokens)
    }

    private fun readWord(input: String, start: Int): String {
        val sb = StringBuilder()
        var idx = start
        while (idx < input.length && input[idx].isLetter()) {
            sb.append(input[idx])
            idx++
        }
        return sb.toString()
    }

    private fun handleWordToken(word: String, tokens: MutableList<Token>) {
        when (word.lowercase(Locale.ROOT)) {
            "ans" -> tokens.add(Token.Number(ansValue, "Ans"))
            "pi" -> tokens.add(Token.Number(Math.PI, "π"))
            "e" -> tokens.add(Token.Number(Math.E, "e"))
            "sin" -> tokens.add(Token.Func("sin"))
            "cos" -> tokens.add(Token.Func("cos"))
            "tan" -> tokens.add(Token.Func("tan"))
            "log" -> tokens.add(Token.Func("log"))
            "ln" -> tokens.add(Token.Func("ln"))
            "sqrt" -> tokens.add(Token.Func("sqrt"))
            "abs" -> tokens.add(Token.Func("abs"))
            else -> throw CalculatorException("Hàm số hoặc biến không nhận dạng được: '$word'")
        }
    }

    /**
     * Chèn nhân ngầm định (ví dụ 2π -> 2 * π, 2(3) -> 2 * (3), (2)(3) -> (2) * (3), 3sin(x) -> 3 * sin(x))
     */
    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        val result = mutableListOf<Token>()
        for (i in tokens.indices) {
            val curr = tokens[i]
            result.add(curr)
            if (i + 1 < tokens.size) {
                val next = tokens[i + 1]
                val needsMul = when (curr) {
                    is Token.Number, is Token.CloseParen, is Token.Factorial, is Token.Percent, is Token.PipeClose -> {
                        when (next) {
                            is Token.Number, is Token.OpenParen, is Token.Func, is Token.PipeOpen -> true
                            else -> false
                        }
                    }
                    else -> false
                }
                if (needsMul) {
                    result.add(Token.Operator('*'))
                }
            }
        }
        return result
    }

    /**
     * Trình phân tích cú pháp đệ quy ưu tiên toán tử (Recursive Descent Parser).
     */
    private class TokenParser(
        private val tokens: List<Token>,
        private val angleMode: AngleMode
    ) {
        private var pos = 0

        fun hasMoreTokens(): Boolean = pos < tokens.size

        private fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null

        private fun next(): Token {
            if (pos >= tokens.size) throw CalculatorException("Lỗi: Biểu thức kết thúc bất ngờ")
            return tokens[pos++]
        }

        // Expression: Term (('+' | '-') Term)*
        fun parseExpression(): Double {
            var left = parseTerm()

            while (hasMoreTokens()) {
                val token = peek()
                if (token is Token.Operator && (token.op == '+' || token.op == '-')) {
                    next() // consume
                    val right = parseTerm()
                    left = if (token.op == '+') {
                        addExact(left, right)
                    } else {
                        subtractExact(left, right)
                    }
                } else {
                    break
                }
            }
            return left
        }

        // Term: Factor (('*' | '/') Factor)*
        private fun parseTerm(): Double {
            var left = parsePower()

            while (hasMoreTokens()) {
                val token = peek()
                if (token is Token.Operator && (token.op == '*' || token.op == '/')) {
                    next() // consume
                    val right = parsePower()
                    if (token.op == '*') {
                        left = multiplyExact(left, right)
                    } else {
                        if (abs(right) < 1e-15) {
                            throw CalculatorException("Lỗi: Không thể chia cho 0")
                        }
                        left = divideExact(left, right)
                    }
                } else {
                    break
                }
            }
            return left
        }

        // Power: Unary ('^' Power)? (right-associative)
        private fun parsePower(): Double {
            val left = parseUnary()

            if (hasMoreTokens()) {
                val token = peek()
                if (token is Token.Operator && token.op == '^') {
                    next() // consume
                    val right = parsePower() // Đệ quy phải
                    if (left < 0 && abs(right - round(right)) > 1e-9) {
                        throw CalculatorException("Lỗi: Lũy thừa cơ số âm với số mũ không nguyên")
                    }
                    if (left == 0.0 && right < 0) {
                        throw CalculatorException("Lỗi: 0 không thể nâng lên lũy thừa âm")
                    }
                    return left.pow(right)
                }
            }
            return left
        }

        // Unary: ('+' | '-')? Postfix
        private fun parseUnary(): Double {
            val token = peek()
            if (token is Token.Operator && (token.op == '+' || token.op == '-')) {
                next() // consume
                val value = parseUnary()
                return if (token.op == '-') -value else value
            }
            return parsePostfix()
        }

        // Postfix: Primary ('!' | '%')*
        private fun parsePostfix(): Double {
            var value = parsePrimary()

            while (hasMoreTokens()) {
                val token = peek()
                when (token) {
                    is Token.Factorial -> {
                        next() // consume
                        if (value < 0) {
                            throw CalculatorException("Lỗi: Giai thừa chỉ áp dụng cho số nguyên không âm")
                        }
                        if (abs(value - round(value)) > 1e-9) {
                            throw CalculatorException("Lỗi: Giai thừa chỉ áp dụng cho số nguyên")
                        }
                        val n = round(value).toLong()
                        if (n > 170) {
                            throw CalculatorException("Lỗi: Giá trị giai thừa quá lớn (vượt quá giới hạn)")
                        }
                        value = computeFactorial(n)
                    }
                    is Token.Percent -> {
                        next() // consume
                        value = value / 100.0
                    }
                    else -> break
                }
            }
            return value
        }

        // Primary: Number | OpenParen Expression CloseParen | Func | Pipe Expression Pipe
        private fun parsePrimary(): Double {
            val token = next()

            return when (token) {
                is Token.Number -> token.value
                is Token.OpenParen -> {
                    val result = parseExpression()
                    if (!hasMoreTokens() || next() !is Token.CloseParen) {
                        throw CalculatorException("Lỗi: Biểu thức thiếu dấu ngoặc đóng ')'")
                    }
                    result
                }
                is Token.PipeOpen -> {
                    // Giá trị tuyệt đối |x|
                    val result = parseExpression()
                    if (!hasMoreTokens() || next() !is Token.PipeClose) {
                        throw CalculatorException("Lỗi: Biểu thức thiếu dấu đóng giá trị tuyệt đối '|'")
                    }
                    abs(result)
                }
                is Token.Func -> {
                    // Có thể có ngoặc hoặc không (ví dụ: sin(30) hoặc √16)
                    val arg = if (hasMoreTokens() && peek() is Token.OpenParen) {
                        next() // consume '('
                        val inner = parseExpression()
                        if (!hasMoreTokens() || next() !is Token.CloseParen) {
                            throw CalculatorException("Lỗi: Thiếu dấu ngoặc đóng ')' sau hàm ${token.name}")
                        }
                        inner
                    } else {
                        parsePower()
                    }
                    evaluateFunction(token.name, arg)
                }
                else -> {
                    throw CalculatorException("Lỗi cú pháp: Ký tự không mong đợi tại đây")
                }
            }
        }

        private fun evaluateFunction(name: String, arg: Double): Double {
            return when (name) {
                "sin" -> {
                    val angleInRad = if (angleMode == AngleMode.DEGREE) Math.toRadians(arg) else arg
                    // Xử lý góc đặc biệt (ví dụ sin(180) = 0, sin(90) = 1)
                    if (angleMode == AngleMode.DEGREE) {
                        val degNorm = (arg % 360.0 + 360.0) % 360.0
                        if (abs(degNorm - 0.0) < 1e-9 || abs(degNorm - 180.0) < 1e-9 || abs(degNorm - 360.0) < 1e-9) return 0.0
                        if (abs(degNorm - 90.0) < 1e-9) return 1.0
                        if (abs(degNorm - 270.0) < 1e-9) return -1.0
                    }
                    sin(angleInRad)
                }
                "cos" -> {
                    val angleInRad = if (angleMode == AngleMode.DEGREE) Math.toRadians(arg) else arg
                    if (angleMode == AngleMode.DEGREE) {
                        val degNorm = (arg % 360.0 + 360.0) % 360.0
                        if (abs(degNorm - 90.0) < 1e-9 || abs(degNorm - 270.0) < 1e-9) return 0.0
                        if (abs(degNorm - 0.0) < 1e-9 || abs(degNorm - 360.0) < 1e-9) return 1.0
                        if (abs(degNorm - 180.0) < 1e-9) return -1.0
                    }
                    cos(angleInRad)
                }
                "tan" -> {
                    if (angleMode == AngleMode.DEGREE) {
                        val degNorm = (arg % 180.0 + 180.0) % 180.0
                        if (abs(degNorm - 90.0) < 1e-9) {
                            throw CalculatorException("Lỗi: tan(${arg.toInt()}°) không xác định (tiệm cận)")
                        }
                        if (abs(degNorm - 0.0) < 1e-9 || abs(degNorm - 180.0) < 1e-9) return 0.0
                        if (abs(degNorm - 45.0) < 1e-9) return 1.0
                        if (abs(degNorm - 135.0) < 1e-9) return -1.0
                        tan(Math.toRadians(arg))
                    } else {
                        val cosVal = cos(arg)
                        if (abs(cosVal) < 1e-14) {
                            throw CalculatorException("Lỗi: tan($arg rad) không xác định")
                        }
                        tan(arg)
                    }
                }
                "sqrt" -> {
                    if (arg < 0) {
                        throw CalculatorException("Lỗi: Căn bậc hai của số âm ($arg)")
                    }
                    sqrt(arg)
                }
                "log" -> {
                    if (arg <= 0) {
                        throw CalculatorException("Lỗi: Logarit cơ số 10 chỉ nhận số dương (arg = $arg)")
                    }
                    log10(arg)
                }
                "ln" -> {
                    if (arg <= 0) {
                        throw CalculatorException("Lỗi: Logarit tự nhiên (ln) chỉ nhận số dương (arg = $arg)")
                    }
                    ln(arg)
                }
                "abs" -> abs(arg)
                else -> throw CalculatorException("Hàm số không hỗ trợ: $name")
            }
        }

        private fun computeFactorial(n: Long): Double {
            if (n == 0L || n == 1L) return 1.0
            var res = 1.0
            for (i in 2..n) {
                res *= i
            }
            return res
        }

        // Tính toán chính xác số thập phân bằng BigDecimal cho phép cộng/trừ/nhân/chia thông thường
        private fun addExact(a: Double, b: Double): Double {
            return try {
                BigDecimal.valueOf(a).add(BigDecimal.valueOf(b), MathContext.DECIMAL64).toDouble()
            } catch (e: Exception) {
                a + b
            }
        }

        private fun subtractExact(a: Double, b: Double): Double {
            return try {
                BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b), MathContext.DECIMAL64).toDouble()
            } catch (e: Exception) {
                a - b
            }
        }

        private fun multiplyExact(a: Double, b: Double): Double {
            return try {
                BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b), MathContext.DECIMAL64).toDouble()
            } catch (e: Exception) {
                a * b
            }
        }

        private fun divideExact(a: Double, b: Double): Double {
            return try {
                BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 12, RoundingMode.HALF_UP).toDouble()
            } catch (e: Exception) {
                a / b
            }
        }
    }

    companion object {
        /**
         * Định dạng hiển thị kết quả chuẩn, tránh hiện tượng 0.30000000000000004
         */
        fun formatResult(value: Double): String {
            if (value.isNaN()) return "Không xác định"
            if (value.isInfinite()) return if (value > 0) "Vô cực (∞)" else "-Vô cực (-∞)"

            // Nếu rất gần số nguyên
            val rounded = round(value)
            if (abs(value - rounded) < 1e-11 && abs(value) < 1e14) {
                return rounded.toLong().toString()
            }

            // Số rất lớn hoặc rất bé -> dùng ký hiệu khoa học
            if (abs(value) >= 1e12 || (abs(value) > 0.0 && abs(value) < 1e-6)) {
                val sciFormat = DecimalFormat("0.######E0", DecimalFormatSymbols(Locale.US))
                return sciFormat.format(value)
            }

            // Làm tròn thông thường tối đa 10 chữ số thập phân, tự loại bỏ số 0 thừa phía sau
            val df = DecimalFormat("0.##########", DecimalFormatSymbols(Locale.US))
            df.maximumFractionDigits = 10
            return df.format(value)
        }

        /**
         * Thử chuyển đổi số thập phân thành phân số tối giản a/b nếu phù hợp (cho học sinh THCS).
         */
        fun tryFindFraction(value: Double, maxDenominator: Long = 1000): String? {
            if (value.isNaN() || value.isInfinite() || abs(value) > 100000) return null
            if (abs(value - round(value)) < 1e-9) return null // Đã là số nguyên

            val sign = if (value < 0) "-" else ""
            val posVal = abs(value)

            // Thuật toán liên phân số (Continued fraction)
            var h1 = 1L; var h2 = 0L
            var k1 = 0L; var k2 = 1L
            var b = posVal

            do {
                val a = b.toLong()
                var aux = h1
                h1 = a * h1 + h2
                h2 = aux
                aux = k1
                k1 = a * k1 + k2
                k2 = aux
                if (k1 > maxDenominator) break
                val approx = h1.toDouble() / k1.toDouble()
                if (abs(posVal - approx) < 1e-8) {
                    return if (k1 != 1L) "$sign$h1/$k1" else null
                }
                val diff = b - a
                if (abs(diff) < 1e-12) break
                b = 1.0 / diff
            } while (true)

            return null
        }
    }
}

/**
 * Lỗi phát sinh trong quá trình phân tích hoặc tính toán.
 */
class CalculatorException(message: String) : RuntimeException(message)
