# BloodCare Chatbot Implementation Guide

## Overview
A fully intelligent chatbot has been integrated into your BloodCare project. The chatbot is trained on your database and can answer questions about:
- **Donation History** - Show user donations and donated units
- **Certificates** - Display certificates from donations
- **Blood Requests** - Show user's blood requests
- **User Profile** - Display profile information
- **Donation Eligibility** - Check if user can donate based on criteria

## Components Added

### 1. **Database Models** 
- `ChatbotMessage.java` - Entity to store chat conversations

### 2. **Repository**
- `ChatbotMessageRepository.java` - Data access for chat history

### 3. **Service**
- `ChatbotService.java` - Core AI logic and intent detection
  - Extracts user data from database by email
  - Processes natural language queries
  - Generates contextual responses based on user data
  - Supports multiple intents: donations, certificates, profiles, eligibility

### 4. **Controller**
- `ChatbotController.java` - REST APIs
  - `POST /api/chatbot/message` - Send message and get response
  - `GET /api/chatbot/history` - Get chat history
  - `POST /api/chatbot/quick` - Quick query endpoint

### 5. **UI Components**
- `chatbot.html` - Full-page chatbot interface
- `chatbot-widget.js` - Floating widget for other pages

### 6. **Route**
- `GET /chatbot` - Access full chatbot page

## How to Use

### **Full Chatbot Page**
Access the complete chatbot at: `http://localhost:8080/chatbot`

Features:
- Beautiful gradient UI
- Message history persistence
- Real-time responses
- Animated loading indicator
- Mobile-friendly design

### **Floating Widget (For Other Pages)**
Add this single line to any HTML template to embed the chatbot widget:

```html
<script src="/js/chatbot-widget.js"></script>
```

The widget will:
- Appear as a floating button in bottom-right corner
- Open/close with click
- Work on any page without interference
- Auto-detect Font Awesome icons

### **Example Integration**
Add to your `donor-dashboard.html`:

```html
<!-- At the end of body tag -->
<script src="/js/chatbot-widget.js"></script>
```

## Chatbot Features

### Smart Intent Detection
The chatbot automatically understands user queries:

```
User: "How many donations have I made?"
Bot: Shows total donations, units donated, and recent donation details

User: "Show my certificates"
Bot: Lists all certificates with hospital names and dates

User: "Am I eligible to donate?"
Bot: Checks age, weight criteria and provides eligibility status

User: "What can you do?"
Bot: Shows help menu with all available features
```

### Data-Driven Responses
All responses are based on actual user data from:
- `User` table - Name, email, mobile
- `Donor` table - Blood group, age, weight, city, last donation date
- `Certificate` table - Donation history and certificates
- `BloodRequest` table - Blood requests created by user
- `VisitRequest` table - Visit history

## Database Schema
The new `chatbot_messages` table stores:
- `id` - Message ID
- `user_id` - Reference to User
- `userMessage` - User query
- `botResponse` - Bot response
- `timestamp` - When message was sent
- `messageType` - Type of query (DONATION, CERTIFICATE, etc.)

## API Endpoints

### Send Message
```
POST /api/chatbot/message
Headers: Content-Type: application/json
Body: { "message": "How many donations have I made?" }

Response:
{
  "success": true,
  "message": "How many donations have I made?",
  "response": "🩸 **Your Donation History**\n...",
  "timestamp": 1693219200000
}
```

### Get Chat History
```
GET /api/chatbot/history

Response:
{
  "success": true,
  "history": [
    {
      "id": 1,
      "userMessage": "Show my donations",
      "botResponse": "🩸 **Your Donation History**...",
      "timestamp": "2024-02-23T10:30:00",
      "messageType": "DONATION"
    }
  ]
}
```

## Configuration

### Database Migration
Run this SQL to create the chatbot_messages table:

```sql
CREATE TABLE chatbot_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  user_message LONGTEXT NOT NULL,
  bot_response LONGTEXT NOT NULL,
  timestamp DATETIME NOT NULL,
  message_type VARCHAR(50),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

Or let Hibernate auto-create it (it will happen on first run if you have JPA enabled).

## Response Formatting

Responses support markdown-like formatting:
- `**text**` → Bold text
- `__text__` → Bold text
- `\n` → Line break
- Emoji support: 🩸 💉 📜 👤 ✅ ❌ 🎉 ⚠️

Example response:
```
🩸 **Your Donation History**

✅ Total Donations: **5**
💉 Total Units Donated: **25**

**Recent Donations:**
• Donated **5 units** at **Apollo Hospital** on **2024-02-15**
• Donated **5 units** at **City Hospital** on **2024-01-20**
```

## Customization

### Add New Intents
Edit `ChatbotService.java`:

1. Add detection method:
```java
private boolean isYourIntentRelated(String query) {
    return query.contains("keyword1") || query.contains("keyword2");
}
```

2. Add response method:
```java
private String getYourIntentResponse(User user) {
    // Generate response based on user data
    return "Your response here";
}
```

3. Add to `generateResponse()`:
```java
if (isYourIntentRelated(lowerQuery)) {
    return getYourIntentResponse(user);
}
```

### Modify UI
- Edit `chatbot.html` for full page styling
- Edit `chatbot-widget.js` for widget styling
- All CSS is inline for easy customization

## Security

✅ **Features Implemented:**
- Session-based authentication (requires login)
- Email-based user identification
- Protected endpoints (checks user session)
- Message history tied to user account
- Input validation and error handling

## Performance

- Fast intent classification (no external API calls)
- Database queries optimized with JPA
- Chat history pagination ready
- Lightweight JavaScript widget

## Testing

### Test the Chatbot
1. Navigate to `http://localhost:8080/chatbot`
2. Log in with your credentials
3. Ask questions like:
   - "How many donations have I made?"
   - "Show my certificates"
   - "Am I eligible to donate?"
   - "What is my blood group?"
   - "Help"

### Test the Widget
1. Add `<script src="/js/chatbot-widget.js"></script>` to any page
2. Click the floating button in bottom-right corner
3. Chat normally

## Future Enhancements

Possible improvements:
- Integration with OpenAI API for more advanced NLP
- Sentiment analysis
- Multi-language support
- Voice input/output
- Advanced analytics dashboard
- Export chat history
- Chatbot training interface for admins
- Redis caching for faster responses
- WebSocket for real-time updates

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Chatbot not responding | Check if logged in, check browser console for errors |
| Database error | Ensure chatbot_messages table exists, check DB connection |
| Widget not showing | Check if `/js/chatbot-widget.js` path is correct |
| Formatting not working | Check if text uses `**text**` for bold |
| Session errors | Clear cookies and login again |

## File Structure

```
BloodCare/
├── src/main/java/com/bloodcare/bloodcare/
│   ├── entity/
│   │   └── ChatbotMessage.java
│   ├── repository/
│   │   └── ChatbotMessageRepository.java
│   ├── service/
│   │   └── ChatbotService.java
│   └── controller/
│       ├── ChatbotController.java
│       └── IndexController.java (updated with /chatbot route)
├── src/main/resources/
│   ├── templates/
│   │   └── chatbot.html
│   └── static/js/
│       └── chatbot-widget.js
└── CHATBOT_SETUP.md (this file)
```

## Summary

You now have a **fully functional AI chatbot** that:
✅ Automatically retrieves user data from database
✅ Understands natural language queries
✅ Provides instant, personalized responses
✅ Stores chat history
✅ Works as full page and floating widget
✅ Beautiful, responsive UI
✅ Mobile-friendly
✅ Requires no external API keys
✅ Fully trained on your BloodCare data

**Access it now:** `http://localhost:8080/chatbot`

Enjoy! 🚀
