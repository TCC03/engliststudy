# LINE Bot English Learning MVP

## Run
1. Copy `.env.example` to `.env` and set values.
2. Ensure Java 17 and Maven are installed.
3. Run:
   - `mvn spring-boot:run`

## Webhook Endpoint
- `POST /api/line/webhook`
- This endpoint now validates `X-Line-Signature` using `LINE_CHANNEL_SECRET`.
- It replies via LINE Reply API using `LINE_CHANNEL_ACCESS_TOKEN`.
- If Reply API fails (e.g., reply token expired), it falls back to Push API.

## Supported Commands
- `今日單字`
- `開始測驗`
- `文法`
- `對話練習`
- `AI聊天`
- `結束聊天`
- `糾錯 [句子]`
- `糾錯(簡單) [句子]`
- `幫助`

## AI Runtime Split
- `AI_PROVIDER=gemini`
- `DEV_ASSISTANT=gpt`
- `GEMINI_API_KEY=...`
- `GEMINI_MODEL=gemini-1.5-flash`

## LINE Credentials
- `LINE_CHANNEL_SECRET=...`
- `LINE_CHANNEL_ACCESS_TOKEN=...`

## Session Persistence
- Session state is saved in Redis when available.
- If Redis is unavailable, the service automatically falls back to in-memory session storage.

## Vocabulary Source (7000 API Sync)
- At startup, the app syncs 7000-word API data to local file `data/vocabulary_seed.csv`.
- `今日單字` reads local vocabulary file only.
- Default API source: `https://raw.githubusercontent.com/zenkarsha/words7000/main/words.json`
- Override with env var: `VOCABULARY_API_URL=...`
- Startup sync can be toggled with `VOCABULARY_SYNC_ON_STARTUP=true|false`.
