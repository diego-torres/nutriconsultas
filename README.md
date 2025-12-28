# nutriconsultas
registro de consultorio de nutrición

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

**SpotBugs:**
```bash
mvn spotbugs:check
```

**PMD:**
```bash
mvn pmd:check
```

**Thymeleaf Template Validation:**
```bash
mvn test-compile exec:java -Dexec.mainClass="com.nutriconsultas.ThymeleafValidator" -Dexec.args="src/main/resources/templates"
```

### Reports

After running the linting tools, reports are generated in the `target/` directory:

- **Checkstyle**: `target/checkstyle-result.xml`
- **SpotBugs**: `target/spotbugsXml.xml`
- **PMD**: `target/pmd.xml`

### Configuration Files

- `checkstyle.xml` - Checkstyle rules configuration
- `spotbugs-exclude.xml` - SpotBugs exclusion patterns for generated code

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

A pre-commit hook is installed to automatically validate and format code before each commit. The hook:

1. Checks if any staged Java files need formatting
2. Automatically formats files if needed
3. Re-stages the formatted files
4. Allows the commit to proceed

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
