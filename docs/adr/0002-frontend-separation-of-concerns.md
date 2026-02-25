# 2. Frontend Separation of Concerns

Date: 2026-02-25

## Status

Accepted

## Context

Inside the `sentio-web` React application, React components were becoming overly bloated. They were explicitly handling HTTP `fetch` and `axios` API calls, defining complex password requirements, parsing granular error responses, and executing generic business logic (such as decompressing radar data). This coupling made testing and reusability very difficult, leading to duplicated code in various parts of the application. 

## Decision

We are migrating our frontend architecture to rigidly enforce the **Separation of Concerns**. We are adopting a layered architecture pattern:

1. **Services (`src/services/`)**: Responsible solely for network requests and communicating with REST APIs. Components must never use `fetch` or `axios` directly. Instead, they call asynchronous methods from domain-specific services like `authService`, `contactService`, and `weatherService`.
2. **Hooks/Stores (`src/hooks/`, `src/stores/`)**: Responsible for managing complex, granular state or specialized business logic (e.g., matching password requirements and observing live form states) isolated away from the view layout level. Custom hooks allow views to remain pure and simple.
3. **Utilities (`src/utils/`)**: Responsible for pure, stateless functions. These include reusable methods such as formatting timestamps, decoding specific binary strings (like radar data), or generically parsing Axios API errors.
4. **Components (`src/components/`)**: The View layer. Responsible strictly for rendering the UI elements and asynchronously orchestrating interactions between the Services and Hooks using cleanly separated loading states (`isLoading`).

## Consequences

* **Positive:**
  * Dramatically improved testability. Business logic and API calls can now be tested in isolation without rendering complicated DOM elements.
  * Reusability. Shared logic (e.g., user password requirements or error handling) is now contained in generic utilities/hooks instead of copied across Registration and Password Reset forms.
  * Clearer, much simpler React components focused entirely on the user interface.

* **Negative:**
  * Slight increase in boilerplate. Adding a new API call now requires declaring the network request in `src/services/` before consuming it in the component.
