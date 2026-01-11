'use client'

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { DynamicIcon, type IconName } from 'lucide-react/dynamic'
import { Link } from 'react-router-dom'

type FAQItem = {
    id: string
    icon: IconName
    question: string
    answer: string
}

export default function FAQsThree() {
    const faqItems: FAQItem[] = [
        {
            id: 'item-1',
            icon: 'cpu',
            question: 'What sensors are compatible with Orbis?',
            answer: 'Orbis supports a wide range of environmental sensors including temperature, humidity, air quality, soil moisture, and acoustic sensors for biodiversity monitoring. Our Hub connects to multiple Node units wirelessly, and we also support Rover and Aerial units for mobile data collection.',
        },
        {
            id: 'item-2',
            icon: 'shield',
            question: 'How is my environmental data protected?',
            answer: 'Your data is encrypted from sensor to cloud and stored securely. You maintain full ownership and can export or delete your data at any time.',
        },
        {
            id: 'item-3',
            icon: 'leaf',
            question: 'How does species recognition work?',
            answer: 'Our AI-powered species recognition uses acoustic analysis and image processing to automatically identify wildlife in your area. The system continuously learns from global research data to improve accuracy, currently supporting over 10,000 species worldwide.',
        },
        {
            id: 'item-4',
            icon: 'alert-triangle',
            question: 'How accurate are the climate early warnings?',
            answer: 'Sentio combines hyperlocal sensor data with regional weather models to provide early warnings with up to 95% accuracy for common weather events. Our proactive analysis can detect anomalies 2-6 hours before traditional forecasting services.',
        },
        {
            id: 'item-5',
            icon: 'globe',
            question: 'How does my data contribute to research?',
            answer: 'With your consent, anonymized environmental data is aggregated into our global research network. This helps scientists build more accurate climate models and enables better predictions. You can opt-in or out at any time from your dashboard settings.',
        },
    ]

    return (
        <section className="bg-background py-20">
            <div className="mx-auto max-w-5xl px-4 md:px-6">
                <div className="flex flex-col gap-10 md:flex-row md:gap-16">
                    <div className="md:w-1/3">
                        <div className="sticky top-20">
                            <h2 className="mt-4 text-3xl font-bold">Frequently Asked Questions</h2>
                            <p className="text-muted-foreground mt-4">
                                Can't find what you're looking for? Contact our{' '}
                                <Link
                                    to="/contact"
                                    className="text-primary font-medium hover:underline">
                                    support team
                                </Link>
                            </p>
                        </div>
                    </div>
                    <div className="md:w-2/3">
                        <Accordion
                            type="single"
                            collapsible
                            className="w-full space-y-2">
                            {faqItems.map((item) => (
                                <AccordionItem
                                    key={item.id}
                                    value={item.id}
                                    className="bg-background shadow-xs rounded-lg border px-4 last:border-b">
                                    <AccordionTrigger className="cursor-pointer items-center py-5 hover:no-underline">
                                        <div className="flex items-center gap-3">
                                            <div className="flex size-6">
                                                <DynamicIcon
                                                    name={item.icon}
                                                    className="m-auto size-4"
                                                />
                                            </div>
                                            <span className="text-base">{item.question}</span>
                                        </div>
                                    </AccordionTrigger>
                                    <AccordionContent className="pb-5">
                                        <div className="px-9">
                                            <p className="text-base">{item.answer}</p>
                                        </div>
                                    </AccordionContent>
                                </AccordionItem>
                            ))}
                        </Accordion>
                    </div>
                </div>
            </div>
        </section>
    )
}
