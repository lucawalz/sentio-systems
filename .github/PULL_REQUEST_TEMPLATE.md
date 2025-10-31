## Pull Request

<!-- 
Replace [TYPE-NUMBER] with the relevant issue ID (e.g., REQ-001, BUG-015, HOTFIX-003)
Ensure the PR title follows: "TYPE-NUMBER: Brief description"
-->

**Related Issue:** Closes #[ISSUE_NUMBER]

---

## Type of Change

<!-- Mark the relevant option with an 'x' -->

- [ ] New feature (REQ)
- [ ] Bug fix (BUG)
- [ ] Hotfix (HOTFIX)
- [ ] Documentation update
- [ ] Refactoring (no functional changes)
- [ ] Style/UI changes
- [ ] Performance improvement
- [ ] Test updates
- [ ] Configuration/infrastructure changes

---

## Description

<!-- Provide a clear and concise description of the changes -->

**What:** 
<!-- What was changed/implemented? -->

**Why:** 
<!-- Why was this change necessary? -->

**How:** 
<!-- How was it implemented? -->

---

## Affected Components

<!-- Mark all that apply with an 'x' -->

- [ ] `sentio-backend`
- [ ] `sentio-web`
- [ ] `sentio-ai/birder`
- [ ] `sentio-ai/preprocessing`
- [ ] `sentio-ai/speciesnet`
- [ ] `sentio-embedded`
- [ ] Infrastructure/Deployment
- [ ] Documentation

---

## Acceptance Criteria Met

<!-- Copy acceptance criteria from the related issue and mark completed items -->

- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

---

## Testing

### Test Coverage

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] E2E tests added/updated
- [ ] Manual testing performed
- [ ] No tests required (explain why below)

### Testing Steps

<!-- Describe how reviewers can test these changes -->

1. 
2. 
3. 

### Test Results

<!-- Provide evidence of successful testing (command output, screenshots, etc.) -->
```

# Example: Test execution output
All tests passed: 45/45
Coverage: 87%
```
---

## Screenshots / Demos

<!-- If applicable, add screenshots, GIFs, or video links demonstrating the changes -->

**Before:**

**After:**

---

## Dependencies

<!-- List any dependencies on other PRs, issues, or external changes -->

- Depends on #[ISSUE_NUMBER]
- Blocked by #[ISSUE_NUMBER]
- Related to #[ISSUE_NUMBER]

---

## Checklist

### Code Quality

- [ ] Code follows project style guidelines
- [ ] Self-review performed
- [ ] Code is well-commented (especially complex logic)
- [ ] No console warnings or errors
- [ ] No unnecessary console.log / debug statements
- [ ] Proper error handling implemented

### Documentation

- [ ] README.md updated (if needed)
- [ ] API documentation updated (if needed)
- [ ] Code comments added for complex logic
- [ ] Architecture docs updated (if needed)

### Security & Performance

- [ ] No sensitive data exposed (API keys, passwords, etc.)
- [ ] Input validation implemented
- [ ] SQL injection / XSS vulnerabilities addressed
- [ ] Performance impact considered
- [ ] Memory leaks checked

### Dependencies

- [ ] New dependencies justified and documented
- [ ] Package versions pinned appropriately
- [ ] Lock files updated (package-lock.json, pom.xml)

### Database & Migrations

- [ ] Database migrations included (if applicable)
- [ ] Migration tested on clean database
- [ ] Rollback procedure documented

### CI/CD

- [ ] All CI checks passing
- [ ] Build succeeds
- [ ] Docker images build successfully (if applicable)
- [ ] Deployment tested in staging (if applicable)

---

## Deployment Notes

<!-- Instructions for deployment, configuration changes, or environment variables -->

### Environment Variables

<!-- List any new or modified environment variables -->
```bash
# Example
NEW_API_KEY=your_api_key_here
```
### Configuration Changes

<!-- Describe any configuration file changes -->

### Migration Steps

<!-- If this requires manual steps during deployment -->

1. 
2. 
3. 

### Rollback Procedure

<!-- How to rollback if this change causes issues -->

---

## Review Focus Areas

<!-- Guide reviewers to areas that need special attention -->

- 
- 
- 

---

## Additional Notes

<!-- Any other information that reviewers should know -->

---

## Post-Merge Actions

<!-- Tasks to be completed after merging -->

- [ ] Update project documentation
- [ ] Notify team in Slack/Discord
- [ ] Update related issues
- [ ] Deploy to staging
- [ ] Deploy to production
- [ ] Monitor logs for errors

---

<!-- 
Branch naming reminders:
- Feature: feature/REQ-XXX-description (from develop)
- Bugfix: bugfix/issue-YYY (from develop)
- Hotfix: hotfix/critical-fix (from main)

Commit message format:
TYPE-NUMBER: Brief description (Closes #ISSUE_NUMBER)
-->