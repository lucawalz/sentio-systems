import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/auth";

// Password validation helper
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
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [isValidEmail, setIsValidEmail] = useState(false);
    const [password, setPassword] = useState("");
    const [passwordErrors, setPasswordErrors] = useState<string[]>([]);
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordMatchError, setPasswordMatchError] = useState<string | null>(null);
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");

    const { register, isLoading, error, clearError } = useAuth();
    const navigate = useNavigate();

    const isStrongPassword = password.length > 0 && passwordErrors.length === 0;

    function validateEmail(value: string) {
        setEmail(value);
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        setIsValidEmail(emailRegex.test(value));
    }

    function validatePassword(value: string) {
        setPassword(value);
        const errors = getPasswordErrors(value);
        setPasswordErrors(errors);

        if (confirmPassword.length > 0 && value !== confirmPassword) {
            setPasswordMatchError("Passwords do not match.");
        } else {
            setPasswordMatchError(null);
        }
    }

    function validateConfirmPassword(value: string) {
        setConfirmPassword(value);
        if (password && value !== password) {
            setPasswordMatchError("Passwords do not match.");
        } else {
            setPasswordMatchError(null);
        }
    }

    async function handleCreateAccount(e: React.FormEvent) {
        e.preventDefault();
        clearError();

        const success = await register({
            username,
            email,
            password,
            firstName,
            lastName,
        });

        if (success) {
            // Redirect to login page after successful registration
            navigate("/login");
        }
    }

    const canSubmit =
        username.length > 0 &&
        isValidEmail &&
        isStrongPassword &&
        !passwordMatchError &&
        firstName.length > 0 &&
        lastName.length > 0;

    return (
        <>
            {/* Background */}
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black" />

            <div className="relative w-full max-w-md p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center mt-20">
                <h2 className="font-bold text-3xl mb-6">Create Account</h2>

                <form onSubmit={handleCreateAccount}>
                    {/* Username */}
                    <input
                        type="text"
                        placeholder="username"
                        className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        disabled={isLoading}
                        autoComplete="username"
                    />

                    {/* Email */}
                    <input
                        type="email"
                        placeholder="email"
                        className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        value={email}
                        onChange={(e) => validateEmail(e.target.value)}
                        disabled={isLoading}
                        autoComplete="email"
                    />

                    {/* Email validation */}
                    {!isValidEmail && email.length > 0 && (
                        <p className="text-red-400 text-sm mb-2">Please enter a valid email</p>
                    )}

                    {/* Password */}
                    <input
                        type="password"
                        placeholder="password"
                        className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        value={password}
                        onChange={(e) => validatePassword(e.target.value)}
                        disabled={isLoading}
                        autoComplete="new-password"
                    />

                    {/* Confirm Password */}
                    <input
                        type="password"
                        placeholder="confirm password"
                        className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        value={confirmPassword}
                        onChange={(e) => validateConfirmPassword(e.target.value)}
                        disabled={isLoading}
                        autoComplete="new-password"
                    />

                    {/* Password requirements errors */}
                    {password.length > 0 && passwordErrors.length > 0 && (
                        <div className="text-left text-red-400 text-sm mb-4">
                            <p className="font-semibold">Password must:</p>
                            {passwordErrors.map((err, i) => (
                                <p key={i}>• {err}</p>
                            ))}
                        </div>
                    )}

                    {/* Strong password confirmation */}
                    {isStrongPassword && (
                        <p className="text-left text-green-400 text-sm mb-2">✔ Strong password</p>
                    )}

                    {/* Password match error */}
                    {passwordMatchError && (
                        <p className="text-red-400 text-sm mb-4 text-left">{passwordMatchError}</p>
                    )}

                    {/* First Name */}
                    <input
                        type="text"
                        placeholder="first name"
                        className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        value={firstName}
                        onChange={(e) => setFirstName(e.target.value)}
                        disabled={isLoading}
                        autoComplete="given-name"
                    />

                    {/* Last Name */}
                    <input
                        type="text"
                        placeholder="last name"
                        className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        value={lastName}
                        onChange={(e) => setLastName(e.target.value)}
                        disabled={isLoading}
                        autoComplete="family-name"
                    />

                    {/* Server error message */}
                    {error && (
                        <p className="text-red-400 text-sm mb-4">{error}</p>
                    )}

                    {/* Submit button */}
                    <button
                        type="submit"
                        className="w-full mt-4 px-6 py-3 bg-white text-black rounded-lg font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                        disabled={!canSubmit || isLoading}
                    >
                        {isLoading ? "Creating Account..." : "Create Account"}
                    </button>
                </form>

                <Link className="w-full mt-10 mb-6 px6 py-3 text-white underline hover-grey" to="/login">
                    <p className="mt-6 mb-6">Login into your Account</p>
                </Link>
            </div>
        </>
    );
}