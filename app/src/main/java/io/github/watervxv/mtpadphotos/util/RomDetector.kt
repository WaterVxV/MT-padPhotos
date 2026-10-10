package io.github.watervxv.mtpadphotos.util

import android.os.Build

/**
 * ROM 系列检测 + 开机自启/省电白名单引导文案（REQUIREMENTS §3.6 / M3-2）。
 * 运行时通过 Build.MANUFACTURER / Build.BRAND / Build.MODEL 判定，无需任何权限。
 */
enum class RomFamily(val displayName: String) {
    XIAOMI("小米 / 红米（MIUI / HyperOS）"),
    OPPO("OPPO / Realme / 一加（ColorOS）"),
    VIVO("vivo / iQOO（OriginOS）"),
    HUAWEI("华为（HarmonyOS / EMUI）"),
    HONOR("荣耀（MagicOS）"),
    MEIZU("魅族（Flyme）"),
    SAMSUNG("三星（One UI）"),
    OTHER("其他 / 原生 Android")
}

data class RomGuide(
    val family: RomFamily,
    val autostartSteps: List<String>,
    val batterySteps: List<String>,
    val note: String
)

object RomDetector {

    fun detect(): RomFamily {
        val combined = listOf(Build.MANUFACTURER, Build.BRAND, Build.MODEL)
            .map { it?.lowercase() ?: "" }
            .joinToString(" ")
        return when {
            "xiaomi" in combined || "redmi" in combined || "poco" in combined -> RomFamily.XIAOMI
            "oppo" in combined || "realme" in combined || "oneplus" in combined -> RomFamily.OPPO
            "vivo" in combined || "iqoo" in combined -> RomFamily.VIVO
            "huawei" in combined || "harmony" in combined -> RomFamily.HUAWEI
            "honor" in combined -> RomFamily.HONOR
            "meizu" in combined -> RomFamily.MEIZU
            "samsung" in combined -> RomFamily.SAMSUNG
            else -> RomFamily.OTHER
        }
    }

    fun currentGuide(): RomGuide = guideFor(detect())

    fun guideFor(family: RomFamily): RomGuide = when (family) {
        RomFamily.XIAOMI -> RomGuide(
            family = family,
            autostartSteps = listOf(
                "打开「设置」→「应用设置」→「自启动」，找到「MT轮播相册」并开启自启动",
                "部分机型入口为「安全中心」→「授权管理」→「自启动管理」"
            ),
            batterySteps = listOf(
                "打开「设置」→「省电与电池」→「应用智能省电」，将本应用设为「无限制」",
                "如弹出「允许后台运行」等提示，选择允许"
            ),
            note = "设置完成后重启平板验证：开机后应自动进入上次播放的相册并开始轮播。"
        )
        RomFamily.OPPO -> RomGuide(
            family = family,
            autostartSteps = listOf(
                "打开「设置」→「应用管理」→「自启动管理」，允许本应用自启动"
            ),
            batterySteps = listOf(
                "打开「设置」→「电池」→「应用耗电管理」，关闭本应用的「应用冻结」"
            ),
            note = "设置完成后重启平板验证自启效果。"
        )
        RomFamily.VIVO -> RomGuide(
            family = family,
            autostartSteps = listOf(
                "打开「i管家」→「应用管理」→「自启动管理」，允许本应用自启动"
            ),
            batterySteps = listOf(
                "打开「设置」→「电池」→「后台耗电管理」，将本应用设为「允许后台耗电」"
            ),
            note = "设置完成后重启平板验证自启效果。"
        )
        RomFamily.HUAWEI -> RomGuide(
            family = family,
            autostartSteps = listOf(
                "打开「设置」→「应用和服务」（旧版为「应用」）→「应用启动管理」",
                "找到「MT轮播相册」，关闭「自动管理」",
                "手动开启「自启动」「关联启动」「后台活动」三个开关"
            ),
            batterySteps = listOf(
                "打开「设置」→「电池」→「更多电池设置」，确认无针对本应用的休眠限制",
                "如开机后仍被杀后台，回到「应用启动管理」确认三个开关均为手动允许状态"
            ),
            note = "设置完成后重启平板验证自启效果。"
        )
        RomFamily.HONOR -> RomGuide(
            family = family,
            autostartSteps = listOf(
                "打开「设置」→「应用」→「应用启动管理」",
                "找到「MT轮播相册」，关闭「自动管理」",
                "手动开启「自启动」和「后台活动」"
            ),
            batterySteps = listOf(
                "打开「设置」→「电池」→「应用耗电管理」，允许本应用后台活动"
            ),
            note = "设置完成后重启平板验证自启效果。"
        )
        RomFamily.MEIZU -> RomGuide(
            family = family,
            autostartSteps = listOf(
                "打开「手机管家」→「权限管理」→「自启动管理」，允许本应用自启动"
            ),
            batterySteps = listOf(
                "打开「设置」→「电池」→「应用耗电管理」，关闭本应用的智能休眠限制"
            ),
            note = "设置完成后重启平板验证自启效果。"
        )
        RomFamily.SAMSUNG -> RomGuide(
            family = family,
            autostartSteps = listOf(
                "打开「设置」→「应用程序」→「自动运行」，开启本应用的自动运行"
            ),
            batterySteps = listOf(
                "打开「设置」→「电池」→「应用程序耗电管理」，关闭「让应用进入休眠」"
            ),
            note = "设置完成后重启平板验证自启效果。"
        )
        RomFamily.OTHER -> RomGuide(
            family = family,
            autostartSteps = listOf(
                "原生 Android 无需额外设置开机自启，安装后默认生效"
            ),
            batterySteps = listOf(
                "如遇后台被杀，请在系统电池优化中将本应用设为「不优化」"
            ),
            note = "设置完成后重启平板验证自启效果。"
        )
    }
}
