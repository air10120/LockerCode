package com.example.pickupcode

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

/**
 * 最近取件码的本地存储 + 应用内广播常量
 *
 * 数据只保存在本机 SharedPreferences（应用私有数据目录），
 * 不联网、不上传、不出设备，卸载 App 时随之清除。
 *
 * 广播机制说明：
 *  SmsReceiver（静态广播接收器）收到快递短信并解析出取件码后，
 *  发送一条包内广播（setPackage 限定仅本应用接收）；
 *  SmsFragment 在前台时动态注册接收器，收到广播后实时刷新界面。
 */
object CodeRepository {

    private const val PREF_NAME = "pickup_code_pref"
    private const val KEY_LATEST_CODE = "latest_code"

    /** 应用内广播 Action：短信取件码已识别 */
    const val ACTION_SMS_CODE_FOUND = "com.example.pickupcode.ACTION_SMS_CODE_FOUND"

    /** 广播携带的取件码 Extra 键 */
    const val EXTRA_CODE = "extra_code"

    /** 保存最近一次识别的取件码 */
    fun save(context: Context, code: String) {
        prefs(context).edit().putString(KEY_LATEST_CODE, code).apply()
    }

    /** 读取最近一次识别的取件码（没有则返回 null） */
    fun getLatestCode(context: Context): String? {
        return prefs(context).getString(KEY_LATEST_CODE, null)
    }

    /** 构建「取件码已识别」的应用内广播（包内定向，其他应用收不到） */
    fun buildCodeFoundIntent(context: Context, code: String): Intent {
        return Intent(ACTION_SMS_CODE_FOUND)
            .setPackage(context.packageName)
            .putExtra(EXTRA_CODE, code)
    }

    // ==================== 扫描定位（主流程）====================

    private const val KEY_LAST_TARGET = "last_target"
    private const val KEY_LAST_CODES = "last_codes"
    private const val KEY_LAST_MATCHED = "last_matched"

    /** 最近一次扫描结果（主界面返回时展示） */
    data class LastScanResult(
        val targetCode: String,   // 用户指定的取件码
        val codes: List<String>,  // OCR 识别出的全部取件码
        val matched: Boolean      // 是否找到匹配
    )

    /** 保存最近一次扫描结果（码列表用逗号拼接存入） */
    fun saveLastScan(context: Context, targetCode: String, codes: List<String>, matched: Boolean) {
        prefs(context).edit()
            .putString(KEY_LAST_TARGET, targetCode)
            .putString(KEY_LAST_CODES, codes.joinToString(","))
            .putBoolean(KEY_LAST_MATCHED, matched)
            .apply()
    }

    /** 读取最近一次扫描结果（没有则返回 null） */
    fun getLastScan(context: Context): LastScanResult? {
        val target = prefs(context).getString(KEY_LAST_TARGET, null) ?: return null
        val codesRaw = prefs(context).getString(KEY_LAST_CODES, "").orEmpty()
        val codes = codesRaw.split(",").filter { it.isNotBlank() }
        val matched = prefs(context).getBoolean(KEY_LAST_MATCHED, false)
        return LastScanResult(target, codes, matched)
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}
