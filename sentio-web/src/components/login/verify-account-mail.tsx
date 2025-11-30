
export function verifyAccountMail(email: string, password: string) {
    //for example fetch on backend, where mail is sent
    return fetch("http://localhost:3000/api/auth/register", { //not sure if Localhost is correct
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    }).then(res => res.json());
}

//created real sentio mail: SentioSystems@outlook.com
//pw: Sentio123!