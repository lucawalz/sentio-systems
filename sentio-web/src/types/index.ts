/**
 * Global type definitions
 * 
 * Structure:
 * - types/index.ts      - Re-exports and common types
 * - types/api.ts        - API response/request types
 * - types/models.ts     - Domain models
 * - types/components.ts - Shared component prop types
 */

export type Nullable<T> = T | null;

export type AsyncState<T> = {
    data: T | null;
    isLoading: boolean;
    error: Error | null;
};
