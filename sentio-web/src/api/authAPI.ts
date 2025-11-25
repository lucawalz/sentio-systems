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

        console.log("FAKE VERIFICATION CODE:", storedCode); // ← steht in der Browser-Konsole

        setTimeout(() => {
            resolve({
                message: "Account erstellt. Bitte gib den Bestätigungscode ein.",
            });
        }, 500);
    });
}

export function verifyAccountMock(email: string, code: string) {
    return new Promise<{ message: string }>((resolve, reject) => {
        if (email !== storedEmail) {
            return reject(new Error("E-Mail ist nicht bekannt (Fake-Check)."));
        }
        if (code !== storedCode) {
            return reject(new Error("Code ist falsch (Fake-Check)."));
        }

        setTimeout(() => {
            resolve({ message: "Account erfolgreich verifiziert! 🎉" });
        }, 500);
    });
}