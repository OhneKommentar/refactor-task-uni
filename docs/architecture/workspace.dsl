workspace "Instakill" "C4 model of the Instakill monolith as it exists today" {

    !identifiers hierarchical

    model {
        user = person "User" "Registers, logs in, creates posts, likes and comments."

        instakill = softwareSystem "Instakill" "Instagram-style social feed (monolith)." {
            frontend = container "Vue SPA" "Single-page app for feed, posts, comments and likes." "Vue 3, Vite, Axios" "Web Browser"

            backend = container "Backend" "Serves the REST API (/api/**) and server-rendered pages (Thymeleaf)." "Java 23, Spring Boot 3.3, Spring Security, Spring Data JPA" {
                restControllers = component "REST Controllers" "Auth, user, post, feed, comment and like endpoints under /api." "Spring MVC"
                webControllers = component "Web Controllers" "Thymeleaf pages for login, register, feed, post detail and post creation." "Spring MVC, Thymeleaf"
                security = component "Security" "SecurityConfig filter chains, JWT authentication filter and token service (HttpOnly ACCESS_TOKEN cookie)." "Spring Security, jjwt"
                authService = component "AuthService / UserService" "Registration, login (BCrypt), user lookup." "Spring Service"
                postService = component "PostService" "Create, read and delete posts; ownership checks." "Spring Service"
                interactionService = component "InteractionService" "Likes (toggle) and comments." "Spring Service"
                feedService = component "FeedService" "Selects a ranking strategy: newest or trending." "Spring Service, Strategy pattern"
                eventPublisher = component "DomainEventPublisher" "Publishes LikeCreatedEvent. Current implementation is a no-op." "Spring Component"
                repositoryAdapters = component "Repository Adapters" "Implement the domain repository interfaces on top of JPA entities." "Spring Component, Entity mapping"
                jpaRepositories = component "Spring Data JPA Repositories" "Query methods and trending query." "Spring Data JPA, Hibernate"
            }

            database = container "Database" "Stores users, posts, comments and likes in a single file." "SQLite (instakill.db)" "Database"
        }

        user -> instakill.frontend "Uses" "Browser"
        user -> instakill.backend "Uses server-rendered pages" "HTTPS (Thymeleaf)"
        instakill.frontend -> instakill.backend "Calls REST API" "JSON over HTTP, JWT cookie"
        instakill.backend -> instakill.database "Reads and writes" "JDBC via Hibernate (ddl-auto: update)"

        instakill.backend.webControllers -> instakill.backend.authService "Registers and logs in users"
        instakill.backend.webControllers -> instakill.backend.postService "Creates, shows and comments on posts"
        instakill.backend.webControllers -> instakill.backend.interactionService "Likes posts"
        instakill.backend.restControllers -> instakill.backend.authService "Auth and user endpoints"
        instakill.backend.restControllers -> instakill.backend.postService "Post and feed endpoints"
        instakill.backend.restControllers -> instakill.backend.interactionService "Like and comment endpoints"
        instakill.backend.restControllers -> instakill.backend.security "Resolves the authenticated principal"
        instakill.backend.webControllers -> instakill.backend.security "Resolves the authenticated principal"
        instakill.backend.authService -> instakill.backend.security "Issues JWT on login"
        instakill.backend.postService -> instakill.backend.feedService "Uses FeedSort for listing"
        instakill.backend.postService -> instakill.backend.repositoryAdapters "Reads and writes posts"
        instakill.backend.interactionService -> instakill.backend.repositoryAdapters "Reads and writes likes and comments"
        instakill.backend.interactionService -> instakill.backend.eventPublisher "Publishes LikeCreatedEvent"
        instakill.backend.authService -> instakill.backend.repositoryAdapters "Reads and writes users"
        instakill.backend.feedService -> instakill.backend.repositoryAdapters "Fetches latest or trending posts"
        instakill.backend.repositoryAdapters -> instakill.backend.jpaRepositories "Delegates to"
        instakill.backend.jpaRepositories -> instakill.database "SQL" "JDBC"
    }

    views {
        systemContext instakill "SystemContext" "Instakill in its environment. No external services are integrated." {
            include *
            autoLayout lr
        }

        container instakill "Containers" "The three containers and how they communicate." {
            include *
            autoLayout lr
        }

        component instakill.backend "Components" "Main components of the backend container and their dependencies." {
            include *
            autoLayout tb
        }

        styles {
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Web Browser" {
                shape WebBrowser
            }
            element "Database" {
                shape Cylinder
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
        }

    }
}
