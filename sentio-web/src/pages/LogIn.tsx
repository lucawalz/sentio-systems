import React from 'react';
import {LoginBox} from "../components/login/login-box.tsx";
import {Navigation} from "../components/layout/navigation.tsx";
import {CreateAccountBox} from "../components/login/create-account-box.tsx";


export default function LogIn({mode}: { mode: "login" | "register" }) {


    return(
        <div className="relative min-h-screen bg-black text-white flex items-center justify-center">
            {mode === "login" ? <LoginBox/> : <CreateAccountBox/>}
            <Navigation/>
        </div>

    );
}

