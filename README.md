# nutriconsultas
registro de consultorio de nutrición

## Project Information

**Project Name:** nutriconsultas

**Company:** Minutriporcion

**Branding:** Minutriporcion is the product and branding name for the nutriconsultas project. The project "nutriconsultas" is developed for the company Minutriporcion.

## Contributing

When creating a pull request, please use the [PR template](.github/pull_request_template.md) which includes checklists for:
- Spring Boot best practices
- Thymeleaf template guidelines
- Code quality requirements
- Testing requirements
- Security and performance considerations

## Development Setup

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **Podman** (or Docker) for running the PostgreSQL database container
- **Podman Compose** (or Docker Compose) - optional but recommended

### Quick Start

The easiest way to start the development environment is using the provided `dev-start.sh` script:

```bash
./dev-start.sh
```

This script will:
1. Load environment variables from `.env` file (if present)
2. Check for Podman installation
3. Start or create the PostgreSQL database container
4. Wait for the database to be ready
5. Start the Spring Boot application

### Environment Configuration

Create a `.env` file in the project root to customize database settings:

```bash
# Database Configuration
POSTGRES_DB=nutriconsultas
POSTGRES_USER=nutriconsultas
POSTGRES_PASSWORD=nutriconsultas
POSTGRES_PORT=5432

# Spring Boot JDBC Configuration
JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/nutriconsultas
JDBC_DATABASE_USERNAME=nutriconsultas
JDBC_DATABASE_PASSWORD=nutriconsultas
```

If no `.env` file is present, the script will use default values.

### Manual Setup

If you prefer to set up the database manually:

1. **Start the database container:**
   ```bash
   podman compose up -d postgres
   ```
   Or using Docker:
   ```bash
   docker compose up -d postgres
   ```

2. **Run the Spring Boot application:**
   ```bash
   mvn spring-boot:run
   ```

### Database Container Management

**Check if the database container is running:**
```bash
podman ps | grep nutriconsultas-db
```

**Stop the database container:**
```bash
podman stop nutriconsultas-db
```

**Start the database container:**
```bash
podman start nutriconsultas-db
```

**Remove the database container:**
```bash
podman rm -f nutriconsultas-db
```

**View database logs:**
```bash
podman logs nutriconsultas-db
```

### Troubleshooting

**Database connection issues:**
- Ensure the database container is running: `podman ps`
- Check if the port is already in use: `lsof -i :5432`
- Verify environment variables are set correctly

**Podman not found:**
- Install Podman: `brew install podman` (macOS) or follow [Podman installation guide](https://podman.io/getting-started/installation)
- Alternatively, you can use Docker by modifying the script or using `docker compose` commands

**Port conflicts:**
- Change the `POSTGRES_PORT` in your `.env` file if port 5432 is already in use
- Update the `JDBC_DATABASE_URL` accordingly

## Code Quality Tools

This project uses several linting and formatting tools to maintain code quality and consistency.

### Available Tools

- **Spring Java Format**: Automatic code formatting following Spring's coding standards
- **Checkstyle**: Code style checking and enforcement
- **SpotBugs**: Static analysis for bug detection
- **PMD**: Code quality analysis and common programming flaw detection
- **Thymeleaf Validator**: Template validation for Thymeleaf HTML files

### Quick Start

#### Format Code

To automatically format all Java code:

```bash
mvn spring-javaformat:apply
```

This will format all Java source files according to Spring's coding standards. The formatting is automatically applied during the `validate` phase of the Maven build lifecycle.

**Fixing Formatting Issues:**
- Run `mvn spring-javaformat:apply` to fix all formatting issues automatically
- The pre-commit hook runs checkstyle validation (not automatic formatting)
- Formatting issues are typically spacing, indentation, and brace placement

#### Run All Linting Tools

To run all linting and formatting tools at once:

```bash
./lint.sh
```

This script will:
1. Format Java code with Spring Java Format
2. Run Checkstyle checks
3. Run SpotBugs analysis
4. Run PMD analysis
5. Validate Thymeleaf templates

#### Individual Tool Commands

**Checkstyle:**
```bash
mvn checkstyle:check
```

**Fixing Checkstyle Issues:**
- Review the report at `target/checkstyle-result.xml`
- Common issues and fixes:
  - **Line length**: Keep lines under 120 characters
    - Break long lines into multiple lines
    - Use method chaining on separate lines
    - Extract complex expressions into variables
  - **Naming conventions**: Follow Java naming standards
    - Classes: PascalCase (e.g., `MyClass`)
    - Methods/Variables: camelCase starting with lowercase (e.g., `myMethod`, `myVariable`)
    - **NEVER use underscore prefix for variables** (e.g., `_variable` is invalid, use `variable` or `variableEntity`)
    - Constants: UPPER_SNAKE_CASE (e.g., `MY_CONSTANT`)
  - **Missing braces (NeedBraces)**: Always use braces for if/for/while statements
    - ❌ Bad: `if (condition) statement;`
    - ✅ Good: `if (condition) { statement; }`
  - **RightCurly**: Closing braces should be on the same line as else/catch/finally
    - ❌ Bad: `} else {` on separate lines
    - ✅ Good: `} else {` on same line
  - **VisibilityModifier**: Fields should be private with accessor methods
    - ❌ Bad: `public String name;` or `static Map map;`
    - ✅ Good: `private String name;` with getter/setter, or `private static final Map MAP;`
  - **FinalClass**: Utility classes should be declared as `final`
    - ❌ Bad: `public class MyComparators { }`
    - ✅ Good: `public final class MyComparators { }`
  - **HideUtilityClassConstructor**: Utility classes should have private constructors
    - ❌ Bad: `public class WebContextFactory { }` (implicit public constructor)
    - ✅ Good: `public class WebContextFactory { private WebContextFactory() { } }`
  - **Unused imports**: Remove unused imports
  - **Whitespace**: Follow consistent whitespace rules
    - No whitespace after opening brace: `{` not `{ `
- Most formatting issues can be auto-fixed by running `mvn spring-javaformat:apply`
- For naming issues, manually rename variables/classes to match conventions

**SpotBugs:**
```bash
mvn spotbugs:check
```

**Fixing SpotBugs Issues:**
- Review the report at `target/spotbugsXml.xml` or HTML report at `target/reports/spotbugs.html`
- Common issues:
  - **Null pointer dereferences**: Add null checks
  - **Resource leaks**: Ensure resources are closed in finally blocks or use try-with-resources
  - **Inefficient operations**: Optimize string concatenation, use StringBuilder for loops
  - **Bad practices**: Fix equals/hashCode implementations, avoid comparing strings with ==
- Some issues may be false positives and can be excluded in `spotbugs-exclude.xml`

**PMD:**
```bash
mvn pmd:check
```

**Fixing PMD Issues:**
- Review the report at `target/pmd.xml` or HTML report at `target/reports/pmd.html`
- Common issues and fixes:
  - **MethodArgumentCouldBeFinal**: Add `final` keyword to method parameters that aren't reassigned
  - **LocalVariableCouldBeFinal**: Add `final` keyword to local variables that aren't reassigned
  - **OnlyOneReturn**: Refactor methods to use a single return point (use a result variable)
  - **LongVariable**: Shorten excessively long variable names
  - **ShortVariable**: Loop variables like `i`, `j`, `k` are acceptable (excluded in custom ruleset)
  - **UnusedAssignment**: Remove unused variable assignments
  - **GuardLogStatement**: Wrap logger calls with log level checks (e.g., `if (logger.isDebugEnabled())`)
  - **AvoidCatchingGenericException**: Catch specific exceptions instead of generic `Exception`
- The project uses a custom PMD ruleset (`pmd-ruleset.xml`) that excludes ShortVariable for loop variables
- Most violations can be fixed by adding `final` keywords and refactoring methods

**Thymeleaf Template Validation:**
Template validation runs automatically during the test lifecycle via `ThymeleafTemplateValidationTest`. To run it:
```bash
mvn test -Dtest=ThymeleafTemplateValidationTest
```

Or run all tests (which includes template validation):
```bash
mvn test
```

To run validation manually (standalone):
```bash
mvn exec:java -Dexec.mainClass="com.nutriconsultas.ThymeleafValidator" -Dexec.args="src/main/resources/templates"
```

**Note:** Template validation no longer blocks application startup. It runs during the test phase, allowing `mvn spring-boot:run` to start the application even if templates need fixes.

### Testing Requirements

**IMPORTANT: All code changes must include tests and template validations.**

When making changes to the codebase:

1. **Always create tests for new functionality:**
   - Service classes: Create `XxxServiceTest.java` with unit tests
   - Controller classes: Create or update `XxxControllerTest.java` with integration tests
   - Test all public methods, edge cases, and error conditions
   - Use JUnit 5, Mockito, and AssertJ

2. **Always update template validators for new templates:**
   - If creating new Thymeleaf templates, update the corresponding `XxxTemplateValidator`
   - Add all mock model variables needed for template validation
   - Ensure templates can be validated without runtime errors

3. **Run tests before committing:**
   ```bash
   mvn test
   ```
   This will run all unit tests, integration tests, and template validations.

4. **Test coverage expectations:**
   - New services: Test all public methods
   - New controllers: Test all endpoints
   - New templates: Ensure validator includes all required variables

### Template Validation Architecture

The Thymeleaf validator uses a modular architecture where each template or template group has its own validator class. This allows for template-specific mock model variables to be defined based on the needs of each template.

**Package Structure:**
- `com.nutriconsultas.validation.template` - Template validator package
  - `TemplateValidator` - Interface for template validators
  - `BaseTemplateValidator` - Base implementation with common mocks
  - `TemplateValidatorRegistry` - Registry that manages all validators
  - `WebContextFactory` - Factory for creating web contexts
  - Individual validators:
    - `PacienteTemplateValidator` - For `sbadmin/pacientes/*` templates
    - `PlatilloTemplateValidator` - For `sbadmin/platillos/*` templates
    - `DietaTemplateValidator` - For `sbadmin/dietas/*` templates
    - `AlimentoTemplateValidator` - For `sbadmin/alimentos/*` templates
    - `EternaTemplateValidator` - For `eterna/*` templates
    - `DefaultTemplateValidator` - Fallback for templates without specific validators

**Creating a New Template Validator:**

1. Create a new class extending `BaseTemplateValidator`:
```java
public class MyTemplateValidator extends BaseTemplateValidator {
    @Override
    public String getTemplatePathPattern() {
        return "sbadmin/mymodule/*";
    }
    
    @Override
    public Map<String, Object> createMockModelVariables() {
        Map<String, Object> variables = super.createMockModelVariables();
        // Add template-specific mocks
        variables.put("myObject", createMockBean("id", 0L, "name", ""));
        return variables;
    }
}
```

2. Register it in `TemplateValidatorRegistry`:
```java
register(new MyTemplateValidator());
```

**Validation Requirements:**
- Each template validator must implement `TemplateValidator` interface
- Validators define mock model variables needed for their templates
- The registry finds the appropriate validator based on template path patterns
- Templates are validated with template-specific mocks, ensuring accurate validation

### Reports

After running the linting tools, reports are generated in the `target/` directory:

- **Checkstyle**: `target/checkstyle-result.xml`
- **SpotBugs**: `target/spotbugsXml.xml`
- **PMD**: `target/pmd.xml`

### Configuration Files

- `checkstyle.xml` - Checkstyle rules configuration
- `spotbugs-exclude.xml` - SpotBugs exclusion patterns for generated code
- `pmd-ruleset.xml` - Custom PMD ruleset configuration (excludes ShortVariable for loop variables)

### IDE Integration

For the best development experience, consider installing IDE plugins:

- **IntelliJ IDEA**: Install the Checkstyle-IDEA plugin and Spring Java Format plugin
- **Eclipse**: Install the Checkstyle plugin and Spring Java Format plugin

### Build Integration

All linting tools are configured to run automatically during the Maven build lifecycle:

- Checkstyle runs during the `validate` phase
- SpotBugs and PMD run during the `verify` phase
- Spring Java Format runs during the `validate` phase
- Thymeleaf validation runs during the `validate` phase

To skip linting during build (not recommended):

```bash
mvn clean install -Dcheckstyle.skip=true -Dspotbugs.skip=true -Dpmd.skip=true
```

### CI/CD Integration

The GitHub Actions workflow (`.github/workflows/maven.yml`) automatically runs linting validation on every push and pull request:

1. **Lint Job** - Runs before the build:
   - Verifies code formatting with Spring Java Format
   - Runs Checkstyle (with strict mode enabled via `ci` profile)
   - Runs SpotBugs
   - Runs PMD
   - Validates Thymeleaf templates
   - Uploads linting reports as artifacts

2. **Build Job** - Runs after linting passes:
   - Builds the project
   - Runs tests
   - Uploads test results

The workflow uses the `ci` Maven profile which enables strict linting (fails on errors). For local development, linting tools report issues but don't fail the build by default.

**View linting results:**
- Go to the Actions tab in GitHub
- Click on a workflow run
- Download the artifacts (checkstyle-results, spotbugs-results, pmd-results) to view detailed reports

### Git Pre-commit Hook

A pre-commit hook is installed to validate code quality before each commit. The hook:

1. Checks if any Java files are staged
2. Runs checkstyle validation to ensure code quality standards
3. Blocks the commit if checkstyle violations are found
4. Allows the commit to proceed if all checks pass

#### Installation

For new clones or to reinstall the hook:

```bash
./setup-git-hooks.sh
```

Or manually copy the hook:

```bash
cp git-hooks/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

The hook will run automatically on every commit.

#### Usage

**To bypass the hook** (not recommended):

```bash
git commit --no-verify
```

**To manually test the hook:**

```bash
.git/hooks/pre-commit
```

**Note:** The hook formats all Java files in the project (Spring Java Format doesn't support formatting only specific files). If you have uncommitted changes, they will be formatted as well. It's recommended to commit or stash changes before committing.
