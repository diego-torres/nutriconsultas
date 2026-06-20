# Agent Documentation for Nutriconsultas Project

This document provides essential information for AI agents working on the nutriconsultas project.

## Project Overview

**nutriconsultas** is a Spring Boot web application for managing a nutrition consultation clinic. It provides functionality for patient management, diet planning, food tracking, and meal planning.

### Project Information

- **Project Name:** nutriconsultas
- **Company:** Minutriporcion
- **Branding:** Minutriporcion is the product and branding name for the nutriconsultas project. The project "nutriconsultas" is developed for the company Minutriporcion.

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

### Multi-Tenant Access Control

**CRITICAL: All patient data is isolated by user (nutritionist).**

The application implements multi-tenant access control where:
- **Patients belong to specific users**: Each `Paciente` entity has a `userId` field that stores the OAuth2 user identifier (the `sub` claim)
- **User isolation is enforced**: Users can only view and manage their own patients
- **No cross-user visibility**: Patient data is completely isolated per user

#### Implementation Details

1. **Entity Level**:
   - `Paciente` entity includes a `userId` field (String, nullable = false)
   - The `userId` stores the OAuth2 `sub` claim (unique user identifier from Auth0)

2. **Repository Level**:
   - `PacienteRepository` provides methods:
     - `findByUserId(String userId)` - Find all patients for a user
     - `findByIdAndUserId(Long id, String userId)` - Find a specific patient by ID and userId (for security)

3. **Service Level**:
   - `PacienteService` provides userId-filtered methods:
     - `findByIdAndUserId(Long id, String userId)` - Secure patient lookup
     - `findAllByUserId(String userId)` - Get all patients for a user
     - `deleteByIdAndUserId(Long id, String userId)` - Secure patient deletion

4. **Controller Level**:
   - All `PacienteController` methods that access patients:
     - Accept `@AuthenticationPrincipal OidcUser principal` parameter
     - Extract userId using `principal.getSubject()`
     - Use `findByIdAndUserId()` instead of `findById()` for security
     - Call `verifyPatientOwnership()` to ensure patient belongs to user
   - New patients automatically get `userId` set from the authenticated user

5. **REST API Level**:
   - `PacienteRestController` filters all patient data by userId
   - Overrides `getPageArray()` to accept `@AuthenticationPrincipal OidcUser principal`
   - Uses `findAllByUserId()` to return only the current user's patients

#### Security Best Practices

When working with patient data:

1. **Always use userId-filtered methods**: Never use `findById()` or `findAll()` directly
   - ✅ Use: `findByIdAndUserId(id, userId)`
   - ✅ Use: `findAllByUserId(userId)`
   - ❌ Avoid: `findById(id)` (no user filtering)
   - ❌ Avoid: `findAll()` (returns all patients)

2. **Always verify ownership**: Before accessing patient data, verify it belongs to the current user
   ```java
   final String userId = getUserId(principal);
   final Paciente paciente = repository.findByIdAndUserId(id, userId)
       .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
   verifyPatientOwnership(paciente, userId);
   ```

3. **Set userId on creation**: When creating new patients, always set the userId
   ```java
   paciente.setUserId(getUserId(principal));
   ```

4. **Test multi-tenant scenarios**: All tests should verify:
   - Users can only see their own patients
   - Users cannot access other users' patients
   - Patient creation assigns correct userId

#### Related Entities

Entities that reference `Paciente` (like `CalendarEvent`, `ClinicalExam`, `AnthropometricMeasurement`) inherit patient ownership through the relationship. When verifying access to these entities:
1. Load the related entity
2. Access the `paciente` relationship
3. Verify the `paciente.userId` matches the current user's ID

#### Migration Notes

- Existing patients in the database may have `null` userId values
- A migration script may be needed to assign userId to existing patients
- New patients created after this implementation will automatically have userId set

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
9. **Dialogs and confirmations (admin UI)**: Do **not** use native `alert()`, `confirm()`, or inline `onsubmit="return confirm(...)"`. Use **bootstrap-sweetalert** (`swal`) already vendored under `/sbadmin/vendor/bootstrap-sweetalert/`:
   - Include `sweetalert.css` in `<head>` and `sweetalert.js` after jQuery on pages that show dialogs.
   - **Confirm destructive/cancel actions**: `type: 'warning'`, `showCancelButton: true`, `confirmButtonColor: '#d33'`, Spanish `confirmButtonText` / `cancelButtonText`; submit the form or run AJAX only in the `swal(..., function (isConfirm) { ... })` callback when `isConfirm` is true. Prefer `type="button"` + JS over `onsubmit` confirm.
   - **Validation / info**: `type: 'warning'` or `'info'`, short Spanish `title` + `text`, optional `timer`.
   - **Errors**: `type: 'error'`, `timer: 5000`.
   - **Success after AJAX**: `type: 'success'`, `timer: 2000` (see `sbadmin/dietas/listado.html`, `sbadmin/calendar/listado.html`).
   - **Language**: User-facing dialog strings in **Spanish**; match existing admin tone.
   - **Reference templates**: `platform/invitations/list.html`, `contact-inquiries/listado.html`, `pacientes/historial.html`.

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
12. **Logging Sensitive Information**: **CRITICAL - Never log patient-identifiable data**
    - ❌ **NEVER log**: Patient names, emails, phone numbers, medical records, or any personal information
    - ❌ **NEVER log**: Entire patient objects (e.g., `log.info("Patient: {}", paciente)`)
    - ✅ **DO log**: Only non-sensitive identifiers like IDs, timestamps, and system information
    - ✅ **Use LogRedaction utility**: When logging entities that contain patient data, use `LogRedaction` utility class
      - Example: `log.info("Paciente found: {}", LogRedaction.redactPaciente(paciente))`
      - Available methods: `redactPaciente()`, `redactCalendarEvent()`, `redactClinicalExam()`, `redactPacienteDieta()`, `redactAnthropometricMeasurement()`
    - **Location**: `com.nutriconsultas.util.LogRedaction`
    - **Purpose**: Prevents personal information from being exposed in log files for privacy compliance

### Database Best Practices
1. **JPA Entities**: Use `@Entity`, proper relationships (`@OneToMany`, `@ManyToOne`, etc.)
2. **Repository**: Use Spring Data JPA repositories
3. **Transactions**: Use `@Transactional` on service methods
4. **DDL**: Liquibase changelogs (`docs/db/LIQUIBASE.md`); `spring.jpa.hibernate.ddl-auto=none` in all environments (#46). **Hibernate will not apply entity changes automatically** — every table/column/FK/index change needs an incremental Liquibase changeset.
5. **Queries**: Use `@Query` for custom queries, prefer JPQL over native SQL
6. **Pagination**: Use `Pageable` for large datasets

### Liquibase — when entities or data change

**Read [`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md)** (entity/schema/catalog checklist). Summary for agents:

| You changed… | You must… |
|--------------|-----------|
| `@Entity` field, table, relationship, `@Column` | Add incremental changeset under `db/changelog/changes/`, include in `db.changelog-master.yaml`; do **not** edit `001-baseline-schema.*` on deployed DBs |
| Catalog reference data (alimentos, platillos, template dietas) | Update or add Liquibase seed `sqlFile` changesets with `preConditions`; no Java `CommandLineRunner` seeders |
| Patient / tenant runtime data | Schema only via Liquibase; rows created by application code, not seed SQL |
| Brownfield production | Use `preConditions onFail: MARK_RAN`; assume baseline/seed may already be marked ran |

**Testing:** `mvn verify` (H2); boot PostgreSQL with **Java 21** (`./dev-start.sh`). `@DataJpaTest` uses `db.changelog-test-master.yaml` (baseline only) — use `@SpringBootTest` or `LiquibaseMigrationTest` when full seed is required.

**Never:** `ddl-auto=update`, in-place edits to merged baseline/seed files, or assuming a local entity change propagates without a changeset.

## Environment Configuration

### Required Environment Variables
- `JDBC_DATABASE_URL`: PostgreSQL connection URL
- `JDBC_DATABASE_USERNAME`: Database username
- `JDBC_DATABASE_PASSWORD`: Database password
- `AUTH_CLIENT`: Auth0 client ID
- `AUTH_SECRET`: Auth0 client secret
- `AUTH_ISSUER`: Auth0 issuer URI
- `AUTH_AUDIENCE`: Auth0 API identifier for mobile JWT validation (must match mobile `AUTH0_AUDIENCE`)
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

## Patient mobile API (tracking)

Issue registry: [`ISSUE.md`](ISSUE.md). Agent workflow: [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md). Contract docs: [`docs/mobile-api/README.md`](docs/mobile-api/README.md).

## Subscription & access enforcement (tracking)

Issue registry: [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md). Agent workflow: [`SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md`](SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md). Design: [`docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md`](docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md).

**Epic #180–#211** (2026-06-19): ~~#46~~ Liquibase done; ~~#180–#185~~, ~~#190~~, ~~#187~~, ~~#210~~, ~~#211~~ on `main` (PRs [#216](https://github.com/diego-torres/nutriconsultas/pull/216), [#218](https://github.com/diego-torres/nutriconsultas/pull/218), [#224](https://github.com/diego-torres/nutriconsultas/pull/224), [#230](https://github.com/diego-torres/nutriconsultas/pull/230)). **NEXT:** #207 (+ #208 Stripe ops). See [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md).

**Done on `main` (2026-06-14):** #107/#109/#110 (JWT + linkage + DTO envelope); endpoints #91–#98; messages #96/#97 with rate limit (#113); localized errors (#111, PR #151); dashboard IMC gauge (#106).

**Next:** [#134](https://github.com/diego-torres/nutriconsultas/issues/134) create patient invitation (~~#133~~ PR [#229](https://github.com/diego-torres/nutriconsultas/pull/229) on `main`). ~~#114~~, ~~#116~~, ~~#115~~, ~~#112~~ **done**. Subscription: [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md) — ~~#211~~ done (PR [#230](https://github.com/diego-torres/nutriconsultas/pull/230)); **NEXT** #207.

**Schema gate (post-#46):** [#46 Liquibase](https://github.com/diego-torres/nutriconsultas/issues/46) baseline is on `main` (PR #196). All new schema/catalog changes require **incremental Liquibase changesets** — see [`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md) and [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md). ~~#156~~ Phase C done before baseline.

## Nutritionist web — patient MPX (tracking)

Issue registry: [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md). Plan: [`docs/paciente/PATIENT-MPX-PLAN.md`](docs/paciente/PATIENT-MPX-PLAN.md).

**Epic #221–#223** (2026-06-20): export/import patient **registration** to `.mpx` (YAML, no history) + export/delete UI. ~~#221~~ **done** (PR [#254](https://github.com/diego-torres/nutriconsultas/pull/254)). ~~#222~~ **done** (PR [#261](https://github.com/diego-torres/nutriconsultas/pull/261)). ~~#223~~ **done** (PR [#262](https://github.com/diego-torres/nutriconsultas/pull/262)). **NEXT:** [#232 diet catalog](https://github.com/diego-torres/nutriconsultas/issues/232). Complements #190 patient caps; available all tiers.

**Epics #232–#242** (2026-06-19): diet catalog (#232–#235), branding (#236–#237), diet/platillo UX (#238–#240), patient UX (#241–#242). **Epic #257–#259** (2026-06-19): platillo catalog ownership (lock system rows, creator edit, copy). See [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md).

**Bug ~~#250~~** (2026-06-19): diet ingesta platillo name links to wrong catalog platillo — **done** (PR [#256](https://github.com/diego-torres/nutriconsultas/pull/256)).

## Public booking (tracking)

Issue registry: [`ISSUE-PUBLIC-BOOKING.md`](ISSUE-PUBLIC-BOOKING.md). Epic **#245–#248** (2026-06-19): shareable `/consultas/{id}/agendar-cita` link. **NEXT (when active):** [#246 working hours](https://github.com/diego-torres/nutriconsultas/issues/246) → #247 → #248.

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)

