import {Navigation} from "../components/layout/navigation.tsx"
export default function Contact(){

    return(
        <>
            <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black"/>
            <div className="relative min-h-screen bg-black text-white flex items-center justify-center">
            <div className="relative w-full max-w-md p-10 bg-gray-900/40 border border-gray-700 rounded-xl backdrop-blur text-center">
                <div>
                    <h1 className="text-3xl font-bold mb-6 text-white ">Contact</h1>
                </div>


            </div>
            </div>
            <Navigation/>
        </>
    )
}