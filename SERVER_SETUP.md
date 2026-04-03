# BloodCare Server Setup - Complete Solution

## Problem
When signing up, you got this error:
```
POST http://localhost:8082/api/auth/signup net::ERR_CONNECTION_REFUSED
```

This means your **backend server is not running**.

## Solution Summary

Your backend server needs to run on **port 8082**. I've installed **Java 17** and created a startup script for you.

### Quick Start

**Option 1: Use the Batch File (Easiest)**
1. Open File Explorer
2. Navigate to: `C:\Users\Man\OneDrive\Desktop\BloodCare\BloodCare\`
3. Double-click `run-server.bat`
4. Wait for the message "Started BloodCareApplication" (2-5 minutes first time)
5. Then open http://localhost:8080 and try signing up again

**Option 2: Manual Command**
1. Open Command Prompt (cmd) or PowerShell
2. Run this:
```bash
cd C:\Users\Man\OneDrive\Desktop\BloodCare\BloodCare
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
mvnw.cmd spring-boot:run -Dmaven.test.skip=true
```

## Requirements Checklist
- ✓ Java 17 JDK - Installed
- ☐ MySQL Database - Must be running on localhost:3306
- ☐ Port 8082 - Must be available (check in Settings/Network)

## Database Setup
If MySQL isn't running:
1. Start MySQL Server
2. The database `bloodcare_db` will be created automatically
3. Default credentials (from application.properties):
   - Host: localhost
   - Port: 3306
   - User: root
   - Password: (empty)

##  Expected Output
When the server starts successfully, you'll see:
```
Started BloodCareApplication in X.XXX seconds
SLF4J: Found binding in [jar:file:...]
...and more Spring Boot logs
```

##  What Should Work Now
Once the server is running:
1. Open http://localhost:8080
2. Click "Sign Up"
3. Enter your credentials
4. You should see a success message (no more connection refused error)

## Troubleshooting

**Port 8082 already in use:**
- Another application is using port 8082
- Use Windows Task Manager → find process using 8082 → End Task

**MySQL connection failed:**
- Start MySQL Service
- Check credentials in `src/main/resources/application.properties`

**Java not found:**
- Java 17 was installed at: `C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot`
- Restart your computer to make sure PATH is updated

##  Files Modified
- `run-server.bat` - Startup script with Java 17 configuration
- Environment variable `JAVA_HOME` set to Java 17 path

---
**Last Updated:** Feb 8, 2026  
**Status:** Ready to test
