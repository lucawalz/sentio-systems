import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../../context/auth";

export function LoginBox() {
    const navigate = useNavigate();
    const { login, isLoading } = useAuth();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        try {
            await login(username, password);
            navigate("/dashboard");
        } catch (err: any) {
            if (err.statusCode === 401) {
                setError("Invalid username or password");
            } else {
                setError(err.message || "Login failed. Please try again.");
            }
        }
    };

    return (
        <div>
            {/* Background */}
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black" />

            {/* Login-Box */}
            <div className="relative w-full max-w-md p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur">
                <h2 className="text-3xl font-bold mb-6 text-center">Sentio Systems</h2>
                <p className="text-gray-400 mb-8 text-center">Sign in to access your dashboard</p>

                {error && (
                    <div className="mb-4 p-3 bg-red-500/10 border border-red-500/50 text-red-500 rounded text-sm text-center">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">Username</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="w-full px-4 py-2 bg-black/50 border border-gray-700 rounded-lg focus:outline-none focus:border-white text-white"
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full px-4 py-2 bg-black/50 border border-gray-700 rounded-lg focus:outline-none focus:border-white text-white"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        className="w-full mt-6 px-6 py-3 bg-white text-black rounded-lg font-semibold hover:bg-gray-200 transition-colors disabled:opacity-50"
                        disabled={isLoading}
                    >
                        {isLoading ? "Signing in..." : "Sign In"}
                    </button>
                </form>

                <div className="mt-8 pt-6 border-t border-gray-800 text-center">
                    <Link className="text-sm text-gray-400 hover:text-white transition-colors" to="/create-account">
                        Need an account? <span className="underline">Create one</span>
                    </Link>
                </div>
            </div>
        </div>
    );
}
