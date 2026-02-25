import { useState, useEffect } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import { LogoIcon } from '@/components/ui/logo'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Loader2, Check, X, Mail } from 'lucide-react'
import { authService } from '@/services/api/auth'

export default function VerifyEmailPage() {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const token = searchParams.get('token')

    const [isVerifying, setIsVerifying] = useState(true)
    const [success, setSuccess] = useState(false)
    const [error, setError] = useState<string | null>(null)

    // Resend verification state
    const [resendEmail, setResendEmail] = useState('')
    const [isResending, setIsResending] = useState(false)
    const [resendSuccess, setResendSuccess] = useState(false)

    useEffect(() => {
        const verifyEmail = async () => {
            if (!token) {
                setIsVerifying(false)
                setError('No verification token provided.')
                return
            }

            try {
                const data = await authService.verifyEmail(token)

                if (data.success) {
                    setSuccess(true)
                } else {
                    setError(data.message || 'Verification failed')
                }
            } catch {
                setError('Failed to verify email. Please try again.')
            } finally {
                setIsVerifying(false)
            }
        }

        verifyEmail()
    }, [token])

    const handleResend = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!resendEmail.trim()) return

        setIsResending(true)
        try {
            await authService.resendVerification(resendEmail)
            setResendSuccess(true)
        } catch {
            // Still show success to prevent email enumeration
            setResendSuccess(true)
        } finally {
            setIsResending(false)
        }
    }

    // Loading state
    if (isVerifying) {
        return (
            <section className="flex min-h-screen bg-zinc-50 px-4 py-16 md:py-32 dark:bg-transparent">
                <div className="bg-muted m-auto h-fit w-full max-w-sm overflow-hidden rounded-[calc(var(--radius)+.125rem)] border shadow-md shadow-zinc-950/5 dark:[--color-muted:var(--color-zinc-900)]">
                    <div className="bg-card -m-px rounded-[calc(var(--radius)+.125rem)] border p-8 pb-6">
                        <div className="text-center">
                            <Link to="/" aria-label="go home">
                                <LogoIcon className="mx-auto" />
                            </Link>
                            <Loader2 className="h-10 w-10 animate-spin mx-auto mt-6 text-primary" />
                            <h1 className="mt-4 text-xl font-semibold">Verifying your email...</h1>
                            <p className="mt-2 text-sm text-muted-foreground">
                                Please wait while we confirm your email address.
                            </p>
                        </div>
                    </div>
                </div>
            </section>
        )
    }

    // Success state
    if (success) {
        return (
            <section className="flex min-h-screen bg-zinc-50 px-4 py-16 md:py-32 dark:bg-transparent">
                <div className="bg-muted m-auto h-fit w-full max-w-sm overflow-hidden rounded-[calc(var(--radius)+.125rem)] border shadow-md shadow-zinc-950/5 dark:[--color-muted:var(--color-zinc-900)]">
                    <div className="bg-card -m-px rounded-[calc(var(--radius)+.125rem)] border p-8 pb-6">
                        <div className="text-center">
                            <Link to="/" aria-label="go home">
                                <LogoIcon className="mx-auto" />
                            </Link>
                            <div className="mx-auto mt-6 flex h-14 w-14 items-center justify-center rounded-full bg-green-500/10">
                                <Check className="h-7 w-7 text-green-500" />
                            </div>
                            <h1 className="mt-4 text-xl font-semibold">Email Verified!</h1>
                            <p className="mt-2 text-sm text-muted-foreground">
                                Your email has been successfully verified. You can now sign in to your account.
                            </p>
                            <Button className="w-full mt-6" onClick={() => navigate('/login')}>
                                Sign In
                            </Button>
                        </div>
                    </div>
                    <div className="p-3">
                        <p className="text-accent-foreground text-center text-sm">
                            Need help?
                            <Button asChild variant="link" className="px-2">
                                <Link to="/contact">Contact us</Link>
                            </Button>
                        </p>
                    </div>
                </div>
            </section>
        )
    }

    // Error state with resend option
    return (
        <section className="flex min-h-screen bg-zinc-50 px-4 py-16 md:py-32 dark:bg-transparent">
            <form
                onSubmit={handleResend}
                className="bg-muted m-auto h-fit w-full max-w-sm overflow-hidden rounded-[calc(var(--radius)+.125rem)] border shadow-md shadow-zinc-950/5 dark:[--color-muted:var(--color-zinc-900)]">
                <div className="bg-card -m-px rounded-[calc(var(--radius)+.125rem)] border p-8 pb-6">
                    <div className="text-center">
                        <Link to="/" aria-label="go home">
                            <LogoIcon className="mx-auto" />
                        </Link>
                        <div className="mx-auto mt-6 flex h-14 w-14 items-center justify-center rounded-full bg-destructive/10">
                            <X className="h-7 w-7 text-destructive" />
                        </div>
                        <h1 className="mt-4 text-xl font-semibold">Verification Failed</h1>
                        <p className="mt-2 text-sm text-muted-foreground">
                            {error || 'This verification link is invalid or has expired.'}
                        </p>
                    </div>

                    {resendSuccess ? (
                        <div className="mt-6 rounded-lg bg-green-500/10 border border-green-500/20 p-4 text-sm text-green-600 dark:text-green-400 text-center">
                            <Mail className="h-5 w-5 mx-auto mb-2" />
                            Check your email! We've sent a new verification link.
                        </div>
                    ) : (
                        <div className="mt-6 space-y-4">
                            <div className="space-y-2">
                                <Label htmlFor="email" className="block text-sm">
                                    Resend verification email
                                </Label>
                                <Input
                                    type="email"
                                    id="email"
                                    placeholder="name@example.com"
                                    value={resendEmail}
                                    onChange={(e) => setResendEmail(e.target.value)}
                                    disabled={isResending}
                                />
                            </div>
                            <Button className="w-full" type="submit" disabled={isResending || !resendEmail.trim()}>
                                {isResending ? (
                                    <>
                                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                        Sending...
                                    </>
                                ) : (
                                    <>
                                        <Mail className="mr-2 h-4 w-4" />
                                        Resend Verification Email
                                    </>
                                )}
                            </Button>
                        </div>
                    )}
                </div>

                <div className="p-3">
                    <p className="text-accent-foreground text-center text-sm">
                        Already verified?
                        <Button asChild variant="link" className="px-2">
                            <Link to="/login">Sign in</Link>
                        </Button>
                    </p>
                </div>
            </form>
        </section>
    )
}
