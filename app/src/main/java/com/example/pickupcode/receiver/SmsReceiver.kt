package com.example.pickupcode.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.pickupcode.CodeRepository
import com.example.pickupcode.PickupCodeExtractor

/**
 * 短信广播接收器（核心：自动拦截快递短信并提取取件码）
 *
 * 工作流程：
 *  1. 系统收到新短信时广播 SMS_RECEIVED，本接收器被系统唤醒
 *  2. 拼接短信全部内容
 *  3. 调用 PickupCodeExtractor.extract(content, isSms = true) 提取取件码
 *     （isSms = true 表示必须包含"中通/圆通/顺丰/京东/快递/取件"等关键词，
 *       避免把普通验证码短信误判为取件码）
 *  4. 提取成功：
 *     - 保存到本机 SharedPreferences（App 打开短信页时展示最近取件码）
 *     - 发送包内广播通知前台界面实时刷新
 *
 * 注意：
 *  - Android 11 起非默认短信应用无法读取历史短信收件箱，
 *    因此本应用只依赖 SMS_RECEIVED 广播实时接收新短信，不查询历史短信。
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 只处理短信到达广播
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // 取出短信内容（一条短信可能被拆分多条，需要拼接）
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val builder = StringBuilder()
        for (message in messages) {
            builder.append(message.messageBody)
        }
        val fullText = builder.toString()

        // 提取取件码（短信场景：要求包含快递关键词）
        val code = PickupCodeExtractor.extract(fullText, isSms = true)
        if (code != null) {
            // 1. 保存最近取件码到本机
            CodeRepository.save(context, code)

            // 2. 发送包内广播，App 在前台时 SmsFragment 会实时刷新展示
            context.sendBroadcast(CodeRepository.buildCodeFoundIntent(context, code))

            Log.d("SmsReceiver", "识别到取件码: $code")
        }
    }
}
