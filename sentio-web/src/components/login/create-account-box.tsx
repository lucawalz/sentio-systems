import { useEffect } from "react";
import { authService } from "../../services/authService";

export function CreateAccountBox() {
    useEffect(() => {
        authService.initiateRegister();
    }, []);

    return (
        <div className="flex items-center justify-center min-h-screen">
            <p className="text-white">Redirecting to registration...</p>
        </div>
    );
}