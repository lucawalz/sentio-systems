export default function StatsSection() {
    return (
        <section className="py-12 md:py-20">
            <div className="mx-auto max-w-5xl space-y-8 px-6 md:space-y-16">
                <div className="relative z-10 mx-auto max-w-xl space-y-6 text-center">
                    <h2 className="text-4xl font-medium lg:text-5xl">Impact in Numbers</h2>
                    <p>Building a global network of distributed environmental intelligence to empower research and protect local ecosystems.</p>
                </div>

                <div className="grid gap-12 divide-y *:text-center md:grid-cols-3 md:gap-2 md:divide-x md:divide-y-0">
                    <div className="space-y-4">
                        <div className="text-5xl font-bold">+50k</div>
                        <p>Active Nodes</p>
                    </div>
                    <div className="space-y-4">
                        <div className="text-5xl font-bold">4.2 PB</div>
                        <p>Data Collected</p>
                    </div>
                    <div className="space-y-4">
                        <div className="text-5xl font-bold">120+</div>
                        <p>Research Partners</p>
                    </div>
                </div>
            </div>
        </section>
    )
}
