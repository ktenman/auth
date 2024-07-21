@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

LAYOUT_WITH_LEGEND()

Person(user, "User", "End user of the application")

System_Boundary(auth_system, "Authentication System") {
Container(frontend, "Frontend", "Thymeleaf", "Provides login interface")
Container(backend, "Backend", "Spring Boot, Kotlin", "Handles authentication logic")
Container(redis, "Session Store", "Redis", "Stores session information")
}

System_Ext(google, "Google OAuth2", "Provides authentication service")

Rel(user, frontend, "Accesses", "HTTPS")
Rel(frontend, backend, "Redirects to", "/oauth2/authorization/google")
Rel(backend, google, "Authenticates with", "OAuth2")
Rel(google, backend, "Returns user info")
Rel(backend, redis, "Stores session", "Redis protocol")
Rel(backend, frontend, "Redirects to configured URL")
@enduml
