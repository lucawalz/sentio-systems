# AI Usage Documentation

This document provides transparency on the use of AI-assisted tools during the development of the Sentio Systems platform.

## AI Tools Used

| Tool | Purpose |
|------|---------|
| **Claude (Anthropic)** | **Architecture & Design**: Defining general program flow and system structure<br>**Development**: Code generation, debugging, and documentation |
| **Gemini (Google)** | **Marketing & Branding**: Wording assistance, frontend copy, and brand markup strategies<br>**Development**: Styling (Tailwind, CSS) |
| **Antigravity (Google)** | **Development**: Converting and implementing design concept into the project, styling, code generation |
| **ChatGPT (OpenAI)** | **Development**: Building unit tests, styling (Tailwind), debugging |
| **Grok Code Fast 1 (xAI)** | **Development**: Documentation, code structuring |

## Usage by Component

| Component | AI-Assisted Areas |
|-----------|-------------------|
| **sentio-backend** | Boilerplate code generation, project setup, API scaffolding, database schema design |
| **sentio-web** | Boilerplate generation, layout drafting, directory structure planning, UI component implementation |
| **sentio-embedded** | **Performance Optimization**: Efficient bounding box visualization without frame loss<br>**Integration**: Complex branching logic and camera stream handling<br>**Testing**: Generation of validation scripts |
| **Infrastructure** | Understanding and implementing CI/CD pipelines, Docker/K8s configurations |
| **Documentation** | General code documentation, READMEs, ADRs |
| **Project Management** | Drafting Issue descriptions and Pull Request templates/summaries |

## Development Practices

All AI-generated code was:

- **Reviewed** by team members before merging
- **Tested** through automated CI/CD pipelines
- **Adapted** to fit project conventions and requirements

AI was used as a **development accelerator** and **learning tool** (especially for CI/CD concepts), not a replacement for understanding. Team members actively reviewed, modified, and validated all AI suggestions.

## Scope of AI Assistance

| Category | Examples |
|----------|----------|
| **Code Generation** | Boilerplate code, utility functions, component templates, test scripts |
| **Architecture** | System design discussions, general flow planning, directory structure |
| **Performance** | Optimization strategies for embedded devices (e.g., streaming latency, rendering) |
| **Marketing** | Brand voice consistency, frontend copy, styling decisions |
| **Project Management** | Creating detailed Issue reports and PR descriptions |
| **DevOps** | CI/CD workflow understanding and implementation |
