package com.example.tools

import com.example.domain.tools.calculator.AngleMode
import com.example.domain.tools.calculator.CalcResult
import com.example.domain.tools.calculator.CalculatorParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test toàn diện cho bộ phân tích và tính toán toán học (F14a - CalculatorParser).
 */
class ScientificCalculatorParserTest {

    @Test
    fun testBasicArithmeticAndPrecedence() {
        val parser = CalculatorParser()

        val r1 = parser.evaluate("2 + 3 * 4")
        assertTrue(r1 is CalcResult.Success)
        assertEquals("14", (r1 as CalcResult.Success).formatted)

        val r2 = parser.evaluate("(2 + 3) * 4")
        assertTrue(r2 is CalcResult.Success)
        assertEquals("20", (r2 as CalcResult.Success).formatted)

        val r3 = parser.evaluate("10 - 2 - 3")
        assertTrue(r3 is CalcResult.Success)
        assertEquals("5", (r3 as CalcResult.Success).formatted)

        val r4 = parser.evaluate("18 / 3 / 2")
        assertTrue(r4 is CalcResult.Success)
        assertEquals("3", (r4 as CalcResult.Success).formatted)
    }

    @Test
    fun testVietnameseDecimalCommaAndExactPrecision() {
        val parser = CalculatorParser()

        // Kiểm tra dấu phẩy Việt Nam
        val r1 = parser.evaluate("3,5 + 1,5")
        assertTrue(r1 is CalcResult.Success)
        assertEquals("5", (r1 as CalcResult.Success).formatted)

        // Khử sai số dấu phẩy động 0.1 + 0.2
        val r2 = parser.evaluate("0,1 + 0,2")
        assertTrue(r2 is CalcResult.Success)
        assertEquals("0.3", (r2 as CalcResult.Success).formatted)

        val r3 = parser.evaluate("0.1 + 0.2")
        assertTrue(r3 is CalcResult.Success)
        assertEquals("0.3", (r3 as CalcResult.Success).formatted)

        // Phép trừ thập phân
        val r4 = parser.evaluate("1 - 0.9")
        assertTrue(r4 is CalcResult.Success)
        assertEquals("0.1", (r4 as CalcResult.Success).formatted)
    }

    @Test
    fun testPowerAndSqrt() {
        val parser = CalculatorParser()

        val r1 = parser.evaluate("2^3")
        assertTrue(r1 is CalcResult.Success)
        assertEquals("8", (r1 as CalcResult.Success).formatted)

        // Lũy thừa kết hợp phải: 2^3^2 = 2^(3^2) = 2^9 = 512
        val r2 = parser.evaluate("2^3^2")
        assertTrue(r2 is CalcResult.Success)
        assertEquals("512", (r2 as CalcResult.Success).formatted)

        val r3 = parser.evaluate("√16")
        assertTrue(r3 is CalcResult.Success)
        assertEquals("4", (r3 as CalcResult.Success).formatted)

        val r4 = parser.evaluate("sqrt(9 + 16)")
        assertTrue(r4 is CalcResult.Success)
        assertEquals("5", (r4 as CalcResult.Success).formatted)
    }

    @Test
    fun testTrigonometryDegreeAndRadian() {
        val parserDeg = CalculatorParser(angleMode = AngleMode.DEGREE)

        val sin30 = parserDeg.evaluate("sin(30)")
        assertTrue(sin30 is CalcResult.Success)
        assertEquals("0.5", (sin30 as CalcResult.Success).formatted)

        val cos60 = parserDeg.evaluate("cos(60)")
        assertTrue(cos60 is CalcResult.Success)
        assertEquals("0.5", (cos60 as CalcResult.Success).formatted)

        val tan45 = parserDeg.evaluate("tan(45)")
        assertTrue(tan45 is CalcResult.Success)
        assertEquals("1", (tan45 as CalcResult.Success).formatted)

        // tan(90) ở chế độ DEG phải báo lỗi tiệm cận
        val tan90 = parserDeg.evaluate("tan(90)")
        assertTrue(tan90 is CalcResult.Error)
        assertTrue((tan90 as CalcResult.Error).message.contains("không xác định"))

        // Chế độ Radian
        val parserRad = CalculatorParser(angleMode = AngleMode.RADIAN)
        val sinPiOver2 = parserRad.evaluate("sin(π / 2)")
        assertTrue(sinPiOver2 is CalcResult.Success)
        assertEquals("1", (sinPiOver2 as CalcResult.Success).formatted)
    }

    @Test
    fun testLogAndLn() {
        val parser = CalculatorParser()

        val log100 = parser.evaluate("log(100)")
        assertTrue(log100 is CalcResult.Success)
        assertEquals("2", (log100 as CalcResult.Success).formatted)

        val lne = parser.evaluate("ln(e)")
        assertTrue(lne is CalcResult.Success)
        assertEquals("1", (lne as CalcResult.Success).formatted)

        val logZero = parser.evaluate("log(0)")
        assertTrue(logZero is CalcResult.Error)
        assertTrue((logZero as CalcResult.Error).message.contains("số dương"))
    }

    @Test
    fun testFactorialAndPercent() {
        val parser = CalculatorParser()

        val f5 = parser.evaluate("5!")
        assertTrue(f5 is CalcResult.Success)
        assertEquals("120", (f5 as CalcResult.Success).formatted)

        val f0 = parser.evaluate("0!")
        assertTrue(f0 is CalcResult.Success)
        assertEquals("1", (f0 as CalcResult.Success).formatted)

        val p50 = parser.evaluate("50%")
        assertTrue(p50 is CalcResult.Success)
        assertEquals("0.5", (p50 as CalcResult.Success).formatted)

        val mulPercent = parser.evaluate("200 * 15%")
        assertTrue(mulPercent is CalcResult.Success)
        assertEquals("30", (mulPercent as CalcResult.Success).formatted)
    }

    @Test
    fun testAbsoluteValueAndImplicitMultiplication() {
        val parser = CalculatorParser()

        val abs1 = parser.evaluate("|-7|")
        assertTrue(abs1 is CalcResult.Success)
        assertEquals("7", (abs1 as CalcResult.Success).formatted)

        val abs2 = parser.evaluate("abs(-3,5)")
        assertTrue(abs2 is CalcResult.Success)
        assertEquals("3.5", (abs2 as CalcResult.Success).formatted)

        // Nhân ngầm định: 2(3+4) = 14
        val imp1 = parser.evaluate("2(3 + 4)")
        assertTrue(imp1 is CalcResult.Success)
        assertEquals("14", (imp1 as CalcResult.Success).formatted)

        // Nhân ngầm định với π: 2π
        val imp2 = parser.evaluate("2π")
        assertTrue(imp2 is CalcResult.Success)
        assertEquals(2 * Math.PI, (imp2 as CalcResult.Success).value, 1e-9)
    }

    @Test
    fun testMemoryAns() {
        val parser = CalculatorParser(ansValue = 42.0)

        val r = parser.evaluate("Ans / 2 + 8")
        assertTrue(r is CalcResult.Success)
        assertEquals("29", (r as CalcResult.Success).formatted)
    }

    @Test
    fun testDivisionByZeroError() {
        val parser = CalculatorParser()
        val r = parser.evaluate("15 / 0")
        assertTrue(r is CalcResult.Error)
        assertTrue((r as CalcResult.Error).message.contains("Không thể chia cho 0"))
    }

    @Test
    fun testNegativeSquareRootError() {
        val parser = CalculatorParser()
        val r = parser.evaluate("√(-9)")
        assertTrue(r is CalcResult.Error)
        assertTrue((r as CalcResult.Error).message.contains("Căn bậc hai của số âm"))
    }

    @Test
    fun testUnbalancedParenthesisError() {
        val parser = CalculatorParser()
        val r = parser.evaluate("(2 + 3 * (4 - 1)")
        assertTrue(r is CalcResult.Error)
        assertTrue((r as CalcResult.Error).message.contains("thiếu dấu ngoặc"))
    }

    @Test
    fun testMalformedExpressionsDoNotCrash() {
        val parser = CalculatorParser()

        val badCases = listOf(
            "",
            "   ",
            "+ * /",
            "5 + +",
            "((((",
            ")2+3(",
            "abc + 5"
        )

        for (expr in badCases) {
            val res = parser.evaluate(expr)
            assertTrue("Biểu thức '$expr' phải trả về CalcResult.Error", res is CalcResult.Error)
            assertNotNull((res as CalcResult.Error).message)
        }
    }

    @Test
    fun testFractionConversion() {
        val f1 = CalculatorParser.tryFindFraction(0.75)
        assertEquals("3/4", f1)

        val f2 = CalculatorParser.tryFindFraction(0.2)
        assertEquals("1/5", f2)

        val f3 = CalculatorParser.tryFindFraction(-0.125)
        assertEquals("-1/8", f3)
    }
}
