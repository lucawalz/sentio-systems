import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/auth-context'
import { Skeleton } from '@/components/ui/skeleton'

interface ProtectedRouteProps {
    children: React.ReactNode
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
    const { isLoading, isAuthenticated } = useAuth()
    const location = useLocation()

    // Show loading skeleton while checking auth
    if (isLoading) {
        return (
            <div className="flex h-screen w-full items-center justify-center bg-background">
                <div className="flex flex-col items-center gap-4">
                    <div className="flex items-center gap-2">
                        <Skeleton className="h-8 w-8 rounded-full" />
                        <Skeleton className="h-4 w-32" />
                    </div>
                    <Skeleton className="h-4 w-48" />
                </div>
            </div>
        )
    }

    // Redirect to login if not authenticated
    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />
    }

    return <>{children}</>
}
