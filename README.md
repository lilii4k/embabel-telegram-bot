<img align="left" src="https://github.com/embabel/embabel-agent/blob/main/embabel-agent-api/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

# Telegram Bot Agent

![Build](https://github.com/embabel/kotlin-agent-template/actions/workflows/maven.yml/badge.svg)

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white) ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) ![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white) ![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)

<br clear="left"/>


A Telegram bot built with the [Embabel framework](https://github.com/embabel/embabel-agent) for AI-powered messaging and survey collection.

Built with Spring Boot 3.5.9, Embabel 0.3.1, and MySQL.

# Setup

## Prerequisites

1. A Telegram bot token (see [TELEGRAM_INTEGRATION.md](./TELEGRAM_INTEGRATION.md) for setup instructions)
2. Your Telegram chat ID
3. MySQL database (for survey functionality)

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

Set your Telegram bot token via environment variable:

```bash
export TELEGRAM_BOT_TOKEN="your_bot_token_here"
```

Or edit `src/main/resources/application.properties`:

```properties
telegram.bot.token=your_bot_token_here
```

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

### One-Way Messaging

Send notification messages (no response expected):

```shell
# Send a message to a user
x "Message user 8360446449 'Hello from Embabel'"

# Send availability request
x "Send a telegram to 8360446449 saying the deployment is complete"

# Notify about completion
x "Notify user 8360446449 that their report is ready"
```

### Shell Commands (Testing)

For direct testing without natural language parsing:

```shell
# Send a message
telegram --chat-id 8360446449 --message "Hello from Embabel!"
```

## How It Works

This bot uses two specialized agents:

### SurveyAgent
Handles asking questions and collecting responses:
- Understands natural language survey requests
- Extracts chat ID, question, and expected response count
- Creates surveys in the database
- Automatically processes incoming responses
- Displays summary when survey completes

**Keywords:** ask, question, survey, poll, fetch, collect, response, answer

### TelegramNotificationAgent
Handles one-way messaging:
- Sends notifications and announcements
- Does not expect responses
- Used for alerts and informational messages

**Keywords:** send message, notify, announce, tell, inform

## Architecture

```
User Request (Natural Language)
    ↓
Agent Selection (AI-powered)
    ↓
├─ SurveyAgent (for questions)
│   ├─ Create survey in database
│   ├─ Send question via Telegram
│   ├─ Collect responses automatically
│   └─ Display summary when complete
│
└─ TelegramNotificationAgent (for notifications)
    └─ Send message via Telegram
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
│   ├── SurveyAgent.kt              # Handles survey creation
│   └── TelegramNotificationAgent.kt # Handles one-way messages
├── bot/
│   └── TelegramBotListener.kt      # Receives Telegram updates
├── entity/
│   ├── Survey.kt                   # Survey entity
│   ├── SurveyResponse.kt           # Response entity
│   └── SurveyStatus.kt             # Status enum
├── repository/
│   ├── SurveyRepository.kt         # Survey data access
│   └── SurveyResponseRepository.kt # Response data access
├── service/
│   ├── SurveyService.kt            # Survey business logic
│   └── SurveyResponseService.kt    # Response processing
└── tools/
    ├── SurveyTools.kt              # Survey operations
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
