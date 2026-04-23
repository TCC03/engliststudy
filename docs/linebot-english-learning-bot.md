# LINE Bot 英文學習機器人專案規格 v1.1

## 1. 專案目標
建立一個可在 LINE 上互動的英文學習機器人，提供每日可完成的學習任務（單字、測驗、文法、語音、AI 糾錯、對話練習），提升使用者的持續學習率與英文表達正確度。

### 1.1 成功指標（MVP 上線後 30 天）
- D7 留存率 >= 25%
- 平均每位活躍使用者每週完成 >= 3 次學習任務
- AI 糾錯後同類錯誤再犯率下降 >= 20%
- 主要功能可用率（成功回覆率）>= 99.0%

## 2. 目標使用者
- 主要族群：A2-B1 程度，想用零碎時間練英文的學生與上班族
- 次要族群：A1（需要慢速語音與簡化解釋）
- 使用動機：通勤、午休、睡前 5-10 分鐘微學習

## 3. 核心功能（MVP）

### 3.1 每日單字
- 使用者輸入「今日單字」
- 回傳：
  - 單字
  - 詞性
  - 中文意思
  - 例句（英文）
  - 例句翻譯（中文）
  - 難度等級（A1-B1）

### 3.2 單字測驗
- 使用者輸入「開始測驗」
- 題型：四選一
- 每次 5 題
- 測驗結束後顯示：
  - 分數
  - 答錯題目解析
  - 建議複習清單
- 出題規則：
  - 優先抽出「最近答錯」題目
  - 同一題 7 天內最多重複 2 次
  - 題目需帶難度與主題標籤

### 3.3 文法小教室
- 使用者輸入「文法」
- 回傳一則文法重點（中文 + 英文例句）
- 提供 2 題練習題（選擇題或改錯題）
- 文法路徑（MVP）：
  - Week 1：現在簡單式 / be 動詞
  - Week 2：過去式 / 常見不規則動詞
  - Week 3：未來式 / 情態動詞 can, should
  - Week 4：介系詞 in, on, at / 時間表達

### 3.4 學習紀錄
- 使用者輸入「我的進度」可查詢：
  - 完成測驗次數
  - 平均分數
  - 最近一次學習時間
  - 各能力面向分數（字彙 / 文法 / 句型）
  - 最近 7 天學習天數

### 3.5 語音播放（TTS）
- 使用者可要求機器人播放單字或例句發音
- 支援情境：
  - 單字發音（美式）
  - 例句發音（正常語速）
  - 例句慢速發音（初學者）
- 機器人回覆 LINE Audio Message
- 音訊規範：
  - 格式：m4a 或 mp3
  - 時長：建議 <= 30 秒
  - 音檔 URL 需可公開讀取且在有效期內
- 若語音服務暫時不可用，回覆文字備援內容

### 3.6 AI 對話糾錯
- 使用者輸入英文句子進行文法與用字修正
- 指令格式：
  - `糾錯 I goed to school yesterday.`
  - `糾錯(簡單) She don't like coffee.`
- 機器人回傳：
  - 原句
  - 修正句
  - 錯誤重點說明（中文，1-2 點）
  - 可替換的自然說法 1 句
- 可指定難度：
  - 簡單修正（A1-A2）
  - 一般修正（B1）
- AI 回覆格式（後端強制 schema）：
  - original_text
  - corrected_text
  - error_explanations[]
  - natural_alternative
  - cefr_level
- 若 AI 服務異常，回覆「目前無法糾錯，請稍後再試」

### 3.7 對話練習
- 使用者輸入「對話練習」進入情境式練習
- MVP 情境：
  - 點餐
  - 自我介紹
  - 問路
- 每回合流程：
  1. Bot 給情境提示
  2. User 回答一句英文
  3. Bot 給 1 句鼓勵 + 1 句修正建議
  4. 累積 5 回合後總結
- 結束輸出：
  - 本次對話分數（0-100）
  - 常見錯誤 Top 3
  - 下次建議練習主題

### 3.8 AI 自由聊天（含即時語法更正）
- 使用者輸入「AI聊天」啟用自由聊天模式
- 使用者可用中英混合輸入，Bot 以英文為主回覆
- 每一輪回覆需包含：
  - AI 回覆內容（英文）
  - 使用者上一句的語法修正（若有錯）
  - 1 句簡短中文說明（為何要這樣改）
- 修正策略：
  - 錯誤少時只微調，不過度改寫
  - 若語意不清，先提出澄清問題
- 使用者輸入「結束聊天」離開模式
- 結束時輸出：
  - 本次聊天回合數
  - 常見錯誤類型 Top 3
  - 建議下次練習主題

## 4. 指令設計
- 今日單字
- 開始測驗
- 文法
- 我的進度
- 單字語音 [單字]
- 例句語音 [句子]
- 糾錯 [英文句子]
- 糾錯(簡單) [英文句子]
- 對話練習
- AI聊天
- 結束聊天
- 幫助

## 5. 對話流程（簡化）
1. 使用者傳送訊息
2. Webhook 接收 LINE Event
3. 後端解析指令與參數
4. 路由到對應服務（單字、測驗、文法、語音、AI 糾錯、對話練習、AI 自由聊天）
5. 回覆 LINE 訊息
6. 寫入學習紀錄與事件日誌

## 6. 技術建議
- 語言：Java 17+
- 後端框架：Spring Boot
- LINE SDK：line-bot-sdk-java（或 line-bot-spring-boot）
- 語音服務（TTS）：
  - Google Cloud Text-to-Speech（建議）
  - 或 Azure Speech Service
- AI 服務（糾錯/對話）：
  - Google Gemini API
  - 使用固定 System Prompt + JSON Schema 驗證
- 資料庫：
  - MVP：SQLite
  - Production：PostgreSQL
- 快取：Redis（可選，用於語音與題目快取）
- 部署：Render、Railway 或 Azure Web App

## 7. 資料表設計（範例）

### users
- id (PK)
- line_user_id (unique)
- cefr_level
- created_at

### learning_records
- id (PK)
- user_id (FK)
- quizzes_taken
- avg_score
- vocab_score
- grammar_score
- pattern_score
- last_study_at

### vocabulary
- id (PK)
- word
- pos
- meaning_zh
- example_en
- example_zh
- level
- topic_tag

### quiz_items
- id (PK)
- vocab_id (FK)
- question_text
- option_a
- option_b
- option_c
- option_d
- correct_option
- difficulty

### correction_logs
- id (PK)
- user_id (FK)
- original_text
- corrected_text
- error_type
- cefr_level
- created_at

### conversation_sessions
- id (PK)
- user_id (FK)
- scenario
- turn_count
- score
- summary
- created_at

### chat_messages
- id (PK)
- user_id (FK)
- session_id (FK)
- user_text
- corrected_text
- correction_note_zh
- ai_reply_text
- created_at

### audio_cache（可選）
- id (PK)
- text_hash (unique)
- text_content
- voice_type
- speed
- audio_url
- expires_at
- created_at

## 8. 非功能需求
- API 平均回應時間 < 2 秒（不含 AI/TTS）
- 語音生成功能平均回應時間 < 4 秒（含快取）
- AI 糾錯功能平均回應時間 < 5 秒
- 月可用性（SLA）>= 99.5%
- 5xx 錯誤率 < 1%
- 錯誤訊息友善（例如：指令不支援時提示「輸入 幫助 查看功能」）
- 個資最小化：不儲存不必要個人資訊
- 對話資料預設保存 90 天，可由使用者要求刪除

## 9. 分析與追蹤事件
- command_used
- quiz_started
- quiz_completed
- correction_requested
- conversation_started
- conversation_completed
- tts_requested
- ai_chat_started
- ai_chat_turn_completed
- ai_chat_ended

## 10. 開發里程碑

### Milestone 1（3-4 天）
- 建立 LINE Messaging API channel
- 完成 webhook 串接
- 實作「幫助」「今日單字」
- 建立基本資料表

### Milestone 2（3-4 天）
- 完成「開始測驗」與分數計算
- 完成「我的進度」
- 加入事件追蹤

### Milestone 3（3-4 天）
- 實作「文法」內容與練習題
- 新增「單字語音 / 例句語音」
- 完成 audio cache

### Milestone 4（4-5 天）
- 新增「AI 對話糾錯 / 對話練習 / AI 自由聊天」
- JSON schema 驗證與異常降級策略
- 壓力測試與部署上線

## 11. 測試與交付標準

### 11.1 功能驗收標準（Functional Acceptance）
- 可成功接收與回覆 LINE 訊息
- 12 個主要指令可正常使用
- 測驗可計分並回傳解析
- 可成功播放單字與例句語音
- 可針對使用者英文句子提供 AI 糾錯與說明
- 對話練習可完成 5 回合並輸出總結
- AI 自由聊天模式可提供逐輪語法更正與說明
- 可查詢個人學習進度

### 11.2 效能與可靠性驗收標準
- 服務穩定運作 24 小時無中斷 (SLA >= 99.0%)
- 平均 API 回應時間 < 2 秒（不含 AI/TTS）
- AI 服務回應時間 < 5 秒；TTS 回應時間 < 4 秒
- 5xx 錯誤率 < 1%

### 11.3 AI 工程驗收標準
- AI 模型在發生故障時自動切換至備用模型（無使用者感知）
- 模型自動恢復機制正常運作（每 5 分鐘嘗試一次）
- Structured Output 符合預定 JSON Schema，欄位完整率 100%
- 文法糾錯準確度 >= 90%（經人工抽檢 100 件樣本）
- 聊天回覆自然度 >= 80%（經 PM 或語言專家評分）

### 11.4 測試完成條件（Gate Criteria）
開發必須完成以下測試才能交付上線：

**單元測試 (Unit Test)**
- Service 層測試覆蓋率 >= 80%
- 所有 AI 相關邏輯必須有 Mock 測試
- 異常 fallback 邏輯必須有測試驗證

**整合測試 (Integration Test)**
- LINE Webhook 端到端流程測試通過
- AI 服務呼叫與 fallback 機制測試通過
- 資料持久化與快取一致性測試通過

**使用者驗收測試 (UAT)**
- 參照本文件第 5.2 節 UAT 表中所有測試項目必須 PASS
- 壓力測試：100 並發訊息，失敗率 < 5%
- 模型自動切換測試：主模型故障時自動切至備用模型，用戶無感知

**交付前檢查 (Pre-Deployment Checklist)**
- 參照 AI 開發規範第 5.3 節之檢查清單，所有項目打勾確認

### 11.5 缺陷容忍度
- **關鍵缺陷 (Critical)** 應修復後再交付（例如：無法回覆、AI 模型全部故障）
- **高優先缺陷 (High)** 可在 Hotfix List 中記錄，上線後 3 天內修復
- **中優先及以下** 可納入下一個版本

## 12. 未來擴充
- 依程度推薦學習內容
- 每日自動推播練習題
- 發音練習與語音辨識
- 錯題本與間隔複習（Spaced Repetition）
