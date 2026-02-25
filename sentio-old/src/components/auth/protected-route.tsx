import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/auth';

export const ProtectedRoute = () => {
    const { loggedIn, isLoading } = useAuth();

    if (isLoading) {
        return (
            <div className="flex h-screen w-full items-center justify-center">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
        );
    }

    if (!loggedIn) {
        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
};
