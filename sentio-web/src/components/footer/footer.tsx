import {Link} from "react-router-dom";


export default function Footer() {

    return (

        <>
            <footer className="bg-black text-white overflow-x-hidden relative">
                {/* CONTENT WRAPPER */}
                <div className="max-w-6xl mx-auto px-6 py-12">

                    {/* COLUMNS */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-12 text-center md:text-left">

                        {/* LEFT SECTION */}
                        <div>
                            <h2 className="text-xl font-semibold tracking-wide mb-4">Sentio Systems</h2>
                            <p className="text-gray-700 leading-6">
                                Nobelstraße 10<br/>
                                Stuttgart,<br/>
                                GERMANY
                            </p>
                        </div>

                        {/* CENTER SECTION */}
                        <div>
                            <h3 className="text-gray-500 font-semibold uppercase text-sm mb-4">
                                Links
                            </h3>
                            <ul className="space-y-2">
                                <li><Link to="/" className="hover:underline">Home</Link></li>
                                <li><Link to="/dashboard" className="hover:underline">Dashboard</Link></li>
                                {/*<li><Link to="/login" className="hover:underline">Login</Link></li>
                                <li> <Link to=/createAccount className="hover:underline">Create Account</Link></li>*/}
                            </ul>
                        </div>

                        {/* RIGHT SECTION */}
                        <div>
                            <h3 className="text-gray-500 font-semibold uppercase text-sm mb-4">
                                Help
                            </h3>
                            <ul className="space-y-2">
                                <li>
                                    <Link to="/contact" className="hover:underline">Contact</Link>
                                </li>
                                <li>
                                    <Link to="/privacy" className="hover:underline">Privacy Policies</Link>
                                </li>
                            </ul>
                        </div>

                    </div>

                    {/* LINE DIVIDER */}
                    <div className="border-t border-gray-300 mt-10 pt-6">
                        <p className="text-center text-gray-700 text-sm">
                            2025 Sentio Systems. All rights reserved
                        </p>
                    </div>

                </div>
            </footer>
        </>
    )
}