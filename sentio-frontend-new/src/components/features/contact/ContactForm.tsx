import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Card } from '@/components/ui/card'
import { Loader2, CheckCircle } from 'lucide-react'

interface FieldErrors {
    name?: string
    email?: string
}

export default function ContactSection() {
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        country: '',
        website: '',
        job: '',
        message: '',
    })
    const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
    const [isLoading, setIsLoading] = useState(false)
    const [success, setSuccess] = useState(false)

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { id, value } = e.target
        setFormData(prev => ({ ...prev, [id]: value }))
        if (fieldErrors[id as keyof FieldErrors]) {
            setFieldErrors(prev => ({ ...prev, [id]: undefined }))
        }
    }

    const validateForm = (): boolean => {
        const errors: FieldErrors = {}

        if (!formData.name.trim()) {
            errors.name = 'Please fill in this field'
        }

        if (!formData.email.trim()) {
            errors.email = 'Please fill in this field'
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
            errors.email = 'Please enter a valid email address'
        }

        setFieldErrors(errors)
        return Object.keys(errors).length === 0
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()

        if (!validateForm()) {
            return
        }

        setIsLoading(true)
        // Simulate API call
        await new Promise(resolve => setTimeout(resolve, 1500))
        setIsLoading(false)
        setSuccess(true)
    }

    return (
        <section className="py-32">
            <div className="mx-auto max-w-3xl px-8 lg:px-0">
                <h1 className="text-center text-4xl font-semibold lg:text-5xl">Contact Sentio</h1>
                <p className="mt-4 text-center text-muted-foreground">Ready to deploy environmental intelligence? We're here to help.</p>

                <Card className="mx-auto mt-12 max-w-lg p-8 shadow-md sm:p-16">
                    {success ? (
                        <div className="text-center py-8">
                            <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
                            <h2 className="text-xl font-semibold mb-2">Message Sent!</h2>
                            <p className="text-muted-foreground">Thank you for reaching out. We'll get back to you soon.</p>
                            <Button
                                className="mt-6"
                                variant="outline"
                                onClick={() => {
                                    setSuccess(false)
                                    setFormData({ name: '', email: '', country: '', website: '', job: '', message: '' })
                                }}
                            >
                                Send Another Message
                            </Button>
                        </div>
                    ) : (
                        <>
                            <div>
                                <h2 className="text-xl font-semibold">Tell us about your project</h2>
                                <p className="mt-4 text-sm text-muted-foreground">Reach out to our team! We're eager to learn more about your monitoring needs and how we can support your research.</p>
                            </div>

                            <form
                                onSubmit={handleSubmit}
                                className="mt-12 space-y-6">
                                <div className="space-y-2">
                                    <Label htmlFor="name">Full name</Label>
                                    <Input
                                        type="text"
                                        id="name"
                                        value={formData.name}
                                        onChange={handleChange}
                                        disabled={isLoading}
                                        className={fieldErrors.name ? 'border-destructive' : ''}
                                    />
                                    {fieldErrors.name && (
                                        <p className="text-sm text-destructive">{fieldErrors.name}</p>
                                    )}
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="email">Work Email</Label>
                                    <Input
                                        type="email"
                                        id="email"
                                        value={formData.email}
                                        onChange={handleChange}
                                        disabled={isLoading}
                                        className={fieldErrors.email ? 'border-destructive' : ''}
                                    />
                                    {fieldErrors.email && (
                                        <p className="text-sm text-destructive">{fieldErrors.email}</p>
                                    )}
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="country">Country/Region</Label>
                                    <Select
                                        value={formData.country}
                                        onValueChange={(value) => setFormData(prev => ({ ...prev, country: value }))}
                                        disabled={isLoading}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select Country/Region" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="global">Global / International</SelectItem>
                                            <SelectItem value="us">United States</SelectItem>
                                            <SelectItem value="eu">Europe</SelectItem>
                                            <SelectItem value="apac">Asia Pacific</SelectItem>
                                            <SelectItem value="latam">Latin America</SelectItem>
                                            <SelectItem value="mea">Middle East & Africa</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="website">Organization Website</Label>
                                    <Input
                                        type="url"
                                        id="website"
                                        value={formData.website}
                                        onChange={handleChange}
                                        disabled={isLoading}
                                    />
                                    <span className="text-muted-foreground inline-block text-sm">Must start with 'https'</span>
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="job">Job function</Label>
                                    <Select
                                        value={formData.job}
                                        onValueChange={(value) => setFormData(prev => ({ ...prev, job: value }))}
                                        disabled={isLoading}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select Job Function" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="researcher">Researcher / Scientist</SelectItem>
                                            <SelectItem value="engineer">Engineer / Developer</SelectItem>
                                            <SelectItem value="conservation">Conservation Manager</SelectItem>
                                            <SelectItem value="policy">Policy Maker</SelectItem>
                                            <SelectItem value="agriculture">Agricultural Manager</SelectItem>
                                            <SelectItem value="other">Other</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="message">Message</Label>
                                    <Textarea
                                        id="message"
                                        rows={3}
                                        value={formData.message}
                                        onChange={handleChange}
                                        disabled={isLoading}
                                    />
                                </div>

                                <Button type="submit" disabled={isLoading} className="w-full">
                                    {isLoading ? (
                                        <>
                                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                            Sending...
                                        </>
                                    ) : (
                                        'Submit'
                                    )}
                                </Button>
                            </form>
                        </>
                    )}
                </Card>
            </div>
        </section>
    )
}
