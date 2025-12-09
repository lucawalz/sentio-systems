import { useState } from "react";
import type { FormEvent } from "react";
import {Navigation} from "../components/layout/navigation.tsx"
import Footer from "../components/footer/footer.tsx";

export default function Contact(){

    const [reference, setReference] = useState("");
    const [name, setName] = useState("");
    const [surname, setSurname] = useState("");
    const [mail, setMail] = useState("");
    const [message, setMessage] = useState("");
    const [isSending, setIsSending] = useState(false);
    const [feedback, setFeedback] = useState<string | null>(null);

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setIsSending(true);
        setFeedback(null);

        if (!emailRegex.test(mail)) {
            setFeedback("Please enter a valid email address.");
            setIsSending(false);
            return;
        }
        try {
            const res = await fetch("/api/contact", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    reference,
                    name,
                    surname,
                    mail,
                    message,
                }),
            });

            if (!res.ok) {
                setFeedback("Oops, something went wrong. Please try again later.");
                return;
            }

            setFeedback("Thank you! Your message has been sent.");
            setReference("");
            setName("");
            setSurname("");
            setMail("");
            setMessage("");
        } catch (error) {
            // just load real network issues
            console.error("Failed to send contact form", error);
            setFeedback("Oops, something went wrong. Please try again later.");
        } finally {
            setIsSending(false);
        }
    };




    return(

        <>
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black"/>

            <div className="relative min-h-screen bg-black text-white flex items-center justify-center">
                {/* Grid with two boxes */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8 w-full max-w-5xl mt-20 mb-8">

                    {/*Contact head*/}
                    <div className="text-center md:col-span-3">
                        <h1 className="text-4xl font-bold mb-1 text-white">Contact</h1>
                    </div>

                    {/* left contact text box*/}
                    <form
                        onSubmit={handleSubmit}
                        className="md:col-span-2 p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center"
                    >
                        <h2 className="text-2xl font-bold mb-6 text-green-200">Send a message</h2>

                        {/* reason of contact */}
                        <input
                            type="text"
                            placeholder="reference*"
                            className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                            value={reference}
                            onChange={(e) => setReference(e.target.value)}
                            required
                        />

                        <div className="grid grid-cols-2 gap-4 mb-4">
                            <input
                                type="text"
                                placeholder="name*"
                                className="w-full p-3 rounded-lg text-black bg-white"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                required
                            />

                            <input
                                type="text"
                                placeholder="surname*"
                                className="w-full p-3 rounded-lg text-black bg-white"
                                value={surname}
                                onChange={(e) => setSurname(e.target.value)}
                                required
                            />
                        </div>

                        <input
                            type="email"
                            placeholder="mail*"
                            className="w-full p-3 mb-4 rounded-lg text-black bg-white"
                            value={mail}
                            onChange={(e) => setMail(e.target.value)}
                            required
                        />

                        <textarea
                            placeholder="message*"
                            className="w-full p-3 mb-4 rounded-lg text-black bg-white h-32"
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            required
                        />

                        <button
                            disabled={isSending}
                            className="mt-2 px-6 py-2 rounded-lg bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] text-black font-semibold disabled:opacity-60"
                        >
                            {isSending ? "Sending..." : "Send"}
                            {/*className="w-50 mt-4 px-6 py-3 bg-white text-black rounded-lg font-semibold">Send message*/}</button>

                        {feedback && (
                            <p className="mt-3 text-sm text-gray-300">
                                {feedback}
                            </p>
                        )}
                    </form>

                    {/* right contact box with links */}
                    <div
                        className="w-80 p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-left ml-8">
                        <h2 className="text-green-200 text-2xl font-bold mb-6">Other contacts:</h2>

                        <ul className="space-y-2 list-none">
                            <li>
                                Instagram:{" "}
                                <a href="https://www.instagram.com/" target="_blank" className="text-blue-200 hover:underline">@SentioSystems
                                </a>
                            </li>

                            <li>
                                E-Mail:{" "}
                                <a href="mailto:SentioSystems@outlook.com" className="text-blue-200 hover:underline">SentioSystems@outlook.com</a>
                            </li>

                            <li>
                                Phone:{" "}
                                <a href="tel:+49 000 123456" className="custom-scrollbar text-blue-200 hover:underline">+49 000 123456</a>
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