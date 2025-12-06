import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authService } from "../../services/authService";
import type { RegisterRequest } from "../../services/authService";

//external error handling for password requirements + error messages
function getPasswordErrors(password: string): string[] {
    const errors: string[] = [];

    if (!/.{8,}/.test(password)) {
        errors.push("Password must be at least 8 characters long.");
    }
    if (!/[A-Z]/.test(password)) {
        errors.push("Password must contain at least 1 uppercase letter (A–Z).");
    }
    if (!/[a-z]/.test(password)) {
        errors.push("Password must contain at least 1 lowercase letter (a–z).");
    }
    if (!/\d/.test(password)) {
        errors.push("Password must contain at least 1 number.");
    }
    if (!/\W/.test(password)) {
        errors.push("Password must contain at least 1 special character.");
    }

    return errors;
}

export function CreateAccountBox() {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Form State
    const [formData, setFormData] = useState<RegisterRequest>({
        username: "",
        email: "",
        firstName: "",
        lastName: "",
        password: ""
    });

    const [passwordErrors, setPasswordErrors] = useState<string[]>([]);
    const [emailError, setEmailError] = useState<string | null>(null);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });

        // Real-time validation
        if (name === "password") {
            setPasswordErrors(getPasswordErrors(value));
        } else if (name === "email") {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(value)) {
                setEmailError("Invalid email address format.");
            } else {
                setEmailError(null);
            }
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        // Final validation check
        const pwdErrors = getPasswordErrors(formData.password);
        if (pwdErrors.length > 0) {
            setPasswordErrors(pwdErrors);
            return;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(formData.email)) {
            setEmailError("Invalid email address format.");
            return;
        }


        setIsLoading(true);

        try {
            await authService.register(formData);
            // On success, redirect to login
            navigate("/login");
        } catch (err: any) {
            setError(err.message || "Registration failed. Please try again.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen p-4 overflow-y-auto">
            {/* Background */}
            <div className="fixed inset-0 bg-gradient-to-t from-black via-gray-900 to-black pointer-events-none" />

            {/* Register Box */}
            <div className="relative w-full max-w-md p-6 md:p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur my-8">
                <h2 className="text-3xl font-bold mb-6 text-center text-white">Create Account</h2>

                {error && (
                    <div className="mb-4 p-3 bg-red-500/10 border border-red-500/50 text-red-500 rounded text-sm text-center">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1">First Name</label>
                            <input
                                name="firstName"
                                type="text"
                                required
                                value={formData.firstName}
                                onChange={handleChange}
                                className="w-full px-4 py-2 bg-black/50 border border-gray-700 rounded-lg focus:outline-none focus:border-white text-white"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1">Last Name</label>
                            <input
                                name="lastName"
                                type="text"
                                required
                                value={formData.lastName}
                                onChange={handleChange}
                                className="w-full px-4 py-2 bg-black/50 border border-gray-700 rounded-lg focus:outline-none focus:border-white text-white"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">Username</label>
                        <input
                            name="username"
                            type="text"
                            required
                            value={formData.username}
                            onChange={handleChange}
                            className="w-full px-4 py-2 bg-black/50 border border-gray-700 rounded-lg focus:outline-none focus:border-white text-white"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">Email</label>
                        <input
                            name="email"
                            type="email"
                            required
                            value={formData.email}
                            onChange={handleChange}
                            className={`w-full px-4 py-2 bg-black/50 border rounded-lg focus:outline-none text-white ${emailError ? 'border-red-500' : 'border-gray-700 focus:border-white'}`}
                        />
                        {emailError && (
                            <p className="text-xs text-red-400 mt-1">{emailError}</p>
                        )}
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">Password</label>
                        <input
                            name="password"
                            type="password"
                            required
                            value={formData.password}
                            onChange={handleChange}
                            className={`w-full px-4 py-2 bg-black/50 border rounded-lg focus:outline-none text-white ${passwordErrors.length > 0 ? 'border-red-500' : 'border-gray-700 focus:border-white'}`}
                        />
                        {passwordErrors.length > 0 && (
                            <ul className="text-xs text-red-400 mt-2 list-disc list-inside">
                                {passwordErrors.map((err, index) => (
                                    <li key={index}>{err}</li>
                                ))}
                            </ul>
                        )}
                    </div>

                    <button
                        type="submit"
                        className="w-full mt-6 px-6 py-3 bg-white text-black rounded-lg font-semibold hover:bg-gray-200 transition-colors disabled:opacity-50"
                        disabled={isLoading || passwordErrors.length > 0 || !!emailError}
                    >
                        {isLoading ? "Creating Account..." : "Register"}
                    </button>
                </form>

                <div className="mt-8 pt-6 border-t border-gray-800 text-center">
                    <Link className="text-sm text-gray-400 hover:text-white transition-colors" to="/login">
                        Already have an account? <span className="underline">Login</span>
                    </Link>
                </div>
            </div>
        </div>
    );
}