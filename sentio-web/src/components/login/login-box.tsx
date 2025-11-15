import {Link} from "react-router-dom";
import { useState } from "react"

export function LoginBox() {
    const [email, setEmail] = useState("")
    const [isValidEmail, setIsValidEmail] = useState(false)

    function validateEmail(value: string) {
        setEmail(value)

        // simple email regex
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        setIsValidEmail(emailRegex.test(value))
    }


    return (
        <div>
            {/* Background */}
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black"/>

            {/* Login-Box */}
            <div
                className="relative w-full max-w-md p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center">
                <h2 className="text-3xl font-bold mb-6">Login</h2>

                <input
                    type="text"
                    placeholder="email"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                    value={email}
                    onChange={(e) => validateEmail(e.target.value)}
                />

                <input
                    type="password"
                    placeholder="password"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                />
                {/* forgot password LINK */}
                {isValidEmail && (
                    <>
                <a
                    href="/forgot-password"
                    className="block text-sm text-gray-300 underline hover:text-white"
                >
                    Forgot password?
                </a>
                    </>
                )}

                <button className="w-full mt-4 px-6 py-3 bg-white text-black rounded-lg font-semibold">
                    Login
                </button>

                <Link className="w-full mt-4 px6 py-3 text-white" to="/create-account">
                    <p className=" mb-6">Create account</p>
                </Link>


            </div>
        </div>


    )
}
