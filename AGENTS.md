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
   - **Note**: Not run automatically in pre-commit hook to prevent conflicts with checkstyle

2. **Checkstyle** (v3.3.1)
   - Code style checking and enforcement
   - Configuration: `checkstyle.xml`
   - Runs during `validate` phase
   - Command: `mvn checkstyle:check`
   - Max line length: 150 characters (configured in checkstyle.xml)
   - Includes naming conventions, import checks, size violations, whitespace rules
   - **Important**: Checkstyle rules are configured to be compatible with Spring Java Format
   - **Pre-commit Hook**: The pre-commit hook runs `mvn checkstyle:check` to validate code quality before commits
   - Checkstyle rules are aligned with Spring Java Format style:
     - `RightCurly` rule excludes `LITERAL_CATCH` tokens (catch blocks use separate lines, matching Spring Java Format)
     - Suppressions file (`checkstyle-suppressions.xml`) handles Spring Boot application class exceptions

3. **SpotBugs** (v4.7.3)
   - Static analysis for bug detection
   - Exclusions: `spotbugs-exclude.xml`
   - Runs during `verify` phase
   - Command: `mvn spotbugs:check`

4. **PMD** (v3.21.2)
   - Code quality analysis
   - Custom ruleset: `pmd-ruleset.xml` (excludes ShortVariable for loop variables)
   - Rulesets: bestpractices, codestyle, design, errorprone, performance, security
   - Runs during `verify` phase
   - Command: `mvn pmd:check`
   - **Fixing PMD Violations:**
     - **MethodArgumentCouldBeFinal**: Add `final` to method parameters
     - **LocalVariableCouldBeFinal**: Add `final` to local variables
     - **OnlyOneReturn**: Refactor to single return point
     - **LongVariable**: Shorten variable names
     - **UnusedAssignment**: Remove unused assignments
     - **GuardLogStatement**: Add log level guards
     - **AvoidCatchingGenericException**: Catch specific exceptions

5. **Thymeleaf Validator**
   - Custom validator for Thymeleaf templates
   - Validates template syntax
   - Runs during `test` phase (via `ThymeleafTemplateValidationTest`)
   - Command: `mvn test -Dtest=ThymeleafTemplateValidationTest` or `mvn test`
   - Manual execution: `mvn exec:java -Dexec.mainClass="com.nutriconsultas.ThymeleafValidator"`
   - **Architecture**: Modular validator system with per-template validators
   - **Package**: `com.nutriconsultas.validation.template`
   - **Key Components**:
     - `TemplateValidator` - Interface for template validators
     - `BaseTemplateValidator` - Base class with common mock variables
     - `TemplateValidatorRegistry` - Manages all validators
     - `WebContextFactory` - Creates web contexts for validation
   - **Template Validators**: Each template group has its own validator that defines required mock model variables
   - **Adding Validators**: Create a class extending `BaseTemplateValidator` and register it in `TemplateValidatorRegistry`
   - **Note**: Template validation no longer blocks application startup (`mvn spring-boot:run`). It runs as part of the test lifecycle.

### Running All Linting Tools
```bash
./lint.sh
```

### Fixing Code Quality Issues

**Spring Java Format:**
```bash
mvn spring-javaformat:apply
```
- Automatically fixes formatting issues (spacing, indentation, braces)
- Runs automatically during `validate` phase
- **Note**: Not run automatically in pre-commit hook (use manually: `mvn spring-javaformat:apply`)

**Checkstyle:**
```bash
mvn checkstyle:check
```
- Review `target/checkstyle-result.xml` for violations
- **Important**: Checkstyle rules are configured to be compatible with Spring Java Format (Google Java Style Guide)
- Common violations and fixes:
  - **NeedBraces**: Always use braces for if/for/while statements
    - ❌ `if (condition) statement;`
    - ✅ `if (condition) { statement; }`
  - **LineLength**: Keep lines under 150 characters (checkstyle.xml) or 120 characters (project standard)
    - Break long lines, extract variables, use method chaining on separate lines
  - **RightCurly**: Closing braces should be on same line as else/finally (catch blocks are excluded to match Spring Java Format)
    - ❌ `} else {` on separate lines
    - ✅ `} else {` on same line
    - Note: Catch blocks use separate lines (compatible with Spring Java Format): 
      ```java
      } catch (Exception e) {
      ```
      This matches Spring Java Format's style, which is enforced by the pre-commit hook
  - **LocalFinalVariableName/LocalVariableName**: Variables must start with lowercase letter
    - ❌ `final Ingrediente _ingrediente = ...;` or `Paciente _paciente = ...;`
    - ✅ `final Ingrediente ingrediente = ...;` or `Paciente pacienteEntity = ...;`
  - **FinalClass**: Utility classes should be `final`
    - ❌ `public class IngredienteComparators { }`
    - ✅ `public final class IngredienteComparators { }`
  - **VisibilityModifier**: Fields should be private
    - ❌ `static Map map = ...;` or `AlimentosRepository alimentosRepository;`
    - ✅ `private static final Map MAP = ...;` or `private AlimentosRepository alimentosRepository;`
  - **HideUtilityClassConstructor**: Utility classes need private constructors
    - ❌ `public class WebContextFactory { }`
    - ✅ `public class WebContextFactory { private WebContextFactory() { } }`
  - **NoWhitespaceAfter**: No space after opening brace
    - ❌ `{ value }`
    - ✅ `{value}`
- Most formatting issues are fixed by Spring Java Format

**SpotBugs:**
```bash
mvn spotbugs:check
```
- Review `target/spotbugsXml.xml` or HTML report
- Fix null checks, resource leaks, inefficient operations
- Exclude false positives in `spotbugs-exclude.xml`

**PMD:**
```bash
mvn pmd:check
```
- Review `target/pmd.xml` or HTML report
- Add `final` keywords to parameters and local variables
- Refactor methods with multiple returns to single return point
- Fix long variable names, unused assignments, add log guards
- Custom ruleset excludes ShortVariable for loop variables

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

### Testing Requirements for Agents

**CRITICAL: All code changes MUST include tests and template validations.**

When implementing new features or modifying existing code, agents MUST:

1. **Create Unit Tests for Services:**
   - Create test class: `XxxServiceTest.java` in `src/test/java/` mirroring package structure
   - Use `@ExtendWith(MockitoExtension.class)` for unit tests
   - Mock dependencies using `@Mock` annotations
   - Test all public methods including edge cases and error conditions
   - Use AssertJ for assertions: `assertThat()`
   - Follow naming: `testMethodName` for test methods

2. **Create Integration Tests for Controllers:**
   - Create or update test class: `XxxControllerTest.java`
   - Use `@SpringBootTest` with `@AutoConfigureMockMvc` for web layer tests
   - Use `@ExtendWith(MockitoExtension.class)` with `@InjectMocks` for unit-style controller tests
   - Test all endpoints (GET, POST, etc.)
   - Verify model attributes, view names, and redirects
   - Use `@WithMockUser` for authenticated endpoints
   - Test validation errors and success paths

3. **Update Template Validators:**
   - If creating new templates, update the corresponding `XxxTemplateValidator` class
   - Add mock model variables needed for template validation
   - Ensure all template variables used in new templates are included in the validator
   - Validators are in `com.nutriconsultas.validation.template` package
   - Register new validators in `TemplateValidatorRegistry` if creating a new template group

4. **Test Coverage:**
   - Aim for comprehensive coverage of new code
   - Test happy paths, error cases, and edge cases
   - Verify exception handling
   - Test validation logic

**Example Test Structure:**
```java
@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class ExampleServiceTest {
    @InjectMocks
    private ExampleServiceImpl service;
    
    @Mock
    private ExampleRepository repository;
    
    @BeforeEach
    public void setup() {
        // Setup test data
    }
    
    @Test
    public void testMethodName() {
        // Arrange, Act, Assert
    }
}
```

**Template Validator Update Example:**
```java
@Override
public Map<String, Object> createMockModelVariables() {
    Map<String, Object> variables = super.createMockModelVariables();
    // Add new mock variables for new templates
    variables.put("newVariable", createMockObject());
    return variables;
}
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
2. **Line Length**: Maximum 120 characters - break long lines into multiple lines
3. **Naming**: 
   - Classes: PascalCase (e.g., `MyClass`)
   - Methods/Variables: camelCase starting with lowercase (e.g., `myMethod`, `myVariable`)
   - **CRITICAL: NEVER use underscore prefix for variables** (e.g., `_variable` is invalid)
     - Use descriptive names: `pacienteEntity`, `savedDieta`, `ingredienteResult` instead of `_paciente`, `_dieta`, `_ingrediente`
   - Constants: UPPER_SNAKE_CASE (e.g., `MY_CONSTANT`)
4. **Braces**: Always use braces for if/for/while statements
   - ❌ `if (condition) statement;`
   - ✅ `if (condition) { statement; }`
5. **Utility Classes**: Must be `final` with private constructor
   - ❌ `public class MyComparators { }`
   - ✅ `public final class MyComparators { private MyComparators() { } }`
6. **Field Visibility**: All fields must be private
   - ❌ `static Map map;` or `AlimentosRepository repository;`
   - ✅ `private static final Map MAP;` or `private AlimentosRepository repository;`
7. **Brace Placement**: 
   - **else/finally**: Closing braces should be on same line
     - ❌ `} else {` on separate lines
     - ✅ `} else {` on same line
   - **catch blocks**: Use separate lines (Spring Java Format style, enforced by pre-commit hook)
     - ✅ `} catch (Exception e) {` (closing brace on its own line)
     - This is configured in checkstyle.xml by excluding `LITERAL_CATCH` from RightCurly rule
8. **Imports**: Avoid star imports, remove unused imports
9. **Method Length**: Maximum 150 lines
10. **Parameters**: Maximum 7 parameters per method
11. **Logging**: Use appropriate log levels (DEBUG, INFO, WARN, ERROR)

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

