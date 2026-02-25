import { useMemo, useState } from 'react'

export interface PasswordRequirement {
    label: string
    test: (password: string) => boolean
}

export const passwordRequirements: PasswordRequirement[] = [
    { label: 'At least 8 characters', test: (p) => p.length >= 8 },
    { label: 'One uppercase letter', test: (p) => /[A-Z]/.test(p) },
    { label: 'One lowercase letter', test: (p) => /[a-z]/.test(p) },
    { label: 'One number', test: (p) => /[0-9]/.test(p) },
    { label: 'One special character (!@#$%^&*)', test: (p) => /[!@#$%^&*(),.?":{}|<>]/.test(p) },
]

export function usePasswordValidation(password: string) {
    const [showPasswordRequirements, setShowPasswordRequirements] = useState(false)

    const passwordValidation = useMemo(() => {
        return passwordRequirements.map(req => ({
            ...req,
            passed: req.test(password),
        }))
    }, [password])

    const allPasswordRequirementsMet = passwordValidation.every(req => req.passed)

    return {
        showPasswordRequirements,
        setShowPasswordRequirements,
        passwordValidation,
        allPasswordRequirementsMet
    }
}
