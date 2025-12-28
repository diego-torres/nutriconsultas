# Agent Documentation for Nutriconsultas Project

This document provides essential information for AI agents working on the nutriconsultas project.

## Project Overview

**nutriconsultas** is a Spring Boot web application for managing a nutrition consultation clinic. It provides functionality for patient management, diet planning, food tracking, and meal planning.

## Technology Stack

### Core Framework
- **Spring Boot**: 3.4.1
- **Java**: 21
- **Build Tool**: Maven 3.6+
- **Template Engine**: Thymeleaf
- **Database**: PostgreSQL (production), H2 (testing)

### Key Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Thymeleaf
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter OAuth2 Client
- Spring Session (JDBC-backed)
- PostgreSQL Driver
- Lombok
- AWS SDK S3
- Flying Saucer PDF (for PDF generation)
- JWT (jjwt 0.9.1)

### Frontend Templates
- **sb-admin**: Admin dashboard template located in `src/main/resources/templates/sbadmin/`
  - Used for authenticated admin pages (`/admin/**`)
  - Includes sidebar, topbar, footer components
  - Pages: index, login, pacientes, dietas, alimentos, platillos
- **eterna**: Public-facing template located in `src/main/resources/templates/eterna/`
  - Used for public pages (homepage, about, services, etc.)
  - Pages: index, about, blog, contact, portfolio, pricing, services, team

## Project Structure

```
src/
├── main/
│   ├── java/com/nutriconsultas/
│   │   ├── admin/          # Admin dashboard controllers
│   │   ├── alimentos/      # Food management
│   │   ├── consulta/       # Consultation management
│   │   ├── controller/     # Base controllers (AbstractAuthorizedController, etc.)
│   │   ├── dataTables/     # DataTables support
│   │   ├── dieta/          # Diet planning
│   │   ├── model/          # Data models
│   │   ├── paciente/       # Patient management
│   │   ├── platillos/      # Meal/dishes management
│   │   ├── Application.java
│   │   ├── SecurityConfig.java
│   │   └── ThymeleafValidator.java
│   └── resources/
│       ├── templates/      # Thymeleaf templates
│       │   ├── sbadmin/    # Admin templates
│       │   └── eterna/     # Public templates
│       └── static/         # Static assets (CSS, JS, images)
└── test/
    └── java/com/nutriconsultas/
        └── [test packages mirroring main structure]
```

## Code Quality & Linting

### Tools Configured
1. **Spring Java Format** (v0.0.47)
   - Automatic code formatting following Spring's coding standards
   - Runs during `validate` phase
   - Command: `mvn spring-javaformat:apply`

2. **Checkstyle** (v3.3.1)
   - Code style checking and enforcement
   - Configuration: `checkstyle.xml`
   - Runs during `validate` phase
   - Command: `mvn checkstyle:check`
   - Max line length: 120 characters
   - Includes naming conventions, import checks, size violations, whitespace rules

3. **SpotBugs** (v4.7.3)
   - Static analysis for bug detection
   - Exclusions: `spotbugs-exclude.xml`
   - Runs during `verify` phase
   - Command: `mvn spotbugs:check`

4. **PMD** (v3.21.2)
   - Code quality analysis
   - Rulesets: bestpractices, codestyle, design, errorprone, performance, security
   - Runs during `verify` phase
   - Command: `mvn pmd:check`

5. **Thymeleaf Validator**
   - Custom validator for Thymeleaf templates
   - Validates template syntax
   - Runs during `validate` phase
   - Command: `mvn test-compile exec:java -Dexec.mainClass="com.nutriconsultas.ThymeleafValidator"`

### Running All Linting Tools
```bash
./lint.sh
```

### Git Pre-commit Hook
- Automatically formats code before commit
- Installed via `./setup-git-hooks.sh`
- Formats all Java files using Spring Java Format

## Testing

### Test Framework
- **JUnit 5** (via Spring Boot Starter Test)
- **Spring Security Test** for security testing
- **H2 Database** for in-memory testing
- **OpenCSV** for CSV test data

### Test Structure
- Tests mirror main package structure
- Test resources in `src/test/resources/`
- Test configuration: `application-test.properties`
- Sample test data: `alimentos.csv`, `platillos.csv`

### Running Tests
```bash
mvn test
```

## Security Configuration

### Authentication
- **OAuth2** with Auth0 provider
- Spring Security configured in `SecurityConfig.java`
- Session management via Spring Session JDBC

### Security Rules
- `/rest/**` - Requires authentication
- `/admin/**` - Requires authentication
- All other routes - Public access
- CSRF disabled (configured for API usage)
- OAuth2 login enabled

### Controller Patterns
- **AbstractAuthorizedController**: Base class for authenticated controllers
  - Automatically adds `username` and `user_picture` to model
  - Extracted from OAuth2 principal
- **AbstractGridController**: Base for grid/list views
- **AbstractGridItemController**: Base for grid item operations

## Development Best Practices

### Spring Boot Best Practices
1. **Dependency Injection**: Use constructor injection (preferred) or `@Autowired` on fields
2. **Logging**: Use SLF4J with Lombok `@Slf4j` annotation
3. **Validation**: Use `@Valid` and Jakarta Validation annotations
4. **REST Controllers**: Use `@RestController` for API endpoints, `@Controller` for Thymeleaf views
5. **Service Layer**: Implement service interfaces, use `@Service` annotation
6. **Repository Layer**: Extend `JpaRepository` or use `@Repository` annotation
7. **Exception Handling**: Use `@ControllerAdvice` for global exception handling
8. **Configuration**: Use `@Configuration` classes, externalize properties
9. **Testing**: Write unit tests for services, integration tests for controllers
10. **Security**: Always validate user permissions, use method-level security when needed

### Thymeleaf Best Practices
1. **Template Location**: 
   - Admin pages: `sbadmin/` directory
   - Public pages: `eterna/` directory
2. **Fragment Reuse**: Use Thymeleaf fragments for common components (sidebar, footer, topbar)
3. **Security**: Use `sec:` namespace for security-aware expressions
4. **Validation**: Display validation errors using `th:errors`
5. **Internationalization**: Use `th:text` with message keys for i18n
6. **URLs**: Use `@{...}` syntax for URL generation
7. **Model Attributes**: Access model attributes with `${attributeName}`
8. **Conditional Rendering**: Use `th:if`, `th:unless`, `th:switch` for conditional logic

### Code Style Guidelines
1. **Formatting**: Follow Spring Java Format (automatically applied)
2. **Line Length**: Maximum 120 characters
3. **Naming**: 
   - Classes: PascalCase
   - Methods/Variables: camelCase
   - Constants: UPPER_SNAKE_CASE
4. **Imports**: Avoid star imports, remove unused imports
5. **Method Length**: Maximum 150 lines
6. **Parameters**: Maximum 7 parameters per method
7. **Logging**: Use appropriate log levels (DEBUG, INFO, WARN, ERROR)

### Database Best Practices
1. **JPA Entities**: Use `@Entity`, proper relationships (`@OneToMany`, `@ManyToOne`, etc.)
2. **Repository**: Use Spring Data JPA repositories
3. **Transactions**: Use `@Transactional` on service methods
4. **DDL**: `spring.jpa.hibernate.ddl-auto=update` (development only)
5. **Queries**: Use `@Query` for custom queries, prefer JPQL over native SQL
6. **Pagination**: Use `Pageable` for large datasets

## Environment Configuration

### Required Environment Variables
- `JDBC_DATABASE_URL`: PostgreSQL connection URL
- `JDBC_DATABASE_USERNAME`: Database username
- `JDBC_DATABASE_PASSWORD`: Database password
- `AUTH_CLIENT`: Auth0 client ID
- `AUTH_SECRET`: Auth0 client secret
- `AUTH_ISSUER`: Auth0 issuer URI
- `AWS_BUCKET`: S3 bucket name
- `AWS_KEY`: AWS access key
- `AWS_SECRET`: AWS secret key
- `MAPS_KEY`: Google Maps API key (optional)

### Development Setup
1. Create `.env` file with environment variables
2. Run `./dev-start.sh` to start database and application
3. Or manually: `podman compose up -d postgres` then `mvn spring-boot:run`

## Common Patterns

### Controller Pattern
```java
@Controller
@Slf4j
public class ExampleController extends AbstractAuthorizedController {
    
    private final ExampleService service;
    
    public ExampleController(ExampleService service) {
        this.service = service;
    }
    
    @GetMapping("/example")
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "sbadmin/example/list";
    }
}
```

### Service Pattern
```java
@Service
@Slf4j
public class ExampleServiceImpl implements ExampleService {
    
    private final ExampleRepository repository;
    
    public ExampleServiceImpl(ExampleRepository repository) {
        this.repository = repository;
    }
    
    @Override
    @Transactional
    public List<Example> findAll() {
        return repository.findAll();
    }
}
```

### REST Controller Pattern
```java
@RestController
@RequestMapping("/rest/example")
@Slf4j
public class ExampleRestController {
    
    private final ExampleService service;
    
    // REST endpoints...
}
```

## Important Notes

1. **Template Validation**: All Thymeleaf templates are validated during build
2. **Code Formatting**: Code is automatically formatted on commit
3. **CI/CD**: Linting runs in CI with strict mode (`ci` profile)
4. **Security**: Always extend `AbstractAuthorizedController` for authenticated pages
5. **Static Assets**: Located in `src/main/resources/static/`
6. **Port**: Application runs on port 3000 (configurable via `server.port`)
7. **Session Storage**: Uses JDBC-backed sessions (PostgreSQL)

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)

