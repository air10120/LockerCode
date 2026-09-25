---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: c226c988c4ae1226f6aeeb801a0caa8b_653ebea5b8ba11f1b40252540024e231
    ReservedCode1: eemPbLhY0bGrNhBesUgMcqx88VyrVpp4EmOANvWYk0iMDiD1iZq20sP2+sl4xszfRf+inR5IpDF4j/J+ce8c+zR0VwwLyn1aV/BS2vmC0Fa2LOV+Q09SSKEXlWEfqs8U67Lih7URvzVuj8DZMI2S/IEQRx+ZempMu9dRwrQSsyzwaokBTmIgcW50CgQ=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: c226c988c4ae1226f6aeeb801a0caa8b_653ebea5b8ba11f1b40252540024e231
    ReservedCode2: eemPbLhY0bGrNhBesUgMcqx88VyrVpp4EmOANvWYk0iMDiD1iZq20sP2+sl4xszfRf+inR5IpDF4j/J+ce8c+zR0VwwLyn1aV/BS2vmC0Fa2LOV+Q09SSKEXlWEfqs8U67Lih7URvzVuj8DZMI2S/IEQRx+ZempMu9dRwrQSsyzwaokBTmIgcW50CgQ=
---

# Changelog

## v1.0.12 (2026-09-25) - versionCode 13

- 稳定版发布：承接 v1.0.11 的多目标识别优化，正式上架版本。
- 多目标识别框跨帧稳定：命中缓存按取件码 upsert，未命中帧仅做过期清理，不再轮流闪烁。
- 识别框平滑跟随：按标签配对，位移小于预览宽 2% 时沿用旧坐标，减少抖动。

## v1.0.11 (2026-09-24) - versionCode 12

- 实现跨帧持久命中缓存（LinkedHashMap 按码 upsert + 1500ms 过期清理）。
- HIT_HOLD_MS=1500ms，多目标场景下识别框稳定显示。
- 构建 app-1.0.11.apk，commit d47cf54。
*（内容由AI生成，仅供参考）*
