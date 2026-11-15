package com.example.fake

import com.example.data.seed.ContentLoader

class FakeContentLoader(
    var shouldSucceed: Boolean = true,
    var errorMessage: String = "Lỗi kết nối cơ sở dữ liệu giả lập"
) : ContentLoader {

    var callCount = 0

    override suspend fun loadIfEmpty(): Result<Unit> {
        callCount++
        return if (shouldSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(RuntimeException(errorMessage))
        }
    }
}
