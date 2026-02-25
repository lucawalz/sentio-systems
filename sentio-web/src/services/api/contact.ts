// src/services/api/contact.ts
// Handles all contact form-related API requests.

export interface ContactData {
    name: string
    surname: string
    mail: string
    reference: string
    message: string
}

export const contactService = {
    submitContactForm: async (data: ContactData) => {
        const response = await fetch('/api/contact', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        })

        if (!response.ok) {
            throw new Error('Failed to send message')
        }

        return response
    }
}
