package com.example.pickupcode

/**
 * 取件码提取器（核心正则逻辑）
 *
 * 匹配规则：
 *  1. 纯数字：4-8 位（如 123456、30011234）
 *  2. 数字 + 字母组合：3-10 位（如 A12345、B234）
 *  3. 带连字符格式：如 7-2-3001（快递柜 / 丰巢常见形式）
 *
 * 排除规则：
 *  - 11 位纯数字（手机号，如 13812345678）
 *  - 12 位及以上的长数字（订单号等）
 *  - 纯字母单词（如 hello，不可能是取件码）
 *  - 常见噪音：年份（2024、2025 等）
 */
object PickupCodeExtractor {

    /** 常见快递公司 / 驿站关键词：用于判断短信是否为快递短信，减少误报 */
    private val EXPRESS_KEYWORDS = arrayOf(
        "中通", "圆通", "顺丰", "京东", "韵达", "申通", "极兔",
        "邮政", "EMS", "菜鸟", "丰巢", "快递", "取件", "驿站",
        "速递", "包裹", "蜂巢"
    )

    /** 1. 优先匹配「取件码」字样后面紧跟的编码（快递短信几乎都带"取件码"字样） */
    private val CONTEXT_CODE_REGEX = Regex("取件码[\\s:：]*([A-Za-z0-9-]{3,10})")

    /** 2. 带连字符取件码：如 7-2-3001（三段，每段 1-3 位，末段 1-4 位） */
    private val HYPHEN_CODE_REGEX = Regex("\\b[0-9]{1,3}-[0-9]{1,3}-[0-9]{1,4}\\b")

    /** 3. 数字 + 字母混合：3-10 位，前后不能是字母或数字（避免匹配到长串的子串） */
    private val ALNUM_CODE_REGEX = Regex("(?<![A-Za-z0-9])[A-Za-z0-9]{3,10}(?![A-Za-z0-9])")

    /** 4. 纯数字：4-8 位，前后不能是数字（避免匹配到手机号 / 订单号内部的片段） */
    private val DIGIT_CODE_REGEX = Regex("(?<![0-9])[0-9]{4,8}(?![0-9])")

    /** 常见年份（识别时排除，避免把"2024"当成取件码） */
    private val YEAR_REGEX = Regex("19[5-9][0-9]|20[0-3][0-9]")

    /**
     * 从文本中提取取件码
     *
     * @param text  待分析的文本（图片 OCR 识别结果 或 短信内容）
     * @param isSms 是否为短信场景：短信场景要求文本包含快递关键词，减少误报
     * @return 取件码字符串；未找到返回 null
     */
    fun extract(text: String?, isSms: Boolean = false): String? {
        if (text.isNullOrBlank()) return null

        // 短信场景：先判断是否是快递短信（不含关键词则直接放弃，避免误提取验证码等）
        if (isSms && !containsExpressKeyword(text)) return null

        // 优先级 1：取件码字样后的编码（命中率最高）
        CONTEXT_CODE_REGEX.find(text)?.let { match ->
            val candidate = match.groupValues[1].trim()
            if (isValidCode(candidate)) return candidate
        }

        // 优先级 2：带连字符的取件码（如 7-2-3001）
        HYPHEN_CODE_REGEX.find(text)?.let { match ->
            if (isValidCode(match.value)) return match.value
        }

        // 优先级 3：数字 + 字母混合（如 A12345）
        ALNUM_CODE_REGEX.find(text)?.let { match ->
            if (isValidCode(match.value)) return match.value
        }

        // 优先级 4：纯数字 4-8 位（如 123456）
        DIGIT_CODE_REGEX.find(text)?.let { match ->
            if (isValidCode(match.value)) return match.value
        }

        // 全部未命中
        return null
    }

    /**
     * 从文本中提取【所有】取件码（扫描驿站/快递面单场景）
     *
     * 与 extract() 的区别：
     *  - extract() 只返回优先级最高的 1 个（短信场景用）
     *  - extractAll() 返回整张图片 OCR 文本中识别出的全部取件码，
     *    供「指定取件码 → 扫描 → 匹配」流程核对
     *
     * @param text 待分析的文本（图片 OCR 识别结果）
     * @return 去重后的取件码列表（保序），未识别到返回空列表
     */
    fun extractAll(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()

        // LinkedHashSet：保序 + 自动去重（同一编码可能被多个正则同时命中）
        val result = LinkedHashSet<String>()

        // 规则 1：取件码字样后的编码
        CONTEXT_CODE_REGEX.findAll(text).forEach { match ->
            val candidate = match.groupValues[1].trim()
            if (isValidCode(candidate)) result.add(candidate)
        }

        // 规则 2：带连字符取件码（如 7-2-3001）
        HYPHEN_CODE_REGEX.findAll(text).forEach { match ->
            if (isValidCode(match.value)) result.add(match.value)
        }

        // 规则 3：数字 + 字母混合（如 A12345）
        ALNUM_CODE_REGEX.findAll(text).forEach { match ->
            if (isValidCode(match.value)) result.add(match.value)
        }

        // 规则 4：纯数字 4-8 位（如 123456）
        DIGIT_CODE_REGEX.findAll(text).forEach { match ->
            if (isValidCode(match.value)) result.add(match.value)
        }

        return result.toList()
    }

    /** 判断文本是否包含快递关键词 */
    private fun containsExpressKeyword(text: String): Boolean {
        for (keyword in EXPRESS_KEYWORDS) {
            if (text.contains(keyword)) return true
        }
        return false
    }

    /**
     * 校验候选编码是否符合取件码规则
     *
     * 规则汇总：
     *  - 纯数字：仅接受 4-8 位
     *  - 含字母：3-10 位，且必须含数字（排除纯字母单词）
     *  - 排除 11 位手机号、12 位以上数字、年份
     */
    private fun isValidCode(code: String): Boolean {
        if (code.isEmpty()) return false

        // 排除 11 位纯数字（手机号）
        if (code.length == 11 && code.all { it.isDigit() }) return false

        // 排除 12 位及以上的长数字
        if (code.length >= 12) return false

        // 纯数字：仅接受 4-8 位，并排除年份
        if (code.all { it.isDigit() }) {
            return code.length in 4..8 && !YEAR_REGEX.matches(code)
        }

        // 纯字母（如 hello）：不可能是取件码
        if (code.all { it.isLetter() }) return false

        // 数字 + 字母组合（或带连字符）：3-10 位
        return code.length in 3..10
    }
}
