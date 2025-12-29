## Description

<!-- Provide a clear and concise description of what this PR does -->

## Type of Change

<!-- Mark the relevant option with an 'x' -->

- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ✨ New feature (non-breaking change which adds functionality)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] 📚 Documentation update
- [ ] 🔧 Refactoring (no functional changes)
- [ ] ⚡ Performance improvement
- [ ] 🧪 Test addition or update
- [ ] 🔨 Build/CI changes

## Changes Made

<!-- List the main changes in this PR -->

- 
- 
- 

## Related Issues

<!-- Link related issues using: Closes #issue, Fixes #issue, Relates to #issue -->

Closes #

## Spring Boot Specific Checklist

<!-- Mark completed items with an 'x' -->

- [ ] Code follows Spring Boot best practices
- [ ] Proper use of `@Service`, `@Repository`, `@Controller`, `@RestController` annotations
- [ ] Dependency injection is used correctly (constructor injection preferred)
- [ ] Configuration properties are properly externalized (if applicable)
- [ ] Database migrations are included (if schema changes)
- [ ] Transaction management is properly handled
- [ ] Exception handling follows Spring Boot patterns
- [ ] Security considerations addressed (if applicable)
- [ ] Actuator endpoints are not exposed in production (if modified)

## Thymeleaf Specific Checklist

<!-- Mark completed items with an 'x' -->

- [ ] Thymeleaf templates use proper syntax (`th:*` attributes)
- [ ] Template fragments are used appropriately (`th:fragment`, `th:insert`, `th:replace`)
- [ ] Internationalization (i18n) is handled correctly (if applicable)
- [ ] Template expressions are safe and properly escaped
- [ ] No inline JavaScript with unescaped data
- [ ] Forms use proper Thymeleaf form binding (`th:object`, `th:field`)
- [ ] Template validation passes (run `ThymeleafValidator`)
- [ ] All template references are correct (no broken links)
- [ ] Responsive design is maintained (if UI changes)

## Code Quality Checklist

<!-- Mark completed items with an 'x' -->

- [ ] Code is properly formatted (run `mvn spring-javaformat:apply`)
- [ ] Checkstyle passes (run `mvn checkstyle:check`)
- [ ] No SpotBugs warnings (run `mvn spotbugs:check`)
- [ ] PMD checks pass (run `mvn pmd:check`)
- [ ] All existing tests pass (`mvn test`)
- [ ] New tests are added for new functionality
- [ ] Test coverage is maintained or improved
- [ ] Code follows project naming conventions
- [ ] No hardcoded values (use configuration properties)
- [ ] Logging is appropriate (not too verbose, not too sparse)

## Testing

<!-- Describe the testing performed -->

### Manual Testing
- [ ] Tested locally with `./dev-start.sh`
- [ ] Database migrations work correctly (if applicable)
- [ ] All affected endpoints work as expected
- [ ] UI changes are tested in different browsers (if applicable)
- [ ] Mobile responsiveness verified (if UI changes)

### Automated Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated (if applicable)
- [ ] All tests pass locally
- [ ] Test coverage is adequate

## Documentation

<!-- Mark completed items with an 'x' -->

- [ ] README.md updated (if needed)
- [ ] Code comments added for complex logic
- [ ] JavaDoc added for public APIs (if applicable)
- [ ] API documentation updated (if applicable)
- [ ] Changelog updated (if applicable)

## Database Changes

<!-- If this PR includes database changes, fill out this section -->

- [ ] No database changes
- [ ] Migration script created
- [ ] Migration tested on local database
- [ ] Rollback strategy documented
- [ ] Data migration script included (if needed)

## Security Considerations

<!-- Mark completed items with an 'x' -->

- [ ] No sensitive data exposed in logs or responses
- [ ] Input validation is performed
- [ ] SQL injection prevention (using parameterized queries)
- [ ] XSS prevention (proper escaping in templates)
- [ ] CSRF protection maintained (if forms modified)
- [ ] Authentication/authorization checks are in place (if applicable)
- [ ] Dependencies are up to date (no known vulnerabilities)

## Performance Considerations

<!-- Mark completed items with an 'x' -->

- [ ] Database queries are optimized (no N+1 problems)
- [ ] Appropriate use of caching (if applicable)
- [ ] No memory leaks introduced
- [ ] Large data sets are handled efficiently
- [ ] Pagination is used where appropriate

## Deployment Notes

<!-- Any special deployment considerations -->

- [ ] No special deployment steps required
- [ ] Environment variables need to be updated
- [ ] Database migrations need to be run
- [ ] Configuration changes required
- [ ] Other: 

## Screenshots (if applicable)

<!-- Add screenshots for UI changes -->

## Additional Notes

<!-- Any additional information reviewers should know -->

