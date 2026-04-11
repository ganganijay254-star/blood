# BloodCare

BloodCare is a Spring Boot Maven project for blood donation, donor management, blood requests, rewards, notifications, and related admin workflows.

## Project Structure

```text
.
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/com/bloodcare/bloodcare/
│   │   └── resources/
│   └── test/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

## Run Locally

1. Set the required environment variables if you use Google OAuth or email.
2. Make sure MySQL is available for `bloodcare_db`.
3. Start the app with:

```powershell
mvn spring-boot:run
```

Or use the Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

## Notes

- Java version: `17`
- Build tool: Maven
- Main class: `com.bloodcare.bloodcare.BloodCareApplication`
