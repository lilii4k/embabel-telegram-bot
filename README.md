<img align="left" src="https://github.com/embabel/embabel-agent/blob/main/embabel-agent-api/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

# Telegram Bot Agent

![Build](https://github.com/embabel/kotlin-agent-template/actions/workflows/maven.yml/badge.svg)

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white) ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) ![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white) ![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)

<br clear="left"/>


A Telegram bot built with the [Embabel framework](https://github.com/embabel/embabel-agent)

Built with Spring Boot 3.5.9, Embabel 0.3.1, and MySQL.

# Setup

## Prerequisites

1. A Telegram bot token and chat ID (see setup instructions below)
2. MySQL database (for survey functionality)

## Database Setup

1. Create a MySQL database:
```sql
CREATE DATABASE telegram_bot_db;
```

2. Configure database credentials in `src/main/resources/application.properties`:
```properties
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
```

Or set via environment variables:
```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
```

The database tables will be created automatically on first run.

## Telegram Configuration

### 1. Create a Telegram Bot

1. Open Telegram and search for [@BotFather](https://t.me/botfather)
2. Send `/newbot` and follow the instructions to create a bot
3. Copy the bot token provided by BotFather

### 2. Configure the Bot Token

Set your bot token via environment variable:

```bash
export TELEGRAM_BOT_TOKEN="your_bot_token_here"
```

Or edit `src/main/resources/application.properties`:

```properties
telegram.bot.token=your_bot_token_here
```

### 3. Get Your Chat ID

To find your chat ID (needed for sending surveys):

1. Start a conversation with your bot in Telegram
2. Send any message to the bot
3. Visit: `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`
4. Look for `"chat":{"id":...}` in the response - this is your chat ID

**Note:**
- Individual chat IDs are positive numbers (e.g., `8360446449`)
- Group chat IDs are negative numbers (e.g., `-123456789`)

### Troubleshooting

**"Failed to send message: Forbidden"**
- The bot doesn't have permission. User must start a conversation with the bot first and send at least one message.

**"Failed to send message: Bad Request: chat not found"**
- The chat ID is incorrect. Double-check using the `getUpdates` API endpoint above.

# Running

Start the Embabel Spring Shell:

```bash
./scripts/shell.sh
```

Or use Maven directly:

```bash
mvn clean spring-boot:run
```

## Usage

### Survey Collection

Ask questions and collect responses from Telegram users:

```shell
# Ask one person
x "Ask user 8360446449 what their favourite colour is"

# Ask multiple people in a group
x "Ask 5 users in group -123456789 what their favorite food is"

# Survey with specific count
x "Survey 3 users in chat -987654321 about their preferred programming language"
```

**How it works:**
1. Survey question is sent to the specified chat
2. Users respond in Telegram with their answers
3. Responses are automatically collected and stored
4. When all expected responses are received, a summary is displayed in the shell:

```
================================================================================
📊 SURVEY COMPLETE - ID: 2
================================================================================
Question: What is your favourite colour?

Responses (3/3):
1. Alice: Blue
2. Bob: Red
3. Charlie: Green
================================================================================
```

## How It Works

The bot uses two specialized agents that handle different parts of the survey workflow:

### SurveyInitiationAgent
Handles survey creation and initiation:
- Understands natural language survey requests
- Extracts chat ID, question, and expected response count using AI
- Creates survey records in the database
- Sends the survey question to Telegram
- Returns a SurveyInitiated object with the survey details

**Keywords:** ask, question, survey, poll, create, send

### SurveyResultsAgent
Handles survey completion and results processing:
- Triggered when a user submits a survey response
- Checks if the survey has received all expected responses
- When complete, analyzes responses using AI to extract insights
- Publishes formatted results back to Telegram
- Marks the survey as completed in the database

**Keywords:** process, results, analyze, complete

## Architecture

```
User Request (Natural Language)
    ↓
SurveyInitiationAgent
    ├─ Parse request with AI
    ├─ Create survey in database
    ├─ Send question via Telegram
    └─ Return SurveyInitiated
    ↓
[Users respond in Telegram]
    ↓
TelegramBotListener (receives responses)
    ├─ Store response in database
    └─ Trigger SurveyResultsAgent
    ↓
SurveyResultsAgent
    ├─ Check if survey is complete
    ├─ If complete: analyze responses with AI
    ├─ Generate insights summary
    └─ Publish results to Telegram
```

## Database Schema

### surveys
- id (PK)
- chatId
- question
- status (ACTIVE, COMPLETED, CANCELLED)
- expectedCount
- createdAt, completedAt
- summary

### survey_responses
- id (PK)
- surveyId (FK)
- userId
- userName
- response
- respondedAt

### pending_response_changes
- id (PK)
- surveyId (FK)
- userId
- oldResponse
- newResponse
- requestedAt

## Troubleshooting

### Bot not receiving group messages

If the bot isn't receiving messages in group chats:

1. Disable privacy mode via @BotFather:
   ```
   /setprivacy
   [Select your bot]
   Disable
   ```

2. Remove bot from group and re-add it

3. Alternatively, make the bot an admin (admins always receive all messages)

### Survey not completing

- Check that the correct number of unique users have responded
- Each user can only respond once per survey
- Verify responses are text messages (not photos, stickers, etc.)
- Check logs for any errors

## Development

### Project Structure

```
src/main/kotlin/com/embabel/template/
├── agent/
│   ├── SurveyInitiationAgent.kt    # Creates and sends surveys
│   └── SurveyResultsAgent.kt       # Processes responses and publishes results
├── bot/
│   └── TelegramBotListener.kt      # Receives Telegram updates
├── domain/
│   ├── SurveyCheckInput.kt         # Input for checking survey completion
│   ├── SurveyInitiated.kt          # Survey initiation result
│   └── SurveyResults.kt            # Survey results data
├── entity/
│   ├── PendingResponseChange.kt    # Pending response modifications
│   ├── Survey.kt                   # Survey entity
│   ├── SurveyResponse.kt           # Response entity
│   └── SurveyStatus.kt             # Status enum
├── repository/
│   ├── PendingResponseChangeRepository.kt # Pending change data access
│   ├── SurveyRepository.kt         # Survey data access
│   └── SurveyResponseRepository.kt # Response data access
├── service/
│   └── SurveyService.kt            # Survey business logic
└── tools/
    └── TelegramTools.kt            # Telegram messaging
```

### Building

```bash
mvn clean install
```

### Testing

Ensure your database is running and configured, then:

```bash
mvn spring-boot:run
```

## License

Apache License 2.0 - see LICENSE file for details.
