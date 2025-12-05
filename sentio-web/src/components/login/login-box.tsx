import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../../context/auth";

export function LoginBox() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const { login, isLoading, error, clearError } = useAuth();
    const navigate = useNavigate();

    const canSubmit = username.length > 0 && password.length > 0;

    async function handleLogin(e: React.FormEvent) {
        e.preventDefault();
        clearError();

        const success = await login(username, password);
        if (success) {
            navigate("/dashboard");
        }
    }

    return (
        <div>
            {/* Background */}
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black" />

            {/* Login-Box */}
            <div className="relative w-full max-w-md p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center">
                <h2 className="text-3xl font-bold mb-6">Login</h2>

                <form onSubmit={handleLogin}>
                    <input
                        type="text"
                        placeholder="username"
                        className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        disabled={isLoading}
                        autoComplete="username"
                    />

                    <input
                        type="password"
                        placeholder="password"
                        className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        disabled={isLoading}
                        autoComplete="current-password"
                    />

                    {/* Error message */}
                    {error && (
                        <p className="text-red-400 text-sm mb-4">{error}</p>
                    )}

                    {/* Forgot password link */}
                    {username.length > 0 && (
                        <a
                            href="/forgot-password"
                            className="block text-sm text-gray-300 underline hover:text-white mb-4"
                        >
                            Forgot password?
                        </a>
                    )}

                    <button
                        type="submit"
                        className="w-full px-6 py-3 bg-white text-black rounded-lg font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                        disabled={!canSubmit || isLoading}
                    >
                        {isLoading ? "Logging in..." : "Login"}
                    </button>
                </form>

                <Link className="w-full mt-4 px6 py-3 text-white" to="/create-account">
                    <p className="mt-6 mb-6 underline">Create account</p>
                </Link>
            </div>
        </div>
    );
}
