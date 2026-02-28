import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { NativeStartNow } from '@/components/ui/native-start-now-shadcnui'

export default function CallToAction() {
    const navigate = useNavigate()

    return (
        <section className="py-16 md:py-32">
            <div className="mx-auto max-w-5xl px-6">
                <div className="text-center">
                    <h2 className="text-balance text-4xl font-semibold lg:text-5xl">Start Monitoring Today</h2>
                    <p className="mt-4">Join the global network building smarter environmental intelligence.</p>

                    <div className="mt-12 flex flex-wrap justify-center gap-4">
                        <NativeStartNow
                            label="Get Started"
                            onStart={() => { navigate('/login') }}
                            size="md"
                        />

                        <Button
                            asChild
                            size="lg"
                            variant="outline">
                            <Link to="/contact">
                                <span>Book Demo</span>
                            </Link>
                        </Button>
                    </div>
                </div>
            </div>
        </section>
    )
}

