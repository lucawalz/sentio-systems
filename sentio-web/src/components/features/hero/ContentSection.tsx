import { Cpu, Zap } from 'lucide-react'

export default function ContentSection() {
    return (
        <section id="solutions" className="py-16 md:py-32">
            <div className="mx-auto max-w-5xl space-y-8 px-6 md:space-y-16">
                <h2 className="relative z-10 max-w-xl text-4xl font-medium lg:text-5xl">The Orbis + Sentio ecosystem working together.</h2>
                <div className="relative">
                    <div className="relative z-10 space-y-4 md:w-1/2">
                        <p>
                            Orbis and Sentio form a <span className="font-medium">scalable, intelligent solution</span> for hyperlocal environmental monitoring.
                        </p>
                        <p>Distributed sensors connect with powerful AI to capture local data, detect biodiversity, and analyze climate risks proactively.</p>

                        <div className="grid grid-cols-2 gap-3 pt-6 sm:gap-4">
                            <div className="space-y-3">
                                <div className="flex items-center gap-2">
                                    <Zap className="size-4" />
                                    <h3 className="text-sm font-medium">Autonomous</h3>
                                </div>
                                <p className="text-muted-foreground text-sm">Self-sustaining sensor units that operate independently in any environment.</p>
                            </div>
                            <div className="space-y-2">
                                <div className="flex items-center gap-2">
                                    <Cpu className="size-4" />
                                    <h3 className="text-sm font-medium">AI-Powered</h3>
                                </div>
                                <p className="text-muted-foreground text-sm">Advanced species recognition and weather anomaly detection at the edge.</p>
                            </div>
                        </div>
                    </div>
                    <div className="md:mask-l-from-35% md:mask-l-to-55% mt-12 h-fit md:absolute md:-inset-y-12 md:inset-x-0 md:mt-0">
                        <div className="border-border/50 relative rounded-2xl border border-dotted p-2">
                            <img
                                className="rounded-[12px] w-full h-auto"
                                src="/dashboard-monitoring.png"
                                alt="Environmental Monitoring Dashboard"
                            />
                        </div>
                    </div>
                </div>
            </div>
        </section>
    )
}

