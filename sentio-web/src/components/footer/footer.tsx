import {Link} from "react-router-dom";



export default function Footer() {

    return (

        <>
            <footer className="bg-black text-white overflow-x-hidden relative">
                {/* CONTENT WRAPPER */}
                <div className="max-w mx-auto px-6 pt-12 pb-4 border-t border-white">

                    {/* COLUMNS */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-12 text-center md:text-left">

                        {/* LEFT SECTION */}
                        <div>
                            <h2 className="text-xl text-blue-200 font-semibold tracking-wide mb-4">Sentio Systems</h2>
                            <p className="text-white leading-6">
                                Nobelstraße 10<br/>
                                Stuttgart,<br/>
                                GERMANY
                            </p>
                        </div>

                        {/* CENTER SECTION */}
                        <div>
                            <h3 className="text-green-200 font-semibold uppercase text-sm mb-4">
                                Links
                            </h3>
                            <ul className="space-y-2">
                                <li><Link to="/" className="relative inline-block group">Home
                                    <span className="absolute left-0 -bottom-0.5 h-0.5 w-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] transition-all duration-500 group-hover:w-full"
                                    ></span></Link></li>
                                <li><Link to="/dashboard" className="relative inline-block group">Dashboard
                                    <span className="absolute left-0 -bottom-0.5 h-0.5 w-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] transition-all duration-500 group-hover:w-full"
                                ></span>
                            </Link>
                        </li>
                        {/*<li><Link to="/login" className="hover:underline">Login</Link></li>
                                <li> <Link to=/createAccount className="hover:underline">Create Account</Link></li>*/}
                            </ul>
                        </div>

                        {/* RIGHT SECTION */}
                        <div>
                            <h3 className="text-green-200 font-semibold uppercase text-sm mb-4">
                                Help
                            </h3>
                            <ul className="space-y-2">
                                <li>
                                    <Link to="/contact" className="relative inline-block group">Contact
                                        <span className="absolute left-0 -bottom-0.5 h-0.5 w-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] transition-all duration-500 group-hover:w-full"
                                    ></span></Link>
                                </li>
                                <li>
                                    <Link to="/privacy" className="relative inline-block group">Privacy Policies
                                        <span className="absolute left-0 -bottom-0.5 h-0.5 w-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] transition-all duration-500 group-hover:w-full"
                                    ></span></Link>
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