// 版本矩阵锁死在 libs.versions.toml（CLI 构建无 Android Studio 兜底，见计划 §5 R9）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
