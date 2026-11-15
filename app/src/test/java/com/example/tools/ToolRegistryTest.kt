package com.example.tools

import com.example.domain.tool.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test kiểm tra tính toàn vẹn của ToolRegistry (F14).
 */
class ToolRegistryTest {

    @Test
    fun testAllEightToolsRegistered() {
        val tools = ToolRegistry.ALL_TOOLS
        assertEquals(8, tools.size)

        // Kiểm tra id duy nhất
        val uniqueIds = tools.map { it.id }.toSet()
        assertEquals(8, uniqueIds.size)

        // Kiểm tra route duy nhất
        val uniqueRoutes = tools.map { it.route }.toSet()
        assertEquals(8, uniqueRoutes.size)
    }

    @Test
    fun testSubjectToolQuery() {
        // Môn Toán -> Trợ lý AI + Máy tính + Vẽ đồ thị
        val mathTools = ToolRegistry.getToolsForSubject("math")
        assertTrue(mathTools.any { it.id == "ai_tutor" })
        assertTrue(mathTools.any { it.id == "calculator" })
        assertTrue(mathTools.any { it.id == "graph_plotter" })

        // Môn Hóa -> Bảng tuần hoàn
        val chemTools = ToolRegistry.getToolsForSubject("chemistry")
        assertTrue(chemTools.any { it.id == "periodic_table" })

        // KHTN phân môn Hóa -> Bảng tuần hoàn
        val khtnChemTools = ToolRegistry.getToolsForSubject("khtn", phanMon = "hoa")
        assertTrue(khtnChemTools.any { it.id == "periodic_table" })

        // Lịch sử -> Dòng thời gian
        val historyTools = ToolRegistry.getToolsForSubject("history")
        assertTrue(historyTools.any { it.id == "history_timeline" })

        // Địa lí -> Bản đồ
        val geoTools = ToolRegistry.getToolsForSubject("geography")
        assertTrue(geoTools.any { it.id == "geo_map" })

        // Tiếng Anh -> Từ điển + Động từ bất quy tắc
        val englishTools = ToolRegistry.getToolsForSubject("english")
        assertTrue(englishTools.any { it.id == "dictionary" })
        assertTrue(englishTools.any { it.id == "irregular_verbs" })
    }

    @Test
    fun testSearchTools() {
        val searchAi = ToolRegistry.searchTools("trợ lý")
        assertTrue(searchAi.any { it.id == "ai_tutor" })

        val searchCalc = ToolRegistry.searchTools("máy tính")
        assertTrue(searchCalc.any { it.id == "calculator" })

        val searchPlot = ToolRegistry.searchTools("đồ thị")
        assertTrue(searchPlot.any { it.id == "graph_plotter" })

        val searchAll = ToolRegistry.searchTools("")
        assertEquals(8, searchAll.size)
    }
}
