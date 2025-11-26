//fake Backend for account creation

let storedCode: string | null = null;
let storedEmail: string | null = null;

function generateCode(): string {
    return Math.floor(100000 + Math.random() * 900000).toString();
}



//not implemented yet
export function createAccountMock(email: string) {
    return new Promise<{ message: string }>((resolve) => {
        // „save user“
        storedEmail = email;
        storedCode = generateCode();

        console.log("FAKE VERIFICATION CODE:", storedCode); // ← in Browser console

        setTimeout(() => {
            resolve({
                message: "Account created . Please insert verification code.",
            });
        }, 500);
    });
}

export function verifyAccountMock(email: string, code: string) {
    return new Promise<{ message: string }>((resolve, reject) => {
        if (email !== storedEmail) {
            return reject(new Error("Email is not real (fake check)."));
        }
        if (code !== storedCode) {
            return reject(new Error("Code does not match (Fake-Check)."));
        }

        setTimeout(() => {
            resolve({ message: "Verified account successfully " });
        }, 500);
    });
}