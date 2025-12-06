import { Link } from "react-router-dom";
import { useAuth } from "../../context/auth";

export function LoginBox() {
    const { login, isLoading } = useAuth();

    return (
        <div>
            {/* Background */}
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black" />

            {/* Login-Box */}
            <div className="relative w-full max-w-md p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center">
                <h2 className="text-3xl font-bold mb-6">Sentio Systems</h2>
                <p className="text-gray-400 mb-8">Sign in to access your dashboard</p>

                <button
                    onClick={() => login()}
                    className="w-full px-6 py-3 bg-white text-black rounded-lg font-semibold hover:bg-gray-200 transition-colors disabled:opacity-50"
                    disabled={isLoading}
                >
                    {isLoading ? "Redirecting..." : "Sign In with Keycloak"}
                </button>

                <div className="mt-8 pt-6 border-t border-gray-800">
                    <Link className="text-sm text-gray-400 hover:text-white transition-colors" to="/create-account">
                        Need an account? <span className="underline">Create one</span>
                    </Link>
                </div>
            </div>
        </div>
    );
}
