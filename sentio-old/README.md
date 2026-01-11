# Sentio Web Frontend

Frontend application for the Sentio platform, built with React, TypeScript, and Vite.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Development](#development)
- [Building for Production](#building-for-production)
- [Docker](#docker)
- [API Integration](#api-integration)
- [Project Structure](#project-structure)
- [Code Style & Best Practices](#code-style--best-practices)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Environment Variables](#environment-variables)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview

The Sentio Web Frontend provides the user interface for the Sentio platform. This repository contains the complete frontend codebase, implementing both landing pages and dashboard functionality.

## Tech Stack

- **Framework**: React 19.1.0
- **Language**: TypeScript 5.8.3
- **Build Tool**: Vite 7.0.0
- **Styling**: TailwindCSS 4.1.11
- **3D Rendering**: Three.js 0.179.1, React Three Fiber 9.3.0
- **Animation**: GSAP 3.13.0
- **UI Components**: Custom components with Radix UI primitives
- **Data Visualization**: Recharts 3.0.2
- **Routing**: React Router 7.6.3
- **HTTP Client**: Custom fetch wrapper
- **Linting**: ESLint 9.29.0

## Architecture

The application follows a component-based architecture with a clear separation of concerns:

- **Pages**: Top-level route components
- **Components**: Reusable UI elements organized by feature area
- **Services**: API integration and data fetching logic
- **Hooks**: Reusable stateful logic
- **Utils**: Helper functions and utilities

## Prerequisites

- Node.js 18.x or higher
- npm 9.x or higher
- Docker (for containerized deployment)

## Installation

```shell script
# Clone the repository
git clone https://github.com/your-org/sentio-web.git
cd sentio-web

# Install dependencies
npm install
```


## Development

```shell script
# Start the development server
npm run dev

# Run ESLint
npm run lint

# Run ESLint with auto-fix
npm run lint:fix
```


The development server will be available at [http://localhost:5173](http://localhost:5173).

## Building for Production

```shell script
# Build the application
npm run build

# Preview the production build locally
npm run preview
```


The production build will be available in the `dist` directory.

## Docker

### Building the Docker Image

```shell script
docker build -t sentio-web:latest .
```


### Running with Docker Compose

```shell script
docker-compose up
```


The containerized application will be available at [http://localhost:3000](http://localhost:3000).

## API Integration

The application communicates with the Sentio backend API using a custom fetch-based client. API base URL and other configuration can be set via environment variables.

Service modules in the `src/services` directory handle specific API domains:

- `birdService.ts`: Bird detection and information
- `weatherService.ts`: Weather data
- `forecastService.ts`: Weather forecasts
- `aiSummaryService.ts`: AI-generated insights
- `wikipediaService.ts`: External data from Wikipedia

## Project Structure

```
sentio-web/
├── public/              # Static assets
├── src/
│   ├── assets/          # Application assets (images, fonts)
│   ├── components/      # Reusable UI components
│   │   ├── dashboard/   # Dashboard-specific components
│   │   ├── landing/     # Landing page components
│   │   ├── layout/      # Layout components
│   │   ├── shared/      # Shared components
│   │   └── ui/          # Base UI components
│   ├── hooks/           # Custom React hooks
│   ├── lib/             # External libraries and configurations
│   ├── pages/           # Top-level page components
│   ├── services/        # API services
│   ├── styles/          # Global styles
│   ├── utils/           # Utility functions
│   ├── App.tsx          # Root component
│   └── main.tsx         # Application entry point
├── .dockerignore        # Docker ignore file
├── .env                 # Environment variables
├── .gitignore           # Git ignore file
├── components.json      # UI component configuration
├── Dockerfile           # Docker configuration
├── eslint.config.js     # ESLint configuration
├── index.html           # HTML template
├── nginx.conf           # Nginx configuration for Docker
├── package.json         # Dependencies and scripts
├── tsconfig.json        # TypeScript configuration
└── vite.config.ts       # Vite configuration
```


## Code Style & Best Practices

This project follows modern React best practices:

- Functional components with hooks
- TypeScript for type safety
- Component composition for UI
- Custom hooks for reusable logic
- Service modules for API interactions

ESLint is configured to enforce code quality and consistency. Run `npm run lint` to check for issues.

## Testing

TODO: Add testing documentation once test setup is implemented.

## CI/CD

TODO: Add CI/CD documentation once pipeline is established.

## Environment Variables

Create a `.env` file in the root directory with the following variables:

```
VITE_API_BASE_URL=http://localhost:8080
```


**Note**: Environment variables must be prefixed with `VITE_` to be accessible in the client code.

## Troubleshooting

### Common Issues

- **API Connection Issues**: Verify that the backend is running and the `VITE_API_BASE_URL` is correctly set.
- **Build Failures**: Ensure all dependencies are installed with `npm install`.
- **Docker Issues**: Check Docker logs with `docker-compose logs`.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

TODO: Add license information.

---

*This README is maintained by the Sentio development team.*