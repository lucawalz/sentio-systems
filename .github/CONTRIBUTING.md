

# Contributing to Sentio Systems

Thank you for your interest in contributing to Sentio Systems! We welcome contributions from the community.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Branching Strategy](#branching-strategy)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct (coming soon). Please be respectful and constructive in all interactions.

## Getting Started

### Prerequisites

- **Java**: JDK 21
- **Node.js**: v18+ (use `.nvmrc` if available)
- **Python**: 3.11+
- **Docker**: For running services locally
- **Git**: For version control

### Local Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/sentio-systems.git
   cd sentio-systems
   ```

2. **Set up environment variables**
   ```bash
   # Copy example env files
   cp sentio-backend/.env.example sentio-backend/.env
   cp sentio-web/.env.example sentio-web/.env
   cp sentio-ai/.env.example sentio-ai/.env
   ```

3. **Start services with Docker**
   ```bash
   docker-compose up -d
   ```

4. **Install dependencies**
   ```bash
   # Backend (Maven)
   cd sentio-backend && ./mvnw install
   
   # Web (npm)
   cd sentio-web && npm install
   
   # AI (pip)
   cd sentio-ai && pip install -r requirements.txt
   ```

5. **Run the application**
    - Follow module-specific README files in each subdirectory

## Development Workflow

### Branching Strategy

We follow **Git Flow**:

- `main` - Production-ready code
- `develop` - Integration branch for features
- `feature/REQ-XXX-description` - New features (from `develop`)
- `bugfix/issue-YYY-description` - Bug fixes (from `develop`)
- `hotfix/critical-fix` - Urgent production fixes (from `main`)

### Creating a New Feature

1. **Create an issue** using the appropriate template (REQ, bug, etc.)
2. **Create a branch** from `develop`:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/REQ-XXX-short-description
   ```

3. **Make your changes** following our coding standards
4. **Commit regularly** with clear messages
5. **Push and create a Pull Request**

## Commit Message Guidelines

Follow this format:
```
REQ-XXX: Short description of changes

Longer explanation if needed. Wrap at 72 characters.
Explain what and why, not how.

- Bullet points are fine
- Use present tense: "Add feature" not "Added feature"

Closes #XXX
```
### Examples
```

REQ-042: Add telemetry POST endpoint

Implement REST endpoint to receive telemetry data from Orbis Hub.
Includes request validation and database persistence.

Closes #42
```

```
bugfix: Fix temperature sensor reading error

Corrects parsing of negative temperature values from sensor.

Closes #87
```
## Pull Request Process

1. **Ensure your branch is up to date**
   ```bash
   git checkout develop
   git pull origin develop
   git checkout your-branch
   git rebase develop
   ```

2. **Run tests and linting**
   ```bash
   # Backend
   ./mvnw test
   
   # Web
   npm run lint
   npm test
   
   # AI
   pytest
   ```

3. **Update documentation** if you've changed APIs or added features

4. **Update `CHANGELOG.md`** under the `[Unreleased]` section with a summary of your changes using the appropriate category (`Added`, `Changed`, `Fixed`, `Removed`, `Security`, `Deprecated`)

5. **Fill out the PR template** completely

6. **Request review** from at least one team member

7. **Address feedback** promptly and professionally

8. **Squash commits** if requested before merging

### PR Checklist

- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] `CHANGELOG.md` updated under `[Unreleased]`
- [ ] Tests added/updated and passing
- [ ] No new warnings introduced
- [ ] Dependent changes merged
- [ ] Issue reference included (Closes #XXX)


## Coding Standards

We follow **Clean Code** principles throughout the codebase. Code should be readable, maintainable, and self-explanatory.

### Java (Backend)

- Follow **Clean Code** principles (Robert C. Martin)
- Use **Lombok** annotations (`@Data`, `@Builder`, `@AllArgsConstructor`, etc.) to reduce boilerplate
- Use **Spring Boot** best practices:
  - `@RestController` for API endpoints
  - `@Service` for business logic
  - `@Repository` for data access
  - Constructor-based dependency injection (prefer final fields)
- **Jakarta EE** imports (`jakarta.*` instead of `javax.*`)
- Use **Spring Data JPA** for database operations
- Write meaningful, descriptive names that reveal intent
- Keep methods small and focused (single responsibility)
- Avoid deep nesting - extract methods when needed
- Use optional chaining and null-safe operations

**Example Structure:**
```java
@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {
private final TelemetryService telemetryService;

    @PostMapping
    public ResponseEntity<TelemetryResponse> createTelemetry(@Valid @RequestBody TelemetryRequest request) {
        return ResponseEntity.ok(telemetryService.processTelemetry(request));
    }
}
```
### TypeScript/React (Web)

- **TypeScript strict mode** enabled
- Use **functional components** with hooks (no class components)
- **Clean component structure**:
  - Hooks at the top
  - Helper functions after hooks
  - Return JSX at the bottom
- **Proper typing**:
  - Type all props, state, and function parameters
  - Use interfaces for complex types
  - Avoid `any` - use `unknown` if type is truly unknown
- **Component organization**:
  - One component per file
  - Co-locate related components in feature folders
  - Separate business logic into custom hooks or services
- **Styling**:
  - Use **Tailwind CSS** utility classes
  - Use `clsx` or `cn()` for conditional classes
  - Component variants with `class-variance-authority`
- **Modern React patterns**:
  - Use `useRef` for DOM manipulation and animations (GSAP)
  - Memoization with `useMemo`/`useCallback` when appropriate
  - Custom hooks for reusable logic

**Example Component:**
```typescript
export function EnhancedDashboard() {
const [data, setData] = useState<DashboardData | null>(null)
const [loading, setLoading] = useState(true)
const cardRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        fetchDashboardData()
    }, [])

    const fetchDashboardData = async () => {
        try {
            const result = await DashboardService.getData()
            setData(result)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div ref={cardRef} className="dashboard-card">
            {/* JSX */}
        </div>
    )
}
```
### Python (AI/Services)

- Follow **Clean Code** and **PEP 8**
- Use **type hints** for function signatures
- **Document functions** with docstrings (Google style or NumPy style)
- **Modular structure**:
  - Separate concerns into focused functions
  - Keep functions small and testable
  - Use classes for stateful operations
- **Error handling**:
  - Use try-except blocks appropriately
  - Log errors with context
  - Return meaningful error responses
- **Flask/FastAPI conventions** for web services:
  - RESTful route naming
  - Proper HTTP status codes
  - JSON responses

**Example Service:**
```python
from flask import Flask, request, jsonify
from typing import Optional, Dict, Any
import logging

app = Flask(__name__)
logger = logging.getLogger(__name__)

@app.route('/enhance', methods=['POST'])
def enhance_image() -> Dict[str, Any]:
"""
Enhance uploaded image using computer vision techniques.

    Returns:
        JSON response with enhanced image path or error message
    """
    try:
        if 'image' not in request.files:
            return jsonify({'error': 'No image provided'}), 400
            
        result = process_image(request.files['image'])
        return jsonify({'success': True, 'data': result}), 200
        
    except Exception as e:
        logger.error(f"Image enhancement failed: {e}")
        return jsonify({'error': 'Processing failed'}), 500
```
### General Clean Code Principles

- **Meaningful Names**: Variables, functions, and classes should reveal intent
  - `getUserById()` not `get()`
  - `temperatureSensor` not `temp` or `t`
- **Single Responsibility**: Each function/class does one thing well
- **Small Functions**: Aim for functions under 20-30 lines
- **Don't Repeat Yourself (DRY)**: Extract common logic
- **Composition over Deep Nesting**: Extract nested logic into helper functions
- **Comments Explain "Why", Not "What"**: Code should be self-documenting
- **Consistent Formatting**: Use provided linters (ESLint, Prettier, Black)
- **Error Handling**: Handle errors gracefully, don't swallow exceptions
- **Avoid Magic Numbers**: Use named constants

### Code Smells to Avoid

**Avoid:**
- Long parameter lists (>3-4 parameters)
- Deep nesting (>3 levels)
- God classes/functions that do everything
- Primitive obsession (use domain objects)
- Comments that explain what code does (should be obvious)
- Dead code or commented-out code

**Prefer:**
- Small, focused functions
- Early returns to reduce nesting
- Descriptive variable names over comments
- Domain-driven design patterns
- Extracting complex conditions into well-named functions

## Testing Guidelines

### Backend (Java)

- Write unit tests with JUnit 5
- Use Mockito for mocking dependencies
- Aim for >80% code coverage
- Include integration tests for API endpoints

### Web (TypeScript)

- Write component tests with React Testing Library
- Test user interactions, not implementation details
- Mock external dependencies and API calls

### AI (Python)

- Use pytest for testing
- Test model inference with sample data
- Mock external APIs and file I/O

### Test Naming
```
java
// Java
@Test
void shouldReturnTelemetryData_whenValidRequest() { }

// TypeScript
it('should render species list when data is loaded', () => {})

# Python
def test_should_classify_bird_species_correctly():
```
## Documentation

- Update README files when adding features
- Document all public APIs
- Add inline comments for complex algorithms
- Update CHANGELOG.md with your changes
- Include examples in documentation

### API Documentation

- Backend: Use Swagger/OpenAPI annotations
- Document request/response schemas
- Provide example payloads

## Questions?

If you have questions:

1. Check existing documentation
2. Search closed issues
3. Ask in discussions (if enabled)
4. Reach out to maintainers

## Recognition

Contributors will be recognized in our CHANGELOG and release notes. Thank you for making Sentio Systems better!

---

**Happy coding! 🚀**
