package com.example.pickupcode

/**
 * 取件码容错匹配器（增强版：字符容错开关）
 *
 * OCR（光学字符识别）对相似字符经常误识别，例如：
 *  - 数字 0 被识别成字母 O / o
 *  - 数字 1 被识别成字母 I / l / i
 *  - 数字 5 被识别成字母 S / s
 *  - 数字 8 被识别成字母 B / b
 *  - 数字 2 被识别成字母 Z / z
 *  - 数字 6 被识别成 b / g
 *  - 数字 9 被识别成 g / q
 *
 * 容错匹配规则（开启开关时）：
 *  1. 忽略大小写精确匹配 → 直接命中
 *  2. 长度必须一致（取件码本身长度很短，长度不同大概率不是同一个码）
 *  3. 混淆字符（如 0↔o）之间的差异不计入差异数
 *  4. 其余字符差异最多允许 1 个
 *
 * 关闭开关时：忽略大小写精确匹配（严格模式）。
 */
object CodeMatcher {

    /** OCR 常见混淆字符表：key 字符可被列表中的任一字符替换（全部按小写存储，比较时统一小写） */
    private val CONFUSABLE_MAP: Map<Char, List<Char>> = mapOf(
        '0' to listOf('o', 'd'),
        'o' to listOf('0', 'd'),
        'd' to listOf('0', 'o', '6'),
        '1' to listOf('i', 'l'),
        'i' to listOf('1', 'l'),
        'l' to listOf('1', 'i'),
        '5' to listOf('s'),
        's' to listOf('5'),
        '8' to listOf('b'),
        'b' to listOf('8', '6'),
        '6' to listOf('b', 'g'),
        '2' to listOf('z'),
        'z' to listOf('2'),
        '9' to listOf('g', 'q'),
        'g' to listOf('9', 'q', '6'),
        'q' to listOf('9'),
        '7' to listOf('t'),
        't' to listOf('7'),
        '4' to listOf('a'),
        'a' to listOf('4')
    )

    /**
     * 判断识别码是否与指定取件码匹配
     *
     * @param target   用户输入的指定取件码
     * @param candidate OCR 识别出的取件码
     * @param fuzzy    是否开启字符容错（true 开启 / false 严格精确匹配）
     * @return true 表示匹配
     */
    fun matches(target: String, candidate: String, fuzzy: Boolean): Boolean {
        if (target.isBlank() || candidate.isBlank()) return false

        // 严格模式：忽略大小写精确匹配
        if (!fuzzy) return target.equals(candidate, ignoreCase = true)

        // 容错模式：先尝试精确匹配（含大小写不敏感）
        if (target.equals(candidate, ignoreCase = true)) return true

        // 长度必须一致
        if (target.length != candidate.length) return false

        // 逐字符比较：混淆字符不计差异，其余差异最多允许 1 个
        var diffCount = 0
        for (i in target.indices) {
            val tc = target[i].lowercaseChar()
            val cc = candidate[i].lowercaseChar()
            if (tc == cc) continue

            // 混淆字符（如 0 vs o）：不算差异
            if (CONFUSABLE_MAP[tc]?.contains(cc) == true) continue

            diffCount++
            if (diffCount > 1) return false
        }
        return true
    }
}
