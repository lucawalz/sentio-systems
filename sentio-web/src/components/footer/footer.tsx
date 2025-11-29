import {Link} from "react-router-dom";


export default function Footer() {

    return (

        <>
            <footer className="bg-black text-white overflow-x-hidden relative">
                {/* CONTENT WRAPPER */}
                <div className="max-w mx-auto px-6 pt-12 pb-6 border-t border-white">

                    {/* COLUMNS */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-12 text-center md:text-left">

                        {/* LEFT SECTION */}
                        <div>
                            <h2 className="text-xl font-semibold tracking-wide mb-4">Sentio Systems</h2>
                            <p className="text-white leading-6">
                                Nobelstraße 10<br/>
                                Stuttgart,<br/>
                                GERMANY
                            </p>
                        </div>

                        {/* CENTER SECTION */}
                        <div>
                            <h3 className="text-white font-semibold uppercase text-sm mb-4">
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
                            <h3 className="text-white font-semibold uppercase text-sm mb-4">
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
                    <div className="border-t border-white w-250 mx-auto mt-10 pt-4 mb-0">
                        <p className="text-center text-white text-sm mb-0">
                            2025 Sentio Systems. All rights reserved
                        </p>
                    </div>

                </div>
            </footer>
        </>
    )
}