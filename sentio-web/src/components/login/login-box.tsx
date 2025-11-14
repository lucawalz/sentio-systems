import {Link} from "react-router-dom";

export function LoginBox() {


    return (
        <div>
            {/* Background */}
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black"/>

            {/* Login-Box */}
            <div
                className="relative w-full max-w-md p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center">
                <h2 className="text-3xl font-bold mb-6">Login to your account</h2>

                <input
                    type="text"
                    placeholder="e-mail"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                />
                <input
                    type="text"
                    placeholder="passwort"
                    className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                />

                <button className="w-full mt-4 px-6 py-3 bg-white text-black rounded-lg font-semibold">
                    Login
                </button>

                <Link classname="w-full mt-4 px6 py-3 text-white" to="/createAccount">
                    <p>Create your own account</p>
                </Link>

            </div>
        </div>



    )
}