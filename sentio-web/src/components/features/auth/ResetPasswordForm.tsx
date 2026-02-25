import { useState, useEffect, useMemo } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import { authService } from '@/services/api/auth'
import { LogoIcon } from '@/components/ui/logo'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Loader2, Check, X, Eye, EyeOff } from 'lucide-react'
import { cn } from '@/lib/utils'

interface PasswordRequirement {
    label: string
    test: (password: string) => boolean
}

const passwordRequirements: PasswordRequirement[] = [
    { label: 'At least 8 characters', test: (p) => p.length >= 8 },
    { label: 'One uppercase letter', test: (p) => /[A-Z]/.test(p) },
    { label: 'One lowercase letter', test: (p) => /[a-z]/.test(p) },
    { label: 'One number', test: (p) => /[0-9]/.test(p) },
    { label: 'One special character (!@#$%^&*)', test: (p) => /[!@#$%^&*(),.?":{}|<>]/.test(p) },
]

export default function ResetPasswordPage() {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const token = searchParams.get('token')

    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [showConfirmPassword, setShowConfirmPassword] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [isLoading, setIsLoading] = useState(false)
    const [isValidating, setIsValidating] = useState(true)
    const [tokenValid, setTokenValid] = useState(false)
    const [maskedEmail, setMaskedEmail] = useState('')
    const [success, setSuccess] = useState(false)
    const [showPasswordRequirements, setShowPasswordRequirements] = useState(false)

    // Password validation
    const passwordValidation = useMemo(() => {
        return passwordRequirements.map(req => ({
            ...req,
            passed: req.test(password),
        }))
    }, [password])

    const allPasswordRequirementsMet = passwordValidation.every(req => req.passed)
    const passwordsMatch = password === confirmPassword && confirmPassword.length > 0

    // Validate token on mount
    useEffect(() => {
        const validateToken = async () => {
            if (!token) {
                setTokenValid(false)
                setIsValidating(false)
                return
            }

            try {
                const data = await authService.validateResetToken(token)

                if (data.valid) {
                    setTokenValid(true)
                    setMaskedEmail(data.email || '')
                } else {
                    setTokenValid(false)
                    setError(data.error || 'Invalid or expired link')
                }
            } catch {
                setTokenValid(false)
                setError('Failed to validate reset link')
            } finally {
                setIsValidating(false)
            }
        }

        validateToken()
    }, [token])

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setError(null)

        if (!allPasswordRequirementsMet) {
            setError('Password does not meet all requirements')
            return
        }

        if (!token) {
            setError('Missing reset token')
            return
        }

        setIsLoading(true)

        try {
            const data = await authService.resetPassword({ token, password, confirmPassword })

            if (data.success) {
                setSuccess(true)
            } else {
                setError(data.message || 'Failed to reset password')
            }
        } catch {
            setError('Failed to reset password. Please try again.')
        } finally {
            setIsLoading(false)
        }
    }

    // Loading state
    if (isValidating) {
        return (
            <section className="flex min-h-screen bg-zinc-50 px-4 py-16 md:py-32 dark:bg-transparent">
                <div className="m-auto text-center">
                    <Loader2 className="h-8 w-8 animate-spin mx-auto text-primary" />
                    <p className="mt-4 text-muted-foreground">Validating your reset link...</p>
                </div>
            </section>
        )
    }

    // Invalid token state
    if (!tokenValid && !isValidating) {
        return (
            <section className="flex min-h-screen bg-zinc-50 px-4 py-16 md:py-32 dark:bg-transparent">
                <div className="bg-card m-auto h-fit w-full max-w-sm overflow-hidden rounded-[calc(var(--radius)+.125rem)] border p-8 shadow-md shadow-zinc-950/5">
                    <div className="text-center">
                        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10">
                            <X className="h-6 w-6 text-destructive" />
                        </div>
                        <h1 className="mb-2 text-xl font-semibold">Link Expired</h1>
                        <p className="text-sm text-muted-foreground mb-6">
                            {error || 'This password reset link has expired or is invalid.'}
                        </p>
                        <Button asChild className="w-full">
                            <Link to="/forgot-password">Request New Link</Link>
                        </Button>
                    </div>
                </div>
            </section>
        )
    }

    // Success state
    if (success) {
        return (
            <section className="flex min-h-screen bg-zinc-50 px-4 py-16 md:py-32 dark:bg-transparent">
                <div className="bg-card m-auto h-fit w-full max-w-sm overflow-hidden rounded-[calc(var(--radius)+.125rem)] border p-8 shadow-md shadow-zinc-950/5">
                    <div className="text-center">
                        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-green-500/10">
                            <Check className="h-6 w-6 text-green-500" />
                        </div>
                        <h1 className="mb-2 text-xl font-semibold">Password Reset!</h1>
                        <p className="text-sm text-muted-foreground mb-6">
                            Your password has been successfully updated. You can now sign in with your new password.
                        </p>
                        <Button className="w-full" onClick={() => navigate('/login')}>
                            Sign In
                        </Button>
                    </div>
                </div>
            </section>
        )
    }

    // Main form
    return (
        <section className="flex min-h-screen bg-zinc-50 px-4 py-16 md:py-32 dark:bg-transparent">
            <form
                onSubmit={handleSubmit}
                className="bg-muted m-auto h-fit w-full max-w-sm overflow-hidden rounded-[calc(var(--radius)+.125rem)] border shadow-md shadow-zinc-950/5 dark:[--color-muted:var(--color-zinc-900)]">
                <div className="bg-card -m-px rounded-[calc(var(--radius)+.125rem)] border p-8 pb-6">
                    <div>
                        <Link to="/" aria-label="go home">
                            <LogoIcon />
                        </Link>
                        <h1 className="mb-1 mt-4 text-xl font-semibold">Create New Password</h1>
                        <p className="text-sm text-muted-foreground">
                            Enter a new password for <strong>{maskedEmail}</strong>
                        </p>
                    </div>

                    {error && (
                        <div className="mt-4 rounded-lg bg-destructive/10 border border-destructive/20 p-3 text-sm text-destructive">
                            {error}
                        </div>
                    )}

                    <div className="mt-6 space-y-4">
                        {/* New Password */}
                        <div className="space-y-2">
                            <Label htmlFor="password" className="block text-sm">
                                New Password
                            </Label>
                            <div className="relative">
                                <Input
                                    type={showPassword ? 'text' : 'password'}
                                    id="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    onFocus={() => setShowPasswordRequirements(true)}
                                    disabled={isLoading}
                                    className="pr-10"
                                    placeholder="Enter new password"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                                >
                                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                                </button>
                            </div>

                            {/* Password requirements */}
                            {showPasswordRequirements && (
                                <div className="mt-2 space-y-1 rounded-lg bg-muted p-3">
                                    {passwordValidation.map((req, index) => (
                                        <div
                                            key={index}
                                            className={cn(
                                                'flex items-center gap-2 text-xs',
                                                req.passed ? 'text-green-600 dark:text-green-400' : 'text-muted-foreground'
                                            )}
                                        >
                                            {req.passed ? (
                                                <Check className="h-3 w-3" />
                                            ) : (
                                                <X className="h-3 w-3" />
                                            )}
                                            {req.label}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Confirm Password */}
                        <div className="space-y-2">
                            <Label htmlFor="confirmPassword" className="block text-sm">
                                Confirm Password
                            </Label>
                            <div className="relative">
                                <Input
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    id="confirmPassword"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    disabled={isLoading}
                                    className={cn(
                                        'pr-10',
                                        confirmPassword && !passwordsMatch && 'border-destructive'
                                    )}
                                    placeholder="Confirm new password"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                                >
                                    {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                                </button>
                            </div>
                            {confirmPassword && !passwordsMatch && (
                                <p className="text-xs text-destructive">Passwords do not match</p>
                            )}
                            {passwordsMatch && (
                                <p className="text-xs text-green-600 dark:text-green-400 flex items-center gap-1">
                                    <Check className="h-3 w-3" /> Passwords match
                                </p>
                            )}
                        </div>

                        <Button
                            className="w-full"
                            type="submit"
                            disabled={isLoading || !allPasswordRequirementsMet || !passwordsMatch}
                        >
                            {isLoading ? (
                                <>
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                    Updating Password...
                                </>
                            ) : (
                                'Reset Password'
                            )}
                        </Button>
                    </div>
                </div>

                <div className="p-3">
                    <p className="text-accent-foreground text-center text-sm">
                        Remembered your password?
                        <Button asChild variant="link" className="px-2">
                            <Link to="/login">Sign in</Link>
                        </Button>
                    </p>
                </div>
            </form>
        </section>
    )
}
