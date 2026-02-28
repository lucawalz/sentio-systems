# Sentio Systems: Git Workflow & Branching Strategy

To maintain a clean, collaborative, and highly traceable codebase, Sentio Systems strictly enforces a structured Git workflow. This strategy guarantees we meet the **Grading Level 3 Requirements** for Git procedure ("git-Vorgehen inkl. branching dokumentiert und angewandt").

## 1. Branch Hierarchy Model

At the core of the Sentio platform is a standard `main` -> `develop` -> `feature/` branch hierarchy:

*   **`main`**: The production-ready branch. Code here is always stable. Commits merged into `main` trigger SemVer releases and deployment to our production infrastructure. Directly pushing to `main` is strictly prohibited.
*   **`develop`**: The primary integration branch. All feature branches branch off from `develop` and must be merged back into `develop` via a Pull Request.
*   **Feature Branches (`feature/`)**: Ephemeral branches created for individual tasks. They are branched off `develop` and deleted once merged back.
*   **Bugfix Branches (`bugfix/`)**: Branches specifically dedicated to resolving non-critical bugs found during the development lifecycle. Merged into `develop`.
*   **Hotfix Branches (`hotfix/`)**: Urgent fixes branched directly off `main` to repair production outages. Once resolved, they must be merged into *both* `main` and `develop`.

---

## 2. Branch Naming Conventions

All active feature branches must follow strict, descriptive naming conventions matching our Issue Templates:

*   **Features:** `feature/REQ-XXX-brief-description`
    *   *Example:* `feature/REQ-046-frontend-separation`
*   **Bugfixes:** `bugfix/BUG-YYY-brief-description`
    *   *Example:* `bugfix/BUG-015-login-crash`
*   **Hotfixes:** `hotfix/HOTFIX-ZZZ-brief-description`
    *   *Example:* `hotfix/HOTFIX-003-database-timeout`

---

## 3. Pull Request Requirements (Merge Rules)

Code can only enter `develop` or `main` under the following conditions:
1.  **Code Review:** At least 1 approving review from a team member.
2.  **CI Validation:** The Sentio CI/CD pipeline (Linting, Vitest, Backend Tests, Playwright E2E) must exit with a passing `0` status code.
3.  **Traceability checklist:** The PR template must accurately map out which `REQ` tickets are being satisfied within the branch's scope. 

**Merge vs Rebase Policy:**
We employ a **Merge Commit** topology when bringing feature branches into `develop`. Individual feature commits are preserved in the history, providing full traceability from PR merge commit back to every atomic change made during development.

---

## 4. Traceability & Commit Standards (REQ-045)

To satisfy the grading criterion *"Verknüpfung der Anforderungen untereinander, Verlinkung mit git-Commits"*, we must enforce strict bidirectional traceability between GitHub Issues and Git Commits.

### Commit Message Formatting
Every commit that involves business logic, bug fixing, or feature additions **must** reference its corresponding Requirement ID (e.g. `REQ-XXX`) at the beginning of the message.

*   **Format:** `<TYPE>-<ID>: <Description>`
*   **Example 1:** `REQ-046: Extract inline axios calls from WeatherRadar component`
*   **Example 2:** `BUG-015: Prevent null pointer exception on missing user tokens`

### Issue Linking
When executing a commit that entirely completes a requirement, explicitly trigger GitHub's auto-close webhook in the body:
```bash
git commit -m "REQ-039: Expand Frontend unit test coverage

Closes #104"
```

Dependent issues on GitHub must actively be cross-linked in the issue body or comments:
*   `Depends on #XX`
*   `Blocks #XX`

---

## 5. Tagging & Release Strategy

Sentio relies heavily on **Semantic Versioning (SemVer)** for releases. 
When code transitions from `develop` to `main`, a versioned tag is created manually and pushed to the remote (e.g., `v1.2.0`). Tags must be pushed explicitly via `git push --tags`.

*   `MAJOR`: Incompatible API or structural shifts.
*   `MINOR`: Backwards-compatible functionality additions (e.g., fulfilling an entire grading milestone).
*   `PATCH`: Backwards-compatible bug fixes or security patches.

*Active tags must be actively pushed and maintained via `git push --tags` to satisfy the "Tags vorhanden und gepflegt" requirement.*
