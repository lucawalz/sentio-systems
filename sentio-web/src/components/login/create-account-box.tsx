import {useState} from "react";
import {Link} from "react-router-dom";
import { verifyAccountMail } from "./verify-account-mail";

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

    const [email, setEmail] = useState("")
    const [isValidEmail, setIsValidEmail] = useState(false)
    const [password, setPassword] = useState("");
    const [passwordErrors, setPasswordErrors] = useState<string[]>([]);
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordMatchError, setPasswordMatchError] = useState<string | null>(null);

    const isStrongPassword = password.length > 0 && passwordErrors.length === 0;

    //check if insert is an email address
    function validateEmail(value: string) {
        setEmail(value)

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        setIsValidEmail(emailRegex.test(value))
    }


    //Checks if password requirements match otherwise error handling
    function validatePassword(value: string) {
        setPassword(value);
        const errors = getPasswordErrors(value);
        setPasswordErrors(errors);

        //check if confirmPassword matches password
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

    function handleCreateAccount() {
        verifyAccountMail(email, password)
            .then((data) => {
                alert(data.message); // oder setState für UI
            })
            .catch((err) => {
                alert(err.message);
            });
    }

    const canSubmit = isValidEmail && isStrongPassword && !passwordMatchError;

    return (
        <>
            {/*background*/}
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black"/>

            <div
                className="relative w-full max-w-md p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center mt-20">
                <h2 className="font-bold text-3xl mb-6 ">Create Account</h2>

                {/*Mail*/}
                <input
                    type="email"
                    placeholder="email"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                    value={email}
                    onChange={(e) => validateEmail(e.target.value)}
                />

                {/*Mail validation*/}
                {!isValidEmail && email.length > 0 && (
                    <p className="text-red-400 text-sm mb-2">Please enter a valid email</p>
                )}

                {/*Passwort choice*/}
                <input
                    type="password"
                    placeholder="password"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                    value={password}
                    onChange={(e) => validatePassword(e.target.value)}
                />


                {/* CONFIRM PASSWORD */}
                <input
                    type="password"
                    placeholder="confirm password"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                    value={confirmPassword}
                    onChange={(e) => validateConfirmPassword(e.target.value)}
                />

                {/*Error handling Password requirements*/}
                {password.length > 0 && passwordErrors.length > 0 && (
                    <div className="text-left text-red-400 text-sm mb-4">
                        <p className="font-semibold">Password must:</p>
                        {passwordErrors.map((err, i) => (
                            <p key={i}>• {err}</p>
                        ))}
                    </div>
                )}

                {/*Message Password is strong*/}
                {isStrongPassword && (
                    <p className="text-left text-green-400 text-sm mb-2">✔ Strong password</p>
                )}

                {/*Password correct?*/}
                {passwordMatchError && (
                    <p className="text-red-400 text-sm mb-4 text-left">{passwordMatchError}</p>
                )}

                {/*Surname*/}
                <input
                    type="surname"
                    placeholder="surname"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                />

                {/*Name*/}
                <input
                    type="name"
                    placeholder="name"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                />

                {/*Button Create Account*/}
                <button onClick={handleCreateAccount}
                    className="w-full mt-4 px-6 py-3 bg-white text-black rounded-lg font-semibold"
                        disabled={!canSubmit}>
                    Create Account
                </button>

                <Link className="w-full mt-10 mb-6 px6 py-3 text-white underline hover-grey" to="/login">
                    <p className="mt-6 mb-6">Login into your Account</p>
                </Link>
            </div>
        </>
    )
}