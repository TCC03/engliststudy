# Rich Menu 設定指南

Rich Menu 是 LINE Bot 的快捷功能菜單，提供用戶快速訪問主要功能。

## 功能按鈕

當前 Rich Menu 包含四個按鈕：

1. **開始測驗** - 開啟測驗模式
2. **今日單字** - 顯示每日精選單字（Flex Card 格式）
3. **我的字庫** - 用戶個人單字庫（開發中）
4. **幫助** - 顯示所有可用指令

## 設定步驟

### 1. 準備 Rich Menu JSON

已包含文件 `linebot-rich-menu.json` 包含預設配置。該配置定義：
- 菜單尺寸：2500 x 810 像素
- 4 個等寬按鈕，每個 625px 寬
- 3 個按鈕使用 postback 事件（觸發機器人邏輯）
- 1 個按鈕使用 message 事件（發送「help」文本）

### 1-1. 用 Postman 直接建立 Rich Menu

如果你想直接用 Postman 送 API，不用手動在網頁上建立，可以匯入這兩個檔案：

- [postman/line-rich-menu-create.postman_collection.json](postman/line-rich-menu-create.postman_collection.json)
- [postman/linebot.environment.json](postman/linebot.environment.json)

匯入後，請先在 Environment 裡填入 `channel_access_token`，再送出 `Create Rich Menu` 這個 request。

建立成功後，API 會回傳 `richMenuId`。若要繼續上傳 Rich Menu 圖片或設定成預設選單，還要再呼叫對應的 LINE API。

### 2. 上傳到 LINE Developers Console

1. 登入 [LINE Developers Console](https://developers.line.biz/console/)
2. 選擇你的 Bot 應用
3. 進入「Rich Menu」頁面（Messaging API → Rich Menu）
4. 點擊「建立」（Create）
5. 選擇「上傳 JSON」或複製 `linebot-rich-menu.json` 內容到編輯器
6. 設定為默認菜單

### 3. 驗證集成

機器人將自動處理以下 postback 數據：
- `action=start_quiz` → 開始測驗
- `action=daily_vocabulary` → 顯示今日單字
- `action=my_vocabulary` → 我的字庫（尚未實現）

## 自訂菜單

修改 `linebot-rich-menu.json` 來自訂菜單：

```json
{
  "areas": [
    {
      "bounds": { "x": 0, "y": 0, "width": 625, "height": 810 },
      "action": {
        "type": "postback",
        "label": "按鈕標籤",
        "data": "action=your_action_name"
      }
    }
  ]
}
```

## 代碼支持

`MessageRouterService.handlePostback()` 已更新以支援 Rich Menu 的 `action=` 格式：

```java
if (postbackData.startsWith("action=")) {
    String action = postbackData.substring("action=".length());
    switch (action) {
        case "start_quiz":
            startQuizWithFlex(state, replyToken, messagingService);
            return;
        case "daily_vocabulary":
            // Reply with flex card
            return;
        case "my_vocabulary":
            // My vocabulary logic
            return;
    }
}
```

## 後續開發

要添加新功能到 Rich Menu：

1. 編集 `linebot-rich-menu.json` 的 `areas` 陣列，添加新按鈕
2. 在 `MessageRouterService.handlePostback()` 的 switch 語句添加新的 case
3. 上傳更新的 JSON 到 LINE Developers Console
4. 測試 postback 流程
