package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class THCSApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        createNotificationChannel()

        // Nạp dữ liệu mẫu từ assets vào Room nếu chưa có và đồng bộ câu sai F08
        val appScope = CoroutineScope(Dispatchers.IO)
        appScope.launch {
            try {
                container.dataSeeder.seedDatabaseIfNeeded()
                container.wrongQuestionRepository.syncExistingMistakes()
                container.userPreferencesManager.updateStreakOnActive()
                container.syncRepository.syncNow(manual = false)
            } catch (t: Throwable) {
                android.util.Log.w("THCSApplication", "Khởi tạo nền hoặc đồng bộ ban đầu thất bại an toàn: ${t.message}")
            }
        }

        // Tự động kích hoạt đồng bộ nền khi thiết bị có lại kết nối mạng (hoặc Wi-Fi)
        container.syncNetworkChecker.observeChanges {
            appScope.launch {
                try {
                    container.syncRepository.syncNow(manual = false)
                } catch (t: Throwable) {
                    android.util.Log.w("THCSApplication", "Đồng bộ nền khi có mạng thất bại an toàn: ${t.message}")
                }
            }
        }

        // F20a: theo dõi phiên đăng nhập + công tắc thông báo để đăng ký/xóa token.
        // Khi cờ ENABLE_PUSH=false, KHÔNG khởi động gì cả để app hoạt động y như trước.
        if (BuildConfig.ENABLE_PUSH) {
            try {
                container.pushTokenManager.start(appScope)
            } catch (t: Throwable) {
                android.util.Log.w("THCSApplication", "Khởi tạo thông báo đẩy thất bại an toàn: ${t.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_STUDY_REMINDER,
                "Nhắc nhở học tập THCS",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kênh thông báo nhắc nhở giờ tự học mỗi ngày"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_STUDY_REMINDER = "study_reminder_channel"
    }
}
