# LINE Bot 英文學習機器人 - 系統分析設計書 (SA)

## 1. 系統架構總覽 (System Architecture)

系統採用輕量級微服務架構，基於 Java 17 與 Spring Boot 構建，透過 LINE Webhook 作為唯一對外溝通入口。

```text
[ LINE Platform ] 
       │ (HTTPS / POST Webhook)
       ▼
[ Spring Boot 後端應用 ] ─────► [ Redis 快取 ] (狀態管理、對話上下文、TTS快取)
       │
       ├────► [ PostgreSQL / SQLite ] (持久化儲存：用戶、紀錄、題庫)
       │
       ├────► [ Google Gemini API ] (AI 生成層：對話、糾錯解析)
       │
       └────► [ Google Cloud TTS API ] (語音生成層)
```

---

## 2. 狀態機與對話模式管理 (State Management)

由於 LINE Bot 是無狀態 (Stateless) 的 Webhook 設計，系統需依賴 `user_id` 在 Redis (或 DB) 中維護「對話狀態 (Session State)」，以正確路由使用者訊息。

### 2.1 定義狀態列舉 (State Enum)
| 狀態代碼 | 說明 | 觸發條件 | 結束條件 |
|---|---|---|---|
| `NORMAL` | 一般指令模式 | 預設狀態，或結束其他模式時 | 使用者輸入特定進入指令 |
| `QUIZ_MODE` | 測驗模式 (5題) | 輸入「開始測驗」 | 完成 5 題，或中途輸入保留字 |
| `ROLEPLAY_MODE` | 情境對話練習 | 輸入「對話練習」 | 完成 5 回合，或輸入「結束對話」 |
| `FREE_CHAT_MODE` | AI 自由聊天 | 輸入「AI聊天」 | 輸入「結束聊天」 |

### 2.2 訊息路由邏輯 (Message Routing)
1. 收到 `MessageEvent`。
2. 查詢 Redis 取得使用者目前 `State`。
3. 若為 `NORMAL`，解析關鍵字 (如：今日單字、糾錯...)，執行單次回覆。
4. 若為 `FREE_CHAT_MODE`，檢查文字是否為「結束聊天」。若否，將訊息加入歷史上下文，傳送至 Google Gemini API 處理，產生回覆與語法更正。

---

## 3. 核心 API 與資料傳輸設計

### 3.1 Webhook Controller
- **Endpoint**: `POST /api/line/webhook`
- **Security**: 必須驗證 `X-Line-Signature`。
- **Timeout**: LINE Server 要求 Webhook 必須在短時間內 (通常 < 1-2 秒) 回傳 `200 OK`。
- **非同步處理**: 針對需要呼叫 Gemini API 的重度任務 (`FREE_CHAT_MODE`, `ROLEPLAY_MODE`)，Webhook Controller 需立即回覆 `200 OK`，交由底層 `@Async` Thread Pool 執行，完成後使用 LINE `Reply API` (使用 ReplyToken) 發送結果。若超時，改用 `Push API`。

### 3.2 AI 糾錯與聊天回覆規格 (Gemini Structured Output)
為確保後端能精準剖析 AI 回覆並組合 LINE 訊息，要求 Google Gemini API 啟用 `responseMimeType: "application/json"`，或在 System Prompt 強制以 JSON Schema 輸出 (確保 `FR-005` 實作)。

**AI 自由聊天 (Free Chat) JSON Schema 範例：**
```json
{
  "type": "object",
  "properties": {
    "ai_reply_en": { "type": "string", "description": "AI 對使用者的英文回覆" },
    "has_grammar_error": { "type": "boolean", "description": "使用者上一句是否包含文法錯誤" },
    "corrected_user_text": { "type": "string", "description": "若有錯誤，輸出的修正句 (若無則留空)" },
    "correction_note_zh": { "type": "string", "description": "純繁體中文的簡短修正解釋 (若無則留空)" }
  },
  "required": ["ai_reply_en", "has_grammar_error"]
}
```

---

## 4. 資料庫實體與型別設計 (Data Dictionary)

基於 PM 規格延伸物理資料表的型別與限制。

### 4.1 `users`
| 欄位名稱 | 型別 | 屬性 | 說明 |
|---|---|---|---|
| `id` | BIGINT | PK, Auto Inc | 系統內部 ID |
| `line_user_id` | VARCHAR(64) | Unique, Index | LINE 提供的識別碼 |
| `cefr_level` | VARCHAR(10) | Nullable | 預設 A2 |
| `created_at` | TIMESTAMP | | |

### 4.2 `chat_messages` (紀錄自由聊天歷程)
| 欄位名稱 | 型別 | 屬性 | 說明 |
|---|---|---|---|
| `id` | BIGINT | PK | |
| `user_id` | BIGINT | FK, Index | |
| `session_id` | VARCHAR(36) | Index | UUID，用來群組同一回合的聊天 |
| `user_text` | TEXT | | 使用者原始輸入 |
| `corrected_text` | TEXT | Nullable | AI 修正後的句子 |
| `correction_note_zh`| VARCHAR(255) | Nullable | |
| `ai_reply_text` | TEXT | | AI 回覆內容 |
| `created_at` | TIMESTAMP | Index | 用於清除過期資料 (90天限制) |

### 4.3 `audio_cache` (TTS 快取)
| 欄位名稱 | 型別 | 屬性 | 說明 |
|---|---|---|---|
| `text_hash` | VARCHAR(64) | PK/Unique | `SHA-256(text + voice_type + speed)` |
| `audio_url` | VARCHAR(512)| | 存放於 S3 或 Blob 的公開網址 |
| `expires_at` | TIMESTAMP | Index | 網址過期時間 |

---

## 5. 異常處理與降級策略 (Exception & Fallback)

為了滿足 `NFR-004` (可用性) 與 `BR-004`，系統必須做斷路與降級：

1. **Google Gemini API 超時或 5xx 錯誤**
   - **行為**: 重試 1 次 (搭配 Exponential Backoff)。
   - **降級**: 若重試失敗，清除 Reply Token，回傳純文字：「伺服器目前學習量較大，請稍後再試🙏」。
   
2. **Google Cloud TTS 額度耗盡或超時**
   - **行為**: Catch HTTP/Quota Exception。
   - **降級**: 取消回傳 Audio Message，改為純文字 (利用 `備援文字` 欄位)：「目前語音功能維護中，單字發音音標為：[KK 音標]」。

3. **LINE ReplyToken 過期 (超過 1 分鐘)**
   - **行為**: 當 AI 生成過慢，ReplyToken 已經失效。
   - **處理**: 捕捉 LINE SDK 的 API 錯誤，自動 fallback 轉換為 `Push API`，將訊息成功遞送給使用者。

---

## 6. 定期排程任務 (Cron Jobs)

1. **清理對話資料 (Data Retention)**
   - **排程**: 每日凌晨 03:00 (UTC+8)
   - **動作**: `DELETE FROM chat_messages WHERE created_at < NOW() - INTERVAL '90 DAYS'`
2. **快取清理 (Cache Eviction)**
   - **排程**: 每小時
   - **動作**: 刪除 `audio_cache` 中 `expires_at` 已到期的紀錄與實際靜態檔案。