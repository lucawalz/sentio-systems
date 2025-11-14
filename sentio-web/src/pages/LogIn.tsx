import React from 'react';
import {LoginBox} from "../components/login/login-box.tsx";
import {Navigation} from "../components/layout/navigation.tsx"


const LogIn = () => {


    return(
        <div className="relative min-h-screen bg-black text-white flex items-center justify-center">

            <LoginBox/>
            <Navigation/>
        </div>

    );
}



export default  LogIn;