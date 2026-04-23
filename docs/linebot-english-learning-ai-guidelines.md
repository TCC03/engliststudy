# LINE Bot 英文學習機器人 - AI 開發條件與錯誤處理規範

## 1. AI 服務對接基礎設定

### 1.0 開發模型與執行模型分離設定
- 系統執行環境 (Runtime) 使用 Gemini，開發輔助 (Developer Assistant) 使用 GPT。
- 建議在環境變數明確定義，避免團隊誤用：

```env
AI_PROVIDER=gemini
DEV_ASSISTANT=gpt
```

### 1.1 選擇模型與參數
系統採用分層模型策略，優先使用頂尖模型，遇到故障自動降級。

#### 主力模型（優先級 1）
- **模型**：GPT-5.3-codex (或 Claude 3.5 Sonnet)
- **優勢**：具備最頂尖的上下文理解能力、高推理能力與最穩定的結構化輸出 (Structured Outputs) 表現，能完美應對雙語 (中/英) 語法糾錯與自然流暢的情境對話。

#### 備用模型（優先級 2）
- **模型**：GPT-4o 或 Claude 3 Haiku
- **觸發條件**：主力模型在以下情況下自動降級
  - API 連續 429 錯誤（Rate Limited）
  - 連續 500+ 伺服器錯誤達 3 次
  - Structured Output 失敗（refusal）
- **降級機制**：後端維護 Redis Key `ai_model_fallback_status`，紀錄目前使用的模型與切換時間戳。
- **恢復機制**：每 5 分鐘嘗試 1 次主力模型，若成功則立即切換回來。
- **基礎參數配置 (Generation Config)**：
  - `temperature`: 0.3 - 0.5 (確保文法修正的正確性與邏輯一致性，聊天時可略微調高至 0.6)
  - `max_tokens`: 800 (給予足夠長度應付詳細的語法解釋與對話)

### 1.2 強制結構化輸出 (Structured Output)
確保從 API 拿到的結果不會混雜 Markdown 或前後文閒聊，後端必須要求 JSON 返回。
- **作法**：在 API 請求中啟用 **Structured Outputs** (例如使用 `response_format: { type: "json_schema", json_schema: {...} }`)。
- **防呆**：設定 `strict: true` 來保證模型 100% 遵守定義的欄位，免去後端額外的 JSON parse 錯誤處理。

---

## 2. 核心 AI 任務與 System Prompt 規範

### 2.1 句子糾錯 (Sentence Correction)
- **觸發指令**：`糾錯 [句子]` 
- **System Prompt 核心規則**：
  1. 你是一個專業的英文老師，對象是 A2-B1 程度的台灣學生。
  2. 修正使用者的文法、拼字與用詞錯誤，但「盡量保留使用者原本的語意與單字」，不要做不必要的過度改寫。
  3. 解釋必須使用「繁體中文」，並限制在 2 點以內。
  4. 提供一句更道地/自然的說法 (Natural Alternative)。
- **JSON Schema (Response)**：
  ```json
  {
    "original_text": "string",
    "corrected_text": "string",
    "error_explanations": ["string"],
    "natural_alternative": "string"
  }
  ```

### 2.2 自由聊天與即時語法更正 (Free Chat with Correction)
- **觸發指令**：`AI聊天` (進入模式)
- **System Prompt 核心規則**：
  1. 你是一個友善的英文聊天夥伴。
  2. 每一輪對話請回應使用者的話題，維持對話熱度，字數不超過 30 字，全英文回覆 (`ai_reply_en`)。
  3. 檢查使用者**上一句話**是否有文法或用字錯誤，若有，提供修正後的正確句子 (`corrected_user_text`) 與簡短繁體中文解釋 (`correction_note_zh`)。
  4. 如果使用者語意不清（例如只輸入單字），請用英文發問釐清。
  5. 嚴禁在此模式中主動結束對話。
- **JSON Schema (Response)**：
  ```json
  {
    "has_grammar_error": "boolean",
    "corrected_user_text": "string | null",
    "correction_note_zh": "string | null",
    "ai_reply_en": "string"
  }
  ```

---

## 3. 錯誤處理與防護機制 (Error Handling & Retry Policy)

網路請求、API 限流、內容過濾均可能導致 AI 呼叫失敗，後端必須實作以下機制以保證 LINE Bot 的可用性 (SLA)。

### 3.1 錯誤類型定義與應對策略

| 錯誤類型 (Error Type) | 常見 HTTP Status | 發生原因 | 系統處理策略 (Action) | LINE 回覆 (Fallback Message給用戶) |
|---|---|---|---|---|
| **超時 (Timeout)** | 408 / Socket Timeout | API 回應過慢、網路抖動 | 重試 1 次 (逾時設定為 8s)。若失敗，中斷流程。 | 「AI 老師目前思考比較久，請稍後再試一次喔🙏」 |
| **服務不可用 (Server Error)** | 500, 502, 503, 504 | OpenAI 伺服器異常 | 重試 1 次 (Exponential Backoff)。若失敗，記錄 Error Log 並發送告警。 | 「系統連線異常，正在聯絡工程師搶修中🛠️」 |
| **限流 (Rate Limit Exceeded)**| 429 | 短期內 Request 過多、Quota 耗盡 | 延遲 2 秒後重試 1 次。若失敗，降級為忙碌宣告。 | 「目前太多人在跟 AI 老師講話，請等個 1 分鐘再來喔！」 |
| **內容過濾 (Safety Blocked)** | 400 (FinishReason: content_filter) | 內容觸發安全審查 | **不重試**。記錄 User ID 與對話紀錄 (供人工查閱)。 | 「這句話好像包含了不適當的內容，AI 老師無法回應喔🚫」 |
| **結構化輸出失敗 (Refusal)** | 200 (但 refusal 欄位有值) | 模型拒絕產生符合 Schema 的結果 | **不重新呼叫 AI**。後端記錄 refusal 原因。 | 「AI 老師剛剛吃螺絲了，能請你換個說法再說一次嗎？」 |

### 3.2 串接 LINE Reply Token 生命週期保護
- **問題**：LINE Webhook 的 `replyToken` 只有非常短的時效 (通常數秒到一分鐘內)，且 Webhook 必須盡快給予 `200 OK` 否則 LINE 會重發。
- **機制**：
  1. 收到 Webhook，先立即非同步化 (丟入 Message Queue / `@Async` Worker) 處理 AI 請求，Controller 先回 `200 OK` 給 LINE。
  2. 若 AI 處理總時間超過 `replyToken` 時效，後端在呼叫 Reply API 捕捉到 `400 Bad Request` 時，必須自動 Catch Exception，並利用 `Push API` (帶入 `line_user_id`) 將結果「推」給使用者。

### 3.3 防止惡意消耗與長度控制
- **Token 限制**：為確保回覆品質與響應時間，使用者單次輸入建議限制在 1000 字元以內。
- **對話歷史長度 (Context Window)**：自由聊天模式中，可攜帶**最近 10 輪 (20條訊息)** 交給模型，以維持深度的對話上下文。
- **頻率限制 (Rate Limiting)**：在 Redis 中針對 `user_id` 設置計數器。AI 聊天模式下，同一使用者每分鐘不得超過發言 15 次，以防系統被惡意腳本佔用連線池。

---

## 4. 品質監控 (Monitoring)

- 當發生 Structured Output 失敗 (例如收到 `refusal`) 或是極罕見的 JSON 解析錯誤時，必須將「AI 原始回應字串」與「Prompt」印在 Error Log 中，供後續調整 System Prompt 或 Schema 使用。
- 監控 `ai_model_fallback_status` 的狀態，當切換為備用模型時，應記錄 Alert Log，並通知開發團隊查明主力模型的問題。

---

## 5. 本地開發與集成測試 (Development & Testing)

### 5.1 開發環節測試要求
- **Unit Test**：所有 AI 相關的 Service 類別（如 `SentenceCorrectionService`、`FreeChatService`）必須有 JUnit 單元測試，覆蓋率 >= 80%。
- **Mock AI 測試**：測試時應使用 Mock 物件模擬 OpenAI API 回應，驗證：
  - 正常回應解析
  - 異常情況的 fallback（如 JSON 驗證失敗）
  - Timeout 與 Retry 邏輯
  - 模型自動切換的邏輯
- **集成測試**：需驗證 LINE Webhook 與 AI 服務的端到端流程。

### 5.2 交付前驗收標準 (UAT)
開發完成後，必須通過以下測試項目才能交付上線：

| 測試項目 | 預期結果 | 驗收條件 |
|---|---|---|
| **AI 糾錯 - 正常句子** | 應回傳 JSON Schema，`corrected_text` 與 `original_text` 相同，無錯誤解釋 | 通過 |
| **AI 糾錯 - 有文法錯誤** | 應回傳正確修正句子與 1-2 點中文解釋 | 解釋準確度 >= 90% |
| **AI 糾錯 - 邊界條件** | 超長輸入 (1000 chars)、特殊符號、空值 | 應適當截斷或回復適當提示，不拋例外 |
| **自由聊天 - 正常對話** | Bot 逐輪回覆，語法修正欄位正確填充 | 通過 5 輪對話，最後正確總結 |
| **自由聊天 - 離開模式** | 使用者輸入「結束聊天」，系統回傳總結並回到 NORMAL 狀態 | 通過 |
| **AI 模型 Fallback** | 主力模型故障時，自動降級到備用模型，使用者無感知 | 降級成功，無錯誤訊息返回用戶 |
| **Timeout 與 Retry** | AI 呼叫超過 8 秒，系統重試 1 次；失敗則回傳 fallback 訊息 | 訊息正確，無 500 錯誤洩漏給用戶 |
| **LINE Webhook 簽章驗證** | 非法簽章應被拒絕 (400)，合法簽章應接受 | 通過 |
| **壓力測試** | 短時間內發送 100 個併發訊息，監控模型切換與恢復 | 不應出現超過 5% 的失敗率，恢復時間 < 5 分鐘 |
| **資料保留政策** | 對話資料應在 90 天後自動刪除 | DB 驗證，無遺留資料 |

### 5.3 交付前檢查清單 (Pre-Deployment Checklist)
- [ ] 所有 Unit Test 通過且涵蓋率 >= 80%
- [ ] 所有 Integration Test 通過
- [ ] 上表所有 UAT 項目均已驗證通過
- [ ] 代碼已過 Code Review（至少 2 位 Reviewer）
- [ ] System Prompt 已由 PM 或語言專家最後確認
- [ ] Error Log 字樣均為友善用戶用語 (無技術術語洩漏)
- [ ] Redis、DB 連線字串已更新到 Production 環境
- [ ] Monitoring Alert 已設置 (模型 Fallback、5xx 錯誤、超時等)
- [ ] LINE Webhook URL 與 Channel ID 已配置正確
- [ ] 緊急 Rollback 計劃已備妥

如有任何測試失敗，必須回到開發階段修正，不得跳過直接上線。