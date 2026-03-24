# Contributing to Sentio Systems

First of all, thank you for taking the time to contribute!

## Development Workflow

1. **Fork & Clone** the repository.
2. **Branch out** from `main` or `develop` into a reasonably named branch (`feat/your-feature`, `fix/your-fix`).
3. **Commit** your changes following the [Conventional Commits](#commit-message-guidelines) standard.
4. **Push** and open a Pull Request.

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. This leads to more readable messages that are easy to follow.

Structure:
```text
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Common Types:
- `feat:` A new feature
- `fix:` A bug fix
- `docs:` Documentation only changes
- `style:` Changes that do not affect the meaning of the code (white-space, formatting)
- `refactor:` A code change that neither fixes a bug nor adds a feature
- `perf:` A code change that improves performance
- `test:` Adding missing tests or correcting existing tests
- `chore:` Changes to the build process or auxiliary tools and libraries

Example:
```text
feat(backend): add weather forecast caching layer

Implemented Redis-based caching to prevent hitting the external API too frequently.
```

## Pull Requests

- Keep your PRs focused on a single change or feature.
- Ensure all tests pass before requesting review.
- Fill out the Pull Request template comprehensively.
- We will review your PR as soon as possible!
