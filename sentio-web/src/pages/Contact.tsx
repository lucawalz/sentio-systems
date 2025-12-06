import {Navigation} from "../components/layout/navigation.tsx"
import Footer from "../components/footer/footer.tsx";

export default function Contact(){

    return(

        <>
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black"/>

            <div className="relative min-h-screen bg-black text-white flex items-center justify-center">
                {/* Grid with two boxes */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8 w-full max-w-5xl mt-20 mb-8">

                    {/* left contact text box*/}
                    <div
                        className="md:col-span-2 p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center">
                        <h1 className="text-3xl font-bold mb-6 text-white">Contact</h1>

                        {/* reason of contact */}
                        <input
                            type="text"
                            placeholder="reference*"
                            className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        />

                        <div className="grid grid-cols-2 gap-4 mb-4">
                            <input
                                type="text"
                                placeholder="name*"
                                className="w-full p-3 rounded-lg text-black bg-white"
                            />

                            <input
                                type="text"
                                placeholder="surname*"
                                className="w-full p-3 rounded-lg text-black bg-white"
                            />
                        </div>

                        <input
                            type="text"
                            placeholder="mail*"
                            className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                        />

                        <textarea
                            placeholder="message*"
                            className="w-full p-3 mb-4 rounded-lg text-black bg-white h-32"
                        />
                    </div>

                    {/* right contact box with links */}
                    {/*TODO: Style*/}
                    <div
                        className="w-80 p-6 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-left ml-8">
                        <h2 className="text-xl font-bold mb-4">Or contact us via:</h2>

                        <ul className="space-y-2 list-none">
                            <li>
                                Instagram:{" "}
                                <a href="https://www.instagram.com/"
                                   target="_blank"
                                   className="text-blue-400 hover:underline">
                                    @SentioSystems
                                </a>
                            </li>

                            <li>
                                E-Mail:{" "}
                                <a href="mailto:SentioSystems@outlook.com"
                                   className="text-blue-400 hover:underline">
                                    SentioSystems@outlook.com
                                </a>
                            </li>
                        </ul>
                    </div>

                </div>
            </div>

            <Navigation/>
            <Footer/>
        </>
    )
}